package com.dzirbel.robopower

import com.dzirbel.robopower.Dueler.PlayerCardSupplier
import com.dzirbel.robopower.util.maxKeysBy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * Contains the logic for executing duels, by resolving them from a set of [PlayerCardSupplier]s providing cards in the
 * duel to a [DuelResult].
 *
 * This functionality is used for the main game loop but may also be used in bots to simulate duels.
 */
object Dueler {
    /**
     * A wrapper around functionality to provide cards to a duel. Because duels can last multiple rounds, it is
     * insufficient for [duel] to simply accept a list of cards.
     */
    interface PlayerCardSupplier {
        /**
         * Fetches the next [Card] to be played in a round of a duel where [involvedPlayers] are still playing and
         * [previousRounds] have already been played.
         *
         * Normally this might be a suspending function, but instead returns a [Deferred] so that [FromPlayerAndDeck]
         * can ensure calls to the [Deck] are not done concurrently (and avoid launching a job for them).
         *
         * @return a [Deferred] result of the pair <played card, whether the card was drawn from the deck>
         */
        fun CoroutineScope.nextCard(
            involvedPlayers: Set<Int>,
            previousRounds: List<DuelRound>,
        ): Deferred<Pair<Card, Boolean>>

        /**
         * The real-world case where cards are chosen from the handle of a [player] until the hand is depleted and any
         * further cards must be drawn from the [deck].
         */
        class FromPlayerAndDeck(
            private val player: Player,
            private val deck: Deck,
            private val onDeckReshuffled: (previousDiscard: List<Card>) -> Unit = {},
        ) : PlayerCardSupplier {
            override fun CoroutineScope.nextCard(
                involvedPlayers: Set<Int>,
                previousRounds: List<DuelRound>,
            ): Deferred<Pair<Card, Boolean>> {
                return if (player.handSize(includeCardsInPlay = false) == 0) {
                    val (card, previousDiscard) = deck.draw()
                    previousDiscard?.let(onDeckReshuffled)
                    CompletableDeferred(Pair(card, true))
                } else {
                    async { Pair(player.doDuel(involvedPlayers, previousRounds), false) }
                }
            }
        }

        /**
         * A simple case where a predefined list of cards are returned one-by-one in each round of the duel, mainly for
         * testing or simulation.
         */
        class FromCardList(cards: List<Card>) : PlayerCardSupplier {
            private val remainingCards = cards.toMutableList()

            override fun CoroutineScope.nextCard(
                involvedPlayers: Set<Int>,
                previousRounds: List<DuelRound>,
            ): Deferred<Pair<Card, Boolean>> {
                return CompletableDeferred(Pair(remainingCards.removeFirst(), false))
            }
        }
    }

