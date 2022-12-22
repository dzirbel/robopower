package com.dzirbel.robopower

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

class DeckTest {
    @Test
    fun `draw returns cards until exhausted`() {
        val totalCards = Card.values().sumOf { it.multiplicity }
        val deck = Deck(Random(seed = 0))

        val drawnCards = mutableMapOf<Card, Int>()
        repeat(totalCards) {
            val card = deck.draw().first
            drawnCards.compute(card) { _, count -> (count ?: 0) + 1 }
        }

        assertThrows<Deck.DeckExhaustedException> { deck.draw() }

        assertEquals(totalCards, drawnCards.values.sum())
        for ((card, drawnCount) in drawnCards) {
            assertEquals(card.multiplicity, drawnCount)
        }
    }
}
