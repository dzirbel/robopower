package com.dzirbel.robopower.dqn

import com.dzirbel.robopower.DuelRound
import com.dzirbel.robopower.DuelStrategy
import com.dzirbel.robopower.PlayerState
import com.dzirbel.robopower.util.maxFirstIndexBy
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
}
