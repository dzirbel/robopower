package com.dzirbel.robopower.util

/**
 * Returns the key(s) whose associated values are the largest according to [comparator].
 */
internal fun <K, V> Map<K, V>.maxKeysBy(comparator: Comparator<V>): Set<K> {
    val iterator = iterator()
    if (!iterator.hasNext()) throw NoSuchElementException()

    val first = iterator.next()
    var maxValue = first.value
    val maxKeys = mutableSetOf(first.key)

    while (iterator.hasNext()) {
        val (key, value) = iterator.next()
        val comparison = comparator.compare(value, maxValue)
        if (maxValue == null || comparison > 0) {
            maxValue = value
            maxKeys.clear()
            maxKeys.add(key)
        } else if (comparison == 0) {
            maxKeys.add(key)
        }
    }

    return maxKeys
}

/**
 * Returns the first key (or an arbitrary key for unordered maps) whose value yields the maximum non-null result for
 * [selector].
 */
fun <K, V, R : Comparable<R>> Map<K, V>.maxKeyByOrNull(selector: (V) -> R?): K? {
    return this
        .mapNotNull { (key, value) -> selector(value)?.let { key to it } }
        .maxByOrNull { (_, value) -> value }
        ?.first
}
