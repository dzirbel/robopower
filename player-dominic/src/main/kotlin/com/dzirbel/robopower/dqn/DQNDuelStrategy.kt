package com.dzirbel.robopower.dqn

import com.dzirbel.robopower.DuelRound
import com.dzirbel.robopower.DuelStrategy
import com.dzirbel.robopower.PlayerState
import org.jetbrains.kotlinx.dl.api.inference.InferenceModel

class DQNDuelStrategy(private val model: InferenceModel) : DuelStrategy {
    override fun duel(playerState: PlayerState, involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int {
        val input = playerState.baseDuelInput(involvedPlayers = involvedPlayers, previousRounds = previousRounds)

        return playerState.hand.maxFirstIndexBy { card ->
            input.setPlayedCard(card) // adjust input buffer in-place for performance

            // TODO predictions are not thread safe (https://github.com/Kotlin/kotlindl/issues/250), so running duels
            //  in parallel may cause problems
            model.predictSoftly(inputData = input)[0]
        }
    }

    /**
     * Returns the index of the first element in this [List] maximizing [selector].
     *
     * Includes optimizations to invoke [selector] only when necessary: never if this [List] has a single element and
     * saving the set of elements that it has been evaluated on so duplicates are skipped. As such, [T] must be properly
     * hashable and [selector] must always evaluate to the same result on equal items.
     *
     * TODO extract and unit test
     */
    private fun <T, R : Comparable<R>> List<T>.maxFirstIndexBy(selector: (T) -> R): Int {
        return when (size) {
            0 -> throw NoSuchElementException()
            1 -> 0
            else -> {
                val iterator = iterator()

                var maxIndex = 0
                var maxValue: R = selector(iterator.next())
                val seenElements = mutableSetOf<T>()

                var index = 1
                while (iterator.hasNext()) {
                    val element = iterator.next()
                    if (seenElements.add(element)) {
                        val value = selector(element)
                        if (value > maxValue) {
                            maxValue = value
                            maxIndex = index
                        }
                    }

                    index++
                }

                maxIndex
            }
        }
    }
}
