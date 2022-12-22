package com.dzirbel.robopower

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MultiSetTest {
    @Test
    fun `count is accurate`() {
        val multiSet = MultiSet<String>()

        assertEquals(0, multiSet.count("a"))

        multiSet.add("a")
        assertEquals(1, multiSet.count("a"))
        assertEquals(0, multiSet.count("b"))

        multiSet.add("a")
        assertEquals(2, multiSet.count("a"))
        assertEquals(0, multiSet.count("b"))

        multiSet.addAll(listOf("a", "b", "a"))
        assertEquals(4, multiSet.count("a"))
        assertEquals(1, multiSet.count("b"))
        assertEquals(0, multiSet.count("c"))
    }
}
