package com.dzirbel.robopower

private object Assertions {
    val ENABLED: Boolean = javaClass.desiredAssertionStatus()
}

/**
 * Lazily runs [block] if assertions are enabled.
 */
internal fun ifAssertionsEnabled(block: () -> Unit) {
    if (Assertions.ENABLED) block()
}

/**
 * Computes [lazyValue] and asserts it is true only if assertions are enabled.
 */
internal fun assertLazy(lazyValue: () -> Boolean) {
    ifAssertionsEnabled { assert(lazyValue()) }
}
