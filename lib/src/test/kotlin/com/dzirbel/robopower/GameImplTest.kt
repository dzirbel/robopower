package com.dzirbel.robopower

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.random.Random

class GameImplTest {
    // all-0 random generator; necessary for picking the spy card (so the first card is always chosen)
    private val allZeroRandom = object : Random() {
        override fun nextBits(bitCount: Int) = 0
    }

    @Test
    fun `trivial one-on-one game runs successfully`() {
        val sortedNormalCards = Card.deck.filter { it.isNormal }.sortedBy { it.score }
        val game = buildGame(
            playerFactories = listOf(InOrderPlayer, InOrderPlayer),
            sortedNormalCards.take(12), // player 1 gets the 12 lowest cards (weakest first)
            sortedNormalCards.takeLast(12).reversed(), // player 2 gets the 12 highest cards (strongest first)
        )

        val result = game.run()

        assertEquals(1, (result as GameResult.Winner).winner)
        assertEquals(GameImpl.STARTING_CARDS, game.turnCount) // 1 turn for each starting card

        // note discarded and played cards by player 2 are cycled because retaining them puts them at the end of the
        // hand (new cards are also drawn to the end of the hand)
        val eventLog = buildEventLog {
            turnEvents(0, discard = Card.BUZZY, loser = 0 to Card.BUZZY, winner = 1 to Card.ROBO_STRIKER)
            turnEvents(1, discard = Card.COPY, loser = 0 to Card.BUZZY, winner = 1 to Card.COPY)
            turnEvents(0, discard = Card.BUZZY, loser = 0 to Card.BUZZY, winner = 1 to Card.UN_BEAT)
            turnEvents(1, discard = Card.UN_BEAT, loser = 0 to Card.BUZZY, winner = 1 to Card.SLICE)
            turnEvents(0, discard = Card.WIND, loser = 0 to Card.WIND, winner = 1 to Card.ROBO_STRIKER)
            turnEvents(1, discard = Card.SLICE, loser = 0 to Card.WIND, winner = 1 to Card.COPY, endTurn = false)
            add(GameEvent.PlayerEliminated(turnCount = turn, upPlayerIndex = 1, eliminatedPlayerIndex = 0))
        }

        assertEquals(eventLog, game.eventLog)
    }

    @Test
    fun `single round with spy`() {
        val game = buildGame(
            playerFactories = listOf(InOrderPlayer, InOrderPlayer),
            listOf(Card.SPY, Card.SLICE, Card.BUZZY, Card.BUZZY, Card.BUZZY, Card.BUZZY, Card.BUZZY),
            listOf(Card.ROBO_STRIKER, Card.WIND, Card.WIND, Card.WIND, Card.WIND, Card.WIND),
        )

        val result = game.run(maxRounds = 1)

        assertNull(result)
        assertEquals(1, game.turnCount)

        val eventLog = buildEventLog {
            turnEvents(
                upPlayer = 0,
                discard = Card.SPY,
                spiedAndRemaining = 1 to 5, // player index 1 spied with 5 cards remaining afterward
                loser = 1 to Card.WIND,
                winner = 0 to Card.SLICE,
            )
        }

        assertEquals(eventLog, game.eventLog)
    }

