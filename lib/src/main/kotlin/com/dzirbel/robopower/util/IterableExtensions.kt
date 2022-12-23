package com.dzirbel.robopower.util

/**
 * Returns the index of the first element matching the given [predicate], or null if the list does not contain any such
 * element.
 */
fun <T> Iterable<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    return indexOfFirst(predicate).takeIf { it != -1 }
}

/**
 * Returns the first element yielding the largest value of [selector], ignoring null values, or null if there are no
 * such elements.
 */
fun <T, R : Comparable<R>> Iterable<T>.maxByNullableOrNull(selector: (T) -> R?): T? {
    var maxElement: T? = null
    var maxValue: R? = null
    for (element in this) {
        val value = selector(element) ?: continue
        if (maxValue == null || value > maxValue) {
            maxElement = element
            maxValue = value
        }
    }

    return maxElement
}

/**
 * Returns the first element yielding the smallest value of [selector], ignoring null values, or null if there are no
 * such elements.
 */
fun <T, R : Comparable<R>> Iterable<T>.minByNullableOrNull(selector: (T) -> R?): T? {
    var minElement: T? = null
    var minValue: R? = null
    for (element in this) {
        val value = selector(element) ?: continue
        if (minValue == null || value < minValue) {
            minElement = element
            minValue = value
        }
    }

    return minElement
}

/**
 * Returns the index of the value for which [selector] returns the maximum non-null value, or null if no such indices
 * exist.
 */
fun <T, R : Comparable<R>> Iterable<T>.indexOfMaxOrNull(selector: (T) -> R?): Int? {
    var maxIndex: Int? = null
    var maxValue: R? = null
    forEachIndexed { index, element ->
        val value = selector(element)
        @Suppress("UnsafeCallOnNullableType")
        if (value != null && (maxValue == null || value > maxValue!!)) {
            maxIndex = index
            maxValue = value
        }
    }

    return maxIndex
}

/**
 * Returns the index of the value for which [selector] returns the minimum non-null value, or null if no such indices
 * exist.
 */
fun <T, R : Comparable<R>> Iterable<T>.indexOfMinOrNull(selector: (T) -> R?): Int? {
    var maxIndex: Int? = null
    var maxValue: R? = null
    forEachIndexed { index, element ->
        val value = selector(element)
        @Suppress("UnsafeCallOnNullableType")
        if (value != null && (maxValue == null || value < maxValue!!)) {
            maxIndex = index
            maxValue = value
        }
    }

    return maxIndex
}
