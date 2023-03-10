package com.dzirbel.robopower.util

/**
 * A simple wrapper around a [MutableMap] which acts as a multi-set, counting the number of times an element is [add]ed.
 */
class MultiSet<E>(elements: Map<E, Int> = emptyMap()) {
    private val _map = elements.toMutableMap()

    val map: Map<E, Int>
        get() = _map.toMap()

    /**
     * The sum of counts for all elements.
     */
    val total
        get() = _map.values.sum()

    /**
     * A [Set] of elements which have been added.
     */
    val elements: Set<E>
        get() = _map.keys

    /**
     * Adds a count for [element].
     */
    fun add(element: E) {
        _map.compute(element) { _, count -> if (count == null) 1 else count + 1 }
    }

    /**
     * Adds a count for each element in [elements].
     */
    fun addAll(elements: Collection<E>) {
        for (element in elements) add(element)
    }

    /**
     * Removes a count for [element].
     */
    fun remove(element: E) {
        _map.compute(element) { _, count -> if (count == null) null else (count - 1).takeIf { it > 0 } }
    }

    /**
     * Removes a count for each element in [elements].
     */
    fun removeAll(elements: Collection<E>) {
        for (element in elements) remove(element)
    }

    /**
     * Returns the number of times [element] has been added.
     */
    fun count(element: E): Int = _map.getOrDefault(element, 0)

    /**
     * Returns the number of elements satisfying [predicate], respecting multiplicity.
     */
    fun count(predicate: (E) -> Boolean): Int {
        var count = 0
        for ((element, elementCount) in _map) {
            if (predicate(element)) count += elementCount
        }
        return count
    }

    /**
     * Returns a [List] with all the elements of this [MultiSet], respecting multiplicity, in an arbitrary order.
     */
    fun toListWithMultiplicity(): List<E> {
        return buildList {
            for ((element, count) in _map) {
                repeat(count) { add(element) }
            }
        }
    }
}