    /**
     * Builds a [Game] where the cards for each player are pre-determined by [cardsByPlayer] by inserting them in the
     * deck one-by-one in a round robin style.
     */
    private fun buildGame(playerFactories: List<Player.Factory>, vararg cardsByPlayer: List<Card>): GameImpl {
        assert(playerFactories.size == cardsByPlayer.size)
        assert(cardsByPlayer.all { it.size >= GameImpl.STARTING_CARDS })

        val remainingCardsByPlayer = cardsByPlayer.map { it.toMutableList() }
        val usedCardCounts = mutableMapOf<Card, Int>()
        val cardList = buildList {
            // continue as long as the provided card list is non-empty in round-robin order for each draw; this works
            // for both the initial starting deal and allows skipping players in the draw sequence after they have been
            // eliminated
            while (remainingCardsByPlayer.any { it.isNotEmpty() }) {
                remainingCardsByPlayer.forEach { playerCards ->
                    if (playerCards.isNotEmpty()) {
                        val card = playerCards.removeFirst()
                        add(card)
                        usedCardCounts.compute(card) { _, count -> (count ?: 0) + 1 }
                    }
                }
            }

            // now fill in all the remaining cards which are not assigned to any player; order here should not matter
            for (card in Card.values()) {
                val usedCount = usedCardCounts[card] ?: 0
                assert(usedCount <= card.multiplicity)
                repeat(card.multiplicity - usedCount) {
                    add(card)
                }
            }
        }

        val deck = Deck(drawPile = cardList.reversed(), random = allZeroRandom)
        return GameImpl(playerFactories = playerFactories, deck = deck, random = allZeroRandom)
    }

    private fun buildEventLog(builder: EventLogBuilder.() -> Unit): List<GameEvent> {
        return EventLogBuilder().apply(builder).build()
    }

    private class EventLogBuilder {
        var turn = 0
            private set
        private val events = mutableListOf<GameEvent>()

        fun turnEvents(
            upPlayer: Int,
            discard: Card,
            winner: Pair<Int, Card>,
            loser: Pair<Int, Card>,
            spiedAndRemaining: Pair<Int, Int>? = null,
            endTurn: Boolean = true,
        ) {
            val duelResult = buildSimpleDuelResult(loser = loser, winner = winner)
            turnEvents(
                upPlayer = upPlayer,
                discard = discard,
                duelResult = duelResult,
                spiedAndRemaining = spiedAndRemaining,
                endTurn = endTurn,
            )
        }

        fun turnEvents(
            upPlayer: Int,
            discard: Card,
            duelResult: DuelResult,
            spiedAndRemaining: Pair<Int, Int>? = null,
            endTurn: Boolean = true,
        ) {
            turn++

            add(GameEvent.StartTurn(turnCount = turn, upPlayerIndex = upPlayer))
            add(GameEvent.PlayerDraw(turnCount = turn, upPlayerIndex = upPlayer))
            add(GameEvent.PlayerDiscard(turnCount = turn, upPlayerIndex = upPlayer, discardedCard = discard))

            if (spiedAndRemaining != null) {
                add(
                    GameEvent.Spied(
                        turnCount = turn,
                        upPlayerIndex = upPlayer,
                        spiedPlayerIndex = spiedAndRemaining.first,
                        remainingCards = spiedAndRemaining.second,
                    ),
                )
            }

            duelResult.rounds.forEach { round ->
                add(GameEvent.AfterDuelRound(turnCount = turn, upPlayerIndex = upPlayer, round = round))
            }
            add(GameEvent.Duel(turnCount = turn, upPlayerIndex = upPlayer, result = duelResult))

            if (endTurn) {
                add(GameEvent.EndTurn(turnCount = turn, upPlayerIndex = upPlayer))
            }
        }

        fun add(event: GameEvent) {
            events.add(event)
        }

        fun build() = events.toList()
    }

    companion object {
        /**
         * Builds a simple [DuelResult] where a two players dueled with normal cards and did not tie.
         */
        private fun buildSimpleDuelResult(loser: Pair<Int, Card>, winner: Pair<Int, Card>): DuelResult {
            return DuelResult(
                rounds = listOf(
                    DuelRound(
                        playedCards = mapOf(loser, winner),
                        result = DuelRoundResult.LowestLost(losers = mapOf(loser), winners = mapOf(winner)),
                    ),
                ),
                discardedCards = mapOf(loser.first to listOf(loser.second)),
                retainedCards = mapOf(winner.first to listOf(winner.second)),
            )
        }
    }
}
