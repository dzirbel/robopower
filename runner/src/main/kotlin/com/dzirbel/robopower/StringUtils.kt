package com.dzirbel.robopower

/**
 * Returns a string representation of this [Double] with exactly [places] decimal places.
 */
fun Double.roundTo(places: Int = 2): String = String.format("%.${places}f", this)

/**
 * Formats [numerator] and [denominator] as a percent (including the %), with 2 decimal places.
 */
fun formatPercent(numerator: Int, denominator: Int): String {
    return (100 * (numerator.toDouble() / denominator)).roundTo(places = 2) + "%"
}
