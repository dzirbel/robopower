package com.dzirbel.robopower.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MapExtensionsTest {
    @Test
    fun `maxKeysBy for empty map`() {
        assertThrows<NoSuchElementException> { emptyMap<String, Int>().maxKeysBy(Comparator.naturalOrder()) }
    }

    @Test
    fun `maxKeysBy for unique maximum`() {
        val map = mapOf("a" to 1, "b" to 2, "c" to 3, "d" to 0)
        val maxKeys = map.maxKeysBy(Comparator.naturalOrder())
        assertEquals(setOf("c"), maxKeys)
    }

    @Test
    fun `maxKeysBy for duplicate maximum`() {
        val map = mapOf("a" to 1, "b" to 2, "c" to 2, "d" to 0)
        val maxKeys = map.maxKeysBy(Comparator.naturalOrder())
        assertEquals(setOf("b", "c"), maxKeys)
    }

    @Test
    fun `maxKeyByOrNull for empty map`() {
        assertNull(emptyMap<String, Int>().maxKeyByOrNull { it })
    }

    @Test
    fun `maxKeyByOrNull all null selector`() {
        val map = mapOf("a" to 1, "b" to 2, "c" to 3, "d" to 0)
        assertNull(map.maxKeyByOrNull { null })
    }

    @Test
    fun `maxKeyByOrNull for unique maximum`() {
        val map = mapOf("a" to 1, "b" to 2, "c" to 3, "d" to 0)
        assertEquals("b", map.maxKeyByOrNull { if (it % 2 == 0) it else null })
    }

    @Test
    fun `maxKeyByOrNull for duplicate maximum`() {
        val map = mapOf("a" to 1, "b" to 2, "c" to 2, "d" to 0)
        assertTrue(map.maxKeyByOrNull { if (it % 2 == 0) it else null } in setOf("b", "c"))
    }
}