    /**
     * Runs a duel among the given [players] and returns a [DuelResult] with a complete record of the cards played in
     * each round and the resulting actions (cards trapped, discarded, etc).
     *
     * @param players the [PlayerCardSupplier]s providing each round of cards in the duel, provided as [IndexedValue]s
     *  to reference the official player indexes
     * @param onRound callback invoked after each [DuelRound] is played
     */
    @Suppress("ReturnCount")
    fun duel(
        players: Iterable<IndexedValue<PlayerCardSupplier>>,
        onRound: (DuelRound) -> Unit = {},
    ): DuelResult {
        val rounds = mutableListOf<DuelRound>()

        // map from player index to the cards they have removed from their hand and are currently in-play
        val cardsInPlay: MutableMap<Int, List<Card>> = mutableMapOf()

        // map from player index to the cards which they played but have already been returned to their hand (due to
        // not being involved in a double-duel)
        val retainedCards: MutableMap<Int, List<Card>> = mutableMapOf()

        // map from player index to the cards which they have played from the deck
        val drawnCards: MutableMap<Int, List<Card>> = mutableMapOf()

        // set of player indexes that are still involved in the duel
        var involvedPlayers: Set<Int> = players.mapTo(mutableSetOf()) { it.index }

        // whether any traps have been played in previous rounds; if so then even if the duel is resolved without traps
        // the cards in play will ultimately be trapped
        var trapping = false

        while (true) {
            val playedCards: Map<Int, Pair<Card, Boolean>> = getPlayedCardsForDuel(players, involvedPlayers, rounds)
            // extract only the card and not whether it was drawn from the deck
            val playedCardsOnly = playedCards.mapValues { it.value.first }

            // add newly played cards to the pool and map of drawn cards
            for ((cardPlayerIndex, cardResult) in playedCards) {
                cardsInPlay.compute(cardPlayerIndex) { _, cards -> cards.orEmpty().plus(cardResult.first) }
                if (cardResult.second) {
                    drawnCards.compute(cardPlayerIndex) { _, cards -> cards.orEmpty().plus(cardResult.first) }
                }
            }

            val roundResult = duelRound(playedCards = playedCardsOnly, trapping = trapping)
            val round = DuelRound(
                playedCards = playedCardsOnly,
                drawnCards = playedCards.filterValues { it.second }.mapValues { it.value.first },
                result = roundResult,
            )
            rounds.add(round)
            onRound(round)

            when (roundResult) {
                is DuelRoundResult.LowestLost ->
                    if (trapping) {
                        // previous round was a trap, so this was resolving the trap and now all cards in play go to the
                        // winner (so we need to look for the highest card rather than lowest)
                        val winners = playedCards.keys.minus(roundResult.losers.keys)

                        val winner = winners.first()
                        val winnerCards = cardsInPlay.getValue(winner)

                        return DuelResult(
                            rounds = rounds.toList(),
                            // discard all trap(s) played by the winner
                            discardedCards = mapOf(winner to winnerCards.filter { it.isTrap }),
                            // retain cards played by the winner except for their trap(s); note retainedCards cannot
                            // contain any cards by the winner already (since only cards excluded from a double-duel
                            // have already retained cards)
                            retainedCards = retainedCards.plus(winner to winnerCards.filter { !it.isTrap }),
                            // everything in play except for the winner's cards have been trapped
                            trappedCards = mapOf(winner to cardsInPlay.filterKeys { it != winner }),
                            drawnCards = drawnCards,
                        )
                    } else {
                        val loser = roundResult.losers.keys.first()

                        return DuelResult(
                            rounds = rounds.toList(),
                            // discard all the cards played by the loser
                            discardedCards = cardsInPlay.filterKeys { it == loser },
                            // retain all the cards played by everyone else; note again retainedCards cannot contain
                            // cards by players who still have cards in play
                            retainedCards = retainedCards + cardsInPlay.filterKeys { it != loser },
                            // nothing is trapped
                            trappedCards = emptyMap(),
                            drawnCards = drawnCards,
                        )
                    }

                is DuelRoundResult.Counteracted ->
                    return DuelResult(
                        rounds = rounds.toList(),
                        discardedCards = cardsInPlay.toMap(),
                        retainedCards = retainedCards.toMap(), // anything retained in previous rounds is still retained
                        trappedCards = emptyMap(), // impossible for there to be anything trapped after counteracting
                        drawnCards = drawnCards,
                    )

                is DuelRoundResult.Trapped -> {
                    val trapper = roundResult.trapper
                    val trapperCards = cardsInPlay.getValue(trapper)
                    return DuelResult(
                        rounds = rounds.toList(),
                        // discard trap(s) played by the winner
                        discardedCards = mapOf(trapper to trapperCards.filter { it.isTrap }),
                        // retain the cards played by the trapper (except for the trap(s)); note retained cards cannot
                        // container any cards by the trapper already
                        retainedCards = retainedCards
                            .plus(trapper to trapperCards.filter { !it.isTrap })
                            .filterValues { it.isNotEmpty() },
                        // everything in play except for the trapper's cards have been trapped
                        trappedCards = mapOf(roundResult.trapper to cardsInPlay.filterKeys { it != trapper }),
                        drawnCards = drawnCards,
                    )
                }

                is DuelRoundResult.DoubleDuel -> {
                    if (trapping || roundResult.trapping) {
                        trapping = true // if we are or were trapping, everything in play stays in play
                    } else {
                        // since we are not trapping cards, everyone not in the double duel is no longer involved
                        val uninvolvedPlayers = involvedPlayers.minus(roundResult.doubleDuelers.keys)

                        // retain all the cards no longer in play (i.e. they will not be trapped or counteracted in the
                        // subsequent double duel)
                        retainedCards.putAll(cardsInPlay.filterKeys { it in uninvolvedPlayers })

                        // remove the cards from the set of in-play cards (so they don't get trapped or discarded)
                        for (player in uninvolvedPlayers) {
                            cardsInPlay.remove(player)
                        }
                    }

                    // even if trapping we still narrow down the list of involved players since only they play cards now
                    involvedPlayers = roundResult.doubleDuelers.keys
                }
            }
        }
    }

