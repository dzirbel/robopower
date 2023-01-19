package com.dzirbel.robopower

import com.dzirbel.robopower.dqn.toDuelInput

/**
 * Wrapper around a [delegate] strategy which records its dueling choices and accumulates them alongside the game state
 * as [inputs].
 */
internal class DuelTrainingStrategy(private val delegate: DuelStrategy) : DuelStrategy {
    private val _inputs: MutableList<FloatArray> = mutableListOf()

    val inputs: List<FloatArray>
        get() = _inputs.toList()

    override fun duel(playerState: PlayerState, involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int {
        val choice = delegate.duel(playerState, involvedPlayers, previousRounds)

        _inputs.add(
            playerState.toDuelInput(
                playedCard = playerState.hand[choice],
                involvedPlayers = involvedPlayers,
                previousRounds = previousRounds,
            ),
        )

        return choice
    }
}
