package com.dzirbel.robopower

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlayerTest {
    private val playerState = PlayerState(playerIndex = 0, GameState(playerFactories = listOf()))

    @Test
    fun `roundsUntilUp is accurate`() {
        val gameState = GameState(playerFactories = listOf(InOrderPlayer, InOrderPlayer, InOrderPlayer, InOrderPlayer))
        gameState.upPlayerIndex = 1

        gameState.playerStates[0]._hand.add(Card.BUZZY)
        gameState.playerStates[1]._hand.add(Card.BUZZY)
        gameState.playerStates[3]._hand.add(Card.BUZZY)

        assertEquals(0, gameState.players[1].roundsUntilUp)
        assertEquals(1, gameState.players[3].roundsUntilUp)
        assertEquals(2, gameState.players[0].roundsUntilUp)

        assertThrows<IllegalStateException> { gameState.players[2].roundsUntilUp }
    }

    @Test
    fun `invalid index choices are caught`() {
        val invalidChoicePlayer = object : Player(playerState = playerState) {
            override fun discard() = -1
            override fun spy() = -1
            override fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>) = -1
        }

        // deal two cards so dueling needs a choice
        invalidChoicePlayer.deal(Card.BUZZY)
        invalidChoicePlayer.deal(Card.BUZZY)

        assertThrows<PlayerChoiceException> { invalidChoicePlayer.doDiscard() }
        assertThrows<PlayerChoiceException> { invalidChoicePlayer.doSpy() }
        assertThrows<PlayerChoiceException> {
            invalidChoicePlayer.doDuel(involvedPlayers = setOf(0, 1), previousRounds = emptyList())
        }
    }

    @Test
    fun `thrown exceptions are wrapped`() {
        val exceptionThrowingPlayer = object : Player(playerState = playerState) {
            override fun discard() = throw AssertionError(1)
            override fun spy() = throw AssertionError(2)
            override fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>) = throw AssertionError(3)
            override fun onDraw(card: Card) = throw AssertionError(4)
            override fun onCardStolen(card: Card, byPlayerIndex: Int) = throw AssertionError(5)
            override fun onReceiveSpyCard(card: Card, fromPlayerIndex: Int) = throw AssertionError(6)
        }

        // deal two cards so dueling needs a choice and stealing cards works
        exceptionThrowingPlayer.deal(Card.BUZZY)
        exceptionThrowingPlayer.deal(Card.BUZZY)

        val discardException = assertThrows<PlayerThrownException> { exceptionThrowingPlayer.doDiscard() }
        assertEquals(exceptionThrowingPlayer, discardException.player)
        assertEquals("1", discardException.cause?.message)

        val spyException = assertThrows<PlayerThrownException> { exceptionThrowingPlayer.doSpy() }
        assertEquals(exceptionThrowingPlayer, spyException.player)
        assertEquals("2", spyException.cause?.message)

        val duelException = assertThrows<PlayerThrownException> {
            exceptionThrowingPlayer.doDuel(involvedPlayers = setOf(0, 1), previousRounds = emptyList())
        }
        assertEquals(exceptionThrowingPlayer, duelException.player)
        assertEquals("3", duelException.cause?.message)

        val drawException = assertThrows<PlayerThrownException> { exceptionThrowingPlayer.draw(Card.BUZZY) }
        assertEquals(exceptionThrowingPlayer, drawException.player)
        assertEquals("4", drawException.cause?.message)

        val cardStolenException = assertThrows<PlayerThrownException> {
            exceptionThrowingPlayer.stealRandomCard(byPlayerIndex = 1)
        }
        assertEquals(exceptionThrowingPlayer, cardStolenException.player)
        assertEquals("5", cardStolenException.cause?.message)

        val stolenCardReceivedException = assertThrows<PlayerThrownException> {
            exceptionThrowingPlayer.receiveSpyCard(card = Card.BUZZY, fromPlayerIndex = 1)
        }
        assertEquals(exceptionThrowingPlayer, stolenCardReceivedException.player)
        assertEquals("6", stolenCardReceivedException.cause?.message)
    }

    @Test
    fun `dueling cards are re-added to the hand at the end`() {
        val dueledCards = mutableListOf(1, 4, 2)
        val player = object : Player(playerState = playerState) {
            override fun discard() = 0
            override fun spy() = 0
            override fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>) = dueledCards.removeFirst()

            fun assertHand(vararg cards: Card) {
                assertEquals(cards.toList(), hand)
            }
        }

        player.deal(Card.BUZZY)
        player.deal(Card.BUZZY)
        player.deal(Card.SHOCK)
        player.deal(Card.TRAP)
        player.deal(Card.LIGHTOR)
        player.deal(Card.ZIP)
        player.deal(Card.HAIRY)

        val card1 = player.doDuel(involvedPlayers = setOf(0, 1), previousRounds = emptyList())
        val card2 = player.doDuel(involvedPlayers = setOf(0, 1), previousRounds = emptyList())
        val card3 = player.doDuel(involvedPlayers = setOf(0, 1), previousRounds = emptyList())

        assertEquals(Card.BUZZY, card1)
        assertEquals(Card.ZIP, card2)
        assertEquals(Card.TRAP, card3)

        player.postDuel(retainedCards = listOf(card1, card2), trappedCards = listOf(Card.ROBO_STRIKER))

        player.assertHand(Card.BUZZY, Card.SHOCK, Card.LIGHTOR, Card.HAIRY, card1, card2, Card.ROBO_STRIKER)
    }
}
