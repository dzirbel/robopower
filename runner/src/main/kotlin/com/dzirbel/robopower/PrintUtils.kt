package com.dzirbel.robopower

import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.times

fun interface ProgressScope {
    fun onProgress()
}

// TODO document
@OptIn(ExperimentalTime::class)
inline fun withProgress(total: Int, incrementPercent: Int?, block: ProgressScope.() -> Unit): Duration {
    val start = TimeSource.Monotonic.markNow()

    val scope = if (incrementPercent == null) {
        ProgressScope { }
    } else {
        val increment = incrementPercent * total / 100
        val completeIncrement = AtomicInteger(0)
        val maxLength = (total - increment).toString().length
        val printPercent = total % 100 != 0 // only print progress percent if the total is not a multiple of 100

        ProgressScope {
            val complete = completeIncrement.incrementAndGet()

            if (complete in 1 until total && complete % increment == 0) {
                val elapsed = start.elapsedNow()
                val timeRemaining = (total - complete) * (elapsed / complete)

                val fraction = "${complete.toString().padStart(maxLength)} / $total "
                val percent = if (printPercent) "(${formatPercent(complete, total, padFrontChar = ' ')}) " else ""
                val time = "[${elapsed.formatAsSingleValue()} elapsed / ${timeRemaining.formatAsSingleValue()} left]"
                println(fraction + percent + time)
            }
        }
    }

    scope.block()

    return start.elapsedNow()
}
