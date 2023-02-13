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

/**
 * Formats this [Duration] into a single numeric value with the non-zero smallest unit, for example "1.33 hr" instead of
 * "1 hr 20 min".
 *
 * @param padFrontChar character to pad the start of the string to two whole characters (so e.g. 3 minutes is aligned
 *  with 30 minutes), null for no padding. Note that durations 100 hours or longer may not be aligned.
 * @param decimals number of decimal characters in the string
 *
 * TODO unit test
 */
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

/**
 * Pads the start of this numeric [String] with [padChar] to be at least [digits] long.
 *
 * For example, for the string 12.34 with digits=4, two characters of padding will be prepended.
 *
 * TODO unit test
 */
fun String.padWholeNumber(digits: Int, padChar: Char = ' '): String {
    val leading = indexOf('.')
    return if (leading < digits) {
        buildString {
            repeat(digits - leading) { append(padChar) }
            append(this@padWholeNumber)
        }
    } else {
        this
    }
}
