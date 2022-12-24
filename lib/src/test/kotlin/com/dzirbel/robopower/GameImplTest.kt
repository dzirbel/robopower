package com.dzirbel.robopower

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class GameImplTest {
    // all-0 random generator; necessary for picking the spy card (so the first card is always chosen)
    private val allZeroRandom = object : Random() {
        override fun nextBits(bitCount: Int) = 0
    }

    @Test
    fun `trivial one-on-one game runs successfully`() {
        val sortedCards = Card.values()
            .filter { it.isNormal }
            .sortedBy { it.score }
            .flatMap { card -> List(card.multiplicity) { card } }
        val game = buildGame(
            playerFactories = listOf(InOrderPlayer, InOrderPlayer),
            sortedCards.take(12), // player 1 gets the 12 lowest cards (weakest first)
            sortedCards.takeLast(12).reversed(), // player 2 gets the 12 highest cards (strongest first)
        )

        val result = game.run()

        assertEquals(1, (result as GameResult.Winner).winner)
        assertEquals(GameImpl.STARTING_CARDS, game.turnCount) // 1 turn for each starting card

        game.eventLog.forEach {
            println(it)
        }

        // note discarded and played cards by player 2 are cycled because retaining them puts them at the end of the
        // hand (new cards are also drawn to the end of the hand)
        assertEquals(
            listOf(
                eventsForTurn(
                    turnCount = 1,
                    upPlayerIndex = 0,
                    discard = Card.BUZZY,
                    duelResult = buildSimpleDuelResult(loser = 0 to Card.BUZZY, winner = 1 to Card.ROBO_STRIKER),
                ),
                eventsForTurn(
                    turnCount = 2,
                    upPlayerIndex = 1,
                    discard = Card.COPY,
                    duelResult = buildSimpleDuelResult(loser = 0 to Card.BUZZY, winner = 1 to Card.COPY),
                ),
                eventsForTurn(
                    turnCount = 3,
                    upPlayerIndex = 0,
                    discard = Card.BUZZY,
                    duelResult = buildSimpleDuelResult(loser = 0 to Card.BUZZY, winner = 1 to Card.UN_BEAT),
                ),
                eventsForTurn(
                    turnCount = 4,
                    upPlayerIndex = 1,
                    discard = Card.UN_BEAT,
                    duelResult = buildSimpleDuelResult(loser = 0 to Card.BUZZY, winner = 1 to Card.SLICE),
                ),
                eventsForTurn(
                    turnCount = 5,
                    upPlayerIndex = 0,
                    discard = Card.WIND,
                    duelResult = buildSimpleDuelResult(loser = 0 to Card.WIND, winner = 1 to Card.ROBO_STRIKER),
                ),
                eventsForTurn(
                    turnCount = 6,
                    upPlayerIndex = 1,
                    discard = Card.SLICE,
                    duelResult = buildSimpleDuelResult(loser = 0 to Card.WIND, winner = 1 to Card.COPY),
                )
                    .filter { it !is GameEvent.EndTurn },
            )
                .flatten()
                .plus(GameEvent.PlayerEliminated(turnCount = 6, upPlayerIndex = 1, eliminatedPlayerIndex = 0)),
            game.eventLog,
        )
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

    /**
     * Generates the regular events (no spies, etc) for a single game turn with the given parameters.
     */
    private fun eventsForTurn(
        turnCount: Int,
        upPlayerIndex: Int,
        discard: Card,
        duelResult: DuelResult,
    ): List<GameEvent> {
        return listOf(
            GameEvent.StartTurn(turnCount = turnCount, upPlayerIndex = upPlayerIndex),
            GameEvent.PlayerDraw(turnCount = turnCount, upPlayerIndex = upPlayerIndex),
            GameEvent.PlayerDiscard(turnCount = turnCount, upPlayerIndex = upPlayerIndex, discardedCard = discard),
            GameEvent.AfterDuelRound(
                turnCount = turnCount,
                upPlayerIndex = upPlayerIndex,
                round = duelResult.rounds.first(),
            ),
            GameEvent.Duel(turnCount = turnCount, upPlayerIndex = upPlayerIndex, result = duelResult),
            GameEvent.EndTurn(turnCount = turnCount, upPlayerIndex = upPlayerIndex),
        )
    }

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
