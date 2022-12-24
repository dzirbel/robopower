package com.dzirbel.robopower

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

class DeckTest {
    @Test
    fun `draw returns the top card`() {
        val deck = Deck(drawPile = listOf(Card.BUZZY, Card.WIND))

        assertEquals(2, deck.drawPileSize)
        assertEquals(Card.WIND, deck.draw().first)
        assertEquals(1, deck.drawPileSize)
    }

    @Test
    fun `discard pile is in order`() {
        val deck = Deck(drawPile = emptyList())
        deck.discard(Card.BUZZY)
        deck.discard(Card.WIND)
        deck.discard(Card.ROBO_STRIKER)

        assertEquals(listOf(Card.BUZZY, Card.WIND, Card.ROBO_STRIKER), deck.discardPile)
        assertEquals(3, deck.discardPileSize)
    }

    @Test
    fun `discard pile is reshuffled into draw pile`() {
        val deck = Deck(Random(seed = 0))

        val drawnCards = mutableListOf<Card>()
        repeat(Card.deck.size) {
            val (card, previousDiscard) = deck.draw()
            assertNull(previousDiscard)
            drawnCards.add(card)
            deck.discard(card)
        }

        assertEquals(0, deck.drawPileSize)
        assertEquals(Card.deck.size, deck.discardPileSize)

        val (_, previousDiscard) = deck.draw()
        assertEquals(Card.deck.sorted(), previousDiscard?.sorted()) // all cards are the same

        assertEquals(0, deck.discardPileSize)
        assertEquals(Card.deck.size - 1, deck.drawPileSize)
    }

    @Test
    fun `draw returns cards until exhausted with no cards in discard pile`() {
        val deck = Deck(Random(seed = 0))

        val drawnCards = mutableMapOf<Card, Int>()
        repeat(Card.deck.size) {
            // draw but do not discard
            val (card, previousDiscard) = deck.draw()
            assertNull(previousDiscard)
            drawnCards.compute(card) { _, count -> (count ?: 0) + 1 }
        }

        assertThrows<Deck.DeckExhaustedException> { deck.draw() }

        assertEquals(Card.deck.size, drawnCards.values.sum())
        for ((card, drawnCount) in drawnCards) {
            assertEquals(card.multiplicity, drawnCount)
        }
    }
}
