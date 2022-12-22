package com.dzirbel.robopower

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardTest {
    @Test
    fun `99 total cards`() {
        assertEquals(99, Card.values().sumOf { it.multiplicity })
        assertEquals(99, Card.deck.size)
    }
}
