package com.dzirbel.robopower

import java.util.concurrent.atomic.AtomicInteger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.times

/**
 * Builder scope for computation done [withProgress]; [onProgress] should be called whenever one iteration of progress
 * has been completed.
 */
fun interface ProgressScope {
    fun onProgress()
}

/**
 * Print utility which logs user-readable progress on computation done in [block] to standard output and returns the
 * [Duration] spend executing [block].
 *
 * [block] is invoked with a [ProgressScope] that implements the logging, and should invoke [ProgressScope.onProgress]
 * whenever a unit of progress is completed. This allows (as opposed to e.g. having a for-loop inside [withProgress])
 * [block] to organize its work concurrently and use the [ProgressScope.onProgress] callback arbitrarily.
 * [ProgressScope.onProgress] should be called [total] times when [block] exists for the progress reports to be
 * accurate.
 */
@OptIn(ExperimentalTime::class, ExperimentalContracts::class)
fun withProgress(total: Int, incrementPercent: Int?, block: ProgressScope.() -> Unit): Duration {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

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
