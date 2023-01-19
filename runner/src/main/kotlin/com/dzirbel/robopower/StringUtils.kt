package com.dzirbel.robopower

import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Returns a string representation of this [Double] with exactly [places] decimal places.
 */
fun Double.roundTo(places: Int = 2): String = String.format("%.${places}f", this)

/**
 * Formats [numerator] and [denominator] as a percent (including the %), with [places] decimal places (default 2).
 */
fun formatPercent(numerator: Int, denominator: Int, padFrontChar: Char? = null, places: Int = 2): String {
    return ((100 * (numerator.toDouble() / denominator)).roundTo(places = places) + "%")
        .let {
            if (padFrontChar != null) it.padWholeNumber(digits = 2, padChar = padFrontChar) else it
        }
}

// TODO document
// TODO unit test
fun Duration.formatAsSingleValue(
    padFrontChar: Char? = ' ',
    decimals: Int = 2,
): String {
    val unit = toComponents { hours, minutes, _, _ ->
        when {
            minutes == 0 -> DurationUnit.SECONDS
            hours == 0L -> DurationUnit.MINUTES
            else -> DurationUnit.HOURS
        }
    }

    val base = toString(unit = unit, decimals = decimals)

    // pad with 2 digits because seconds and minutes can only take that much up; there will be some ugliness if >=100
    // hours
    return if (padFrontChar != null) base.padWholeNumber(digits = 2) else base
}

// TODO document
// TODO unit test
fun String.padWholeNumber(digits: Int, padChar: Char = ' '): String {
    val leading = substringBefore('.').length // TODO avoid building the string, just get the count
    return if (leading < digits) {
        buildString {
            repeat(digits - leading) { append(padChar) }
            append(this@padWholeNumber)
        }
    } else {
        this
    }
}
