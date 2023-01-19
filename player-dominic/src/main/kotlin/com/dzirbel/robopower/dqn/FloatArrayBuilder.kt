package com.dzirbel.robopower.dqn

/**
 * Simple DSL to build [FloatArray]s conveniently via [buildFloatArray].
 */
internal fun interface FloatArrayBuilder {
    fun add(number: Number)
}

internal fun buildFloatArray(size: Int, builder: FloatArrayBuilder.() -> Unit): FloatArray {
    val array = FloatArray(size)
    var index = 0

    FloatArrayBuilder { number -> array[index++] = number.toFloat() }.builder()
    require(index == size) { "incorrect number of elements in FloatArray builder; expected $size but was $index" }

    return array
}
