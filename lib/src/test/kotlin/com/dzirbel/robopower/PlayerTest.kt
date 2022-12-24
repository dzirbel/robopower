package com.dzirbel.robopower

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlayerTest {
    private val game = GameImpl(playerFactories = listOf(InOrderPlayer, InOrderPlayer))

    @Test
    fun `invalid index choices are caught`() {
        val invalidChoicePlayer = object : Player(playerIndex = 0, game = game) {
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
        val exceptionThrowingPlayer = object : Player(playerIndex = 0, game = game) {
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
}
