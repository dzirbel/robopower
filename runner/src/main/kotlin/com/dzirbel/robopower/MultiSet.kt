package com.dzirbel.robopower

/**
 * A simple wrapper around a [MutableMap] which acts as a multi-set, counting the number of times an element is [add]ed.
 */
class MultiSet<E> {
    private val map = mutableMapOf<E, Int>()

    /**
     * The sum of counts for all elements.
     */
    val total
        get() = map.values.sum()

    /**
     * Adds a count for [element].
     */
    fun add(element: E) {
        map.compute(element) { _, count -> if (count == null) 1 else count + 1 }
    }

    /**
     * Adds a count for each element in [elements].
     */
    fun addAll(elements: Collection<E>) {
        for (element in elements) add(element)
    }

    /**
     * Returns the number of times [element] has been added.
     */
    fun count(element: E): Int = map.getOrDefault(element, 0)
}
