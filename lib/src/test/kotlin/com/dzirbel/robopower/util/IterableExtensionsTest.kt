package com.dzirbel.robopower.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class IterableExtensionsTest {
    private val selectorMod5 = { value: Int? -> if (value != null) value % 5 else null }

    @Test
    fun `indexOfFirstOrNull with element found`() {
        assertEquals(2, listOf(1, 2, 3, 4).indexOfFirstOrNull { it % 3 == 0 })
    }

    @Test
    fun `indexOfFirstOrNull with duplicate elements`() {
        assertEquals(2, listOf(1, 2, 3, 4, 6, 9).indexOfFirstOrNull { it % 3 == 0 })
    }

    @Test
    fun `indexOfFirstOrNull with no matching element`() {
        assertNull(listOf(1, 2, 4, 5, 7, 8).indexOfFirstOrNull { it % 3 == 0 })
    }

    @Test
    fun `maxByNullableOrNull and indexOfMaxOrNull return the max value`() {
        val list = listOf(1, 7, null, 4, 3)
        assertEquals(3, list.indexOfMaxOrNull(selectorMod5))
        assertEquals(4, list.maxByNullableOrNull(selectorMod5))
    }

    @Test
    fun `maxByNullableOrNull and indexOfMaxOrNull return the first max value when tied`() {
        val list = listOf(1, 7, null, 4, 3, 9)
        assertEquals(3, list.indexOfMaxOrNull(selectorMod5))
        assertEquals(4, list.maxByNullableOrNull(selectorMod5))
    }

    @Test
    fun `maxByNullableOrNull and indexOfMaxOrNull return null when no values are non-null`() {
        val list = listOf(1, 7, null, 4, 3)
        assertEquals(null, list.indexOfMaxOrNull { null })
        assertEquals(null, list.maxByNullableOrNull { null })
    }

    @Test
    fun `minByNullableOrNull and indexOfMinOrNull return the min value`() {
        val list = listOf(2, null, 6, 4, 3)
        assertEquals(2, list.indexOfMinOrNull(selectorMod5))
        assertEquals(6, list.minByNullableOrNull(selectorMod5))
    }

    @Test
    fun `minByNullableOrNull and indexOfMinOrNull return the first min value when tied`() {
        val list = listOf(2, null, 6, 4, 1, 3)
        assertEquals(2, list.indexOfMinOrNull(selectorMod5))
        assertEquals(6, list.minByNullableOrNull(selectorMod5))
    }

    @Test
    fun `minByNullableOrNull and indexOfMinOrNull return null when no values are non-null`() {
        val list = listOf(2, null, 6, 4, 3)
        assertEquals(null, list.indexOfMinOrNull { null })
        assertEquals(null, list.minByNullableOrNull { null })
    }
}
