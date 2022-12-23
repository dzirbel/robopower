package com.dzirbel.robopower

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DuelerTest {
    @Test
    fun `higher card wins in one on one`() {
        val player0 = listOf(Card.ROBO_STRIKER)
        val player1 = listOf(Card.BUZZY)
        assertDuel(player0, player1) { players ->
            DuelResult(
                discardedCards = mapOf(1 to player1),
                retainedCards = mapOf(0 to player0),
                rounds = players.createRounds(
                    DuelRoundResult.LowestLost(
                        losers = mapOf(1 to player1.first()),
                        winners = mapOf(0 to player0.first()),
                    ),
                ),
            )
        }
    }

    @Test
    fun `lowest card loses in group battle`() {
        val player0 = listOf(Card.UN_BEAT)
        val player1 = listOf(Card.BUZZY)
        val player2 = listOf(Card.UN_BEAT)
        val player3 = listOf(Card.ALX)
        assertDuel(player0, player1, player2, player3) { players ->
            DuelResult(
                discardedCards = mapOf(1 to player1),
                retainedCards = mapOf(0 to player0, 2 to player2, 3 to player3),
                rounds = players.createRounds(
                    DuelRoundResult.LowestLost(
                        losers = mapOf(1 to player1.first()),
                        winners = mapOf(0 to player0.first(), 2 to player2.first(), 3 to player3.first()),
                    ),
                ),
            )
        }
    }

    @Test
    fun `counteract discards all cards in single round`() {
        val player0 = listOf(Card.UN_BEAT)
        val player1 = listOf(Card.BUZZY)
        val player2 = listOf(Card.UN_BEAT)
        val player3 = listOf(Card.COUNTERACT)
        assertDuel(player0, player1, player2, player3) { players ->
            DuelResult(
                discardedCards = players.withIndex().associate { it.index to it.value },
                rounds = players.createRounds(
                    DuelRoundResult.Counteracted(counteracters = setOf(3)),
                ),
            )
        }
    }

    @Test
    fun `multiple counteracts discard all cards in single round`() {
        val player0 = listOf(Card.UN_BEAT)
        val player1 = listOf(Card.BUZZY)
        val player2 = listOf(Card.COUNTERACT)
        val player3 = listOf(Card.COUNTERACT)
        assertDuel(player0, player1, player2, player3) { players ->
            DuelResult(
                discardedCards = players.withIndex().associate { it.index to it.value },
                rounds = players.createRounds(
                    DuelRoundResult.Counteracted(counteracters = setOf(2, 3)),
                ),
            )
        }
    }

    @Test
    fun `trap traps all regular cards in single round`() {
        val player0 = listOf(Card.UN_BEAT)
        val player1 = listOf(Card.BUZZY)
        val player2 = listOf(Card.UN_BEAT)
        val player3 = listOf(Card.TRAP)
        assertDuel(player0, player1, player2, player3) { players ->
            DuelResult(
                discardedCards = mapOf(3 to player3),
                trappedCards = mapOf(3 to mapOf(0 to player0, 1 to player1, 2 to player2)),
                rounds = players.createRounds(DuelRoundResult.Trapped(trapper = 3)),
            )
        }
    }

    @Test
    fun `counteract counteracts traps in single round`() {
        val player0 = listOf(Card.UN_BEAT)
        val player1 = listOf(Card.BUZZY)
        val player2 = listOf(Card.COUNTERACT)
        val player3 = listOf(Card.TRAP)
        assertDuel(player0, player1, player2, player3) { players ->
            DuelResult(
                discardedCards = players.withIndex().associate { it.index to it.value },
                rounds = players.createRounds(
                    DuelRoundResult.Counteracted(counteracters = setOf(2)),
                ),
            )
        }
    }

    @Test
    fun `double duel with regular cards`() {
        val player0 = listOf(Card.CRUSHER, Card.ROBO_STRIKER)
        val player1 = listOf(Card.BRAINIAC, Card.WIND)
        val player2 = listOf(Card.UN_BEAT)
        assertDuel(player0, player1, player2) { players ->
            DuelResult(
                retainedCards = mapOf(0 to player0, 2 to player2),
                discardedCards = mapOf(1 to player1),
                rounds = players.createRounds(
                    DuelRoundResult.DoubleDuel(
                        trapping = false,
                        doubleDuelers = mapOf(0 to player0.first(), 1 to player1.first()),
                    ),
                    DuelRoundResult.LowestLost(
                        losers = mapOf(1 to player1.last()),
                        winners = mapOf(0 to player0.last()),
                    ),
                ),
            )
        }
    }

    @Test
    fun `double duel is counteracted`() {
        val player0 = listOf(Card.CRUSHER, Card.ROBO_STRIKER)
        val player1 = listOf(Card.BRAINIAC, Card.COUNTERACT)
        val player2 = listOf(Card.UN_BEAT)
        assertDuel(player0, player1, player2) { players ->
            DuelResult(
                retainedCards = mapOf(2 to player2),
                discardedCards = mapOf(0 to player0, 1 to player1),
                rounds = players.createRounds(
                    DuelRoundResult.DoubleDuel(
                        trapping = false,
                        doubleDuelers = mapOf(0 to player0.first(), 1 to player1.first()),
                    ),
                    DuelRoundResult.Counteracted(counteracters = setOf(1)),
                ),
            )
        }
    }

    @Test
    fun `double duel is trapped`() {
        val player0 = listOf(Card.CRUSHER, Card.ROBO_STRIKER)
        val player1 = listOf(Card.BRAINIAC, Card.TRAP)
        val player2 = listOf(Card.UN_BEAT)
        assertDuel(player0, player1, player2) { players ->
            DuelResult(
                retainedCards = mapOf(1 to player1.take(1), 2 to player2),
                discardedCards = mapOf(1 to listOf(Card.TRAP)),
                trappedCards = mapOf(1 to mapOf(0 to player0)),
                rounds = players.createRounds(
                    DuelRoundResult.DoubleDuel(
                        trapping = false,
                        doubleDuelers = mapOf(0 to player0.first(), 1 to player1.first()),
                    ),
                    DuelRoundResult.Trapped(trapper = 1),
                ),
            )
        }
    }

    @Test
    fun `three-way double duel with no traps`() {
        val player0 = listOf(Card.CRUSHER, Card.UN_BEAT)
        val player1 = listOf(Card.BRAINIAC, Card.BUZZY)
        val player2 = listOf(Card.CRUSHER, Card.UN_BEAT)
        assertDuel(player0, player1, player2) { players ->
            DuelResult(
                retainedCards = mapOf(0 to player0, 2 to player2),
                discardedCards = mapOf(1 to player1),
                rounds = players.createRounds(
                    DuelRoundResult.DoubleDuel(
                        trapping = false,
                        doubleDuelers = mapOf(0 to player0.first(), 1 to player1.first(), 2 to player2.first()),
                    ),
                    DuelRoundResult.LowestLost(
                        losers = mapOf(1 to player1.last()),
                        winners = mapOf(0 to player0.last(), 2 to player2.last()),
                    ),
                ),
            )
        }
    }

    @Test
    fun `three-way trapping double duel goes to highest card`() {
        val player0 = listOf(Card.TRAP, Card.ROBO_STRIKER)
        val player1 = listOf(Card.TRAP, Card.BUZZY)
        val player2 = listOf(Card.TRAP, Card.BUZZY)
        val player3 = listOf(Card.WIND)
        assertDuel(player0, player1, player2, player3) { players ->
            DuelResult(
                retainedCards = mapOf(0 to listOf(Card.ROBO_STRIKER)),
                discardedCards = mapOf(0 to listOf(Card.TRAP)),
                trappedCards = mapOf(0 to mapOf(1 to player1, 2 to player2, 3 to player3)),
                rounds = players.createRounds(
                    DuelRoundResult.DoubleDuel(
                        trapping = true,
                        doubleDuelers = mapOf(0 to player0.first(), 1 to player1.first(), 2 to player2.first()),
                    ),
                    DuelRoundResult.LowestLost(
                        losers = mapOf(1 to player1.last(), 2 to player2.last()),
                        winners = mapOf(0 to player0.last()),
                    ),
                ),
            )
        }
    }

    @Test
    fun `4-round trapping double duel goes to highest card`() {
        val player0 = listOf(Card.TRAP, Card.BRAINIAC, Card.TRAP, Card.ROBO_STRIKER)
        val player1 = listOf(Card.TRAP, Card.CRUSHER, Card.TRAP, Card.BUZZY)
        assertDuel(player0, player1) { players ->
            DuelResult(
                retainedCards = mapOf(0 to listOf(Card.BRAINIAC, Card.ROBO_STRIKER)),
                discardedCards = mapOf(0 to listOf(Card.TRAP, Card.TRAP)),
                trappedCards = mapOf(0 to mapOf(1 to player1)),
                rounds = players.createRounds(
                    DuelRoundResult.DoubleDuel(trapping = true, doubleDuelers = mapOf(0 to Card.TRAP, 1 to Card.TRAP)),
                    DuelRoundResult.DoubleDuel(
                        trapping = false,
                        doubleDuelers = mapOf(0 to Card.BRAINIAC, 1 to Card.CRUSHER),
                    ),
                    DuelRoundResult.DoubleDuel(trapping = true, doubleDuelers = mapOf(0 to Card.TRAP, 1 to Card.TRAP)),
                    DuelRoundResult.LowestLost(
                        losers = mapOf(1 to Card.BUZZY),
                        winners = mapOf(0 to Card.ROBO_STRIKER),
                    ),
                ),
            )
        }
    }

    private fun assertDuel(vararg cards: List<Card>, expectedResult: (players: List<List<Card>>) -> DuelResult) {
        val result = Dueler.duel(cards.map { Dueler.PlayerCardSupplier.FromCardList(it) })
        assertEquals(result, expectedResult(cards.toList()))

        val totalInputCards = cards.sumOf { it.size }
        val totalResultCards = result.discardedCards.values.sumOf { it.size } +
            result.retainedCards.values.sumOf { it.size } +
            result.trappedCards.values.sumOf { trappedCardByPlayer -> trappedCardByPlayer.values.sumOf { it.size } }
        assertEquals(totalInputCards, totalResultCards)
    }

    private fun List<List<Card>>.createRounds(vararg roundResults: DuelRoundResult): List<DuelRound> {
        return roundResults.mapIndexed { roundIndex, duelRoundResult ->
            DuelRound(
                result = duelRoundResult,
                playedCards = mapIndexedNotNull { playerIndex, playerCards ->
                    playerCards.getOrNull(roundIndex)?.let { playerIndex to it }
                }.toMap(),
                drawnCards = emptyMap(), // cards are never drawn for mocked players
            )
        }
    }
}