    /**
     * Determines the result of a single round of a duel as the returned [DuelRound].
     *
     * @param playedCards the cards played in this round, as a map from player index to the card played by that player
     * @param trapping whether this round is the result of a previous round with multiple tied Traps; in this case
     *  instead of the lowest card being eliminated (or double dueling if it is tied), the highest card now wins all the
     *  cards in play (again double dueling if tied)
     */
    fun duelRound(playedCards: Map<Int, Card>, trapping: Boolean = false): DuelRoundResult {
        return if (playedCards.any { it.value.isCounteract }) {
            DuelRoundResult.Counteracted(
                counteracters = playedCards.filterValues { it.isCounteract }.keys.toSet(),
            )
        } else {
            val trappers: Map<Int, Card> = playedCards.filterValues { it.isTrap }
            when {
                // no traps (or counteracts): the lowest card loses; if tied double duel
                trappers.isEmpty() ->
                    if (trapping) {
                        // if already trapping, instead find the highest card(s) to win the trap
                        val winners: Set<Int> = playedCards.maxKeysBy(Card.comparatorByScore)

                        if (winners.size == 1) {
                            val winner = winners.first()
                            DuelRoundResult.LowestLost(
                                losers = playedCards.minus(winner),
                                winners = mapOf(winner to playedCards.getValue(winner)),
                            )
                        } else {
                            DuelRoundResult.DoubleDuel(
                                trapping = false,
                                doubleDuelers = playedCards.filterKeys { playerIndex -> playerIndex in winners },
                            )
                        }
                    } else {
                        val losers: Set<Int> = playedCards.maxKeysBy(Card.comparatorByScore.reversed())

                        if (losers.size == 1) {
                            val loser = losers.first()
                            DuelRoundResult.LowestLost(
                                losers = mapOf(loser to playedCards.getValue(loser)),
                                winners = playedCards.minus(loser),
                            )
                        } else {
                            DuelRoundResult.DoubleDuel(
                                trapping = false,
                                doubleDuelers = playedCards.filterKeys { playerIndex -> playerIndex in losers },
                            )
                        }
                    }

                // a single trap: everything else is trapped
                trappers.size == 1 ->
                    DuelRoundResult.Trapped(trapper = trappers.keys.first())

                // multiple traps: double duel to trap all the cards in play
                else ->
                    DuelRoundResult.DoubleDuel(trapping = true, doubleDuelers = trappers)
            }
        }
    }

    /**
     * Synchronously retrieves the cards played from each of the given [players] for a round of dueling, running the
     * card retrieval of each in parallel.
     *
     * @return map from player index to the pair <played card, whether the card was drawn from the deck> (as in
     *  [PlayerCardSupplier.nextCard])
     */
    private fun getPlayedCardsForDuel(
        players: Iterable<IndexedValue<PlayerCardSupplier>>,
        involvedPlayers: Set<Int>,
        previousRounds: List<DuelRound>,
    ): Map<Int, Pair<Card, Boolean>> {
        return runBlocking {
            players
                .filter { it.index in involvedPlayers }
                .associate { (playerIndex, player) ->
                    with(player) {
                        playerIndex to nextCard(involvedPlayers, previousRounds)
                    }
                }
                .mapValues { it.value.await() }
        }
    }
}
