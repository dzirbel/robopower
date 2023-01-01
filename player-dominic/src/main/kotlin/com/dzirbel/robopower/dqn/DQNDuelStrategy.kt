package com.dzirbel.robopower.dqn

import com.dzirbel.robopower.Card
import com.dzirbel.robopower.DuelRound
import com.dzirbel.robopower.DuelStrategy
import com.dzirbel.robopower.PlayerState
import org.jetbrains.kotlinx.dl.api.inference.InferenceModel
import org.jetbrains.kotlinx.dl.api.inference.TensorFlowInferenceModel
import java.io.File

class DQNDuelStrategy internal constructor(
    private val model: InferenceModel,
    training: Boolean,
) : DuelStrategy {
    private val _states: MutableList<FloatArray>? = if (training) mutableListOf() else null
    val states: List<FloatArray>?
        get() = _states

    constructor(modelFilename: String) : this(
        // TODO model is never closed
        model = TensorFlowInferenceModel.load(File(modelFilename)).also {
            @Suppress("SpreadOperator")
            it.reshape(*DuelInput.packedDims)
        },
        training = false,
    )

    override fun duel(playerState: PlayerState, involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int {
        // TODO names
        var maxPrediction: Float? = null
        var bestCard: Card? = null
        var finalInput: FloatArray? = null

        for (card in playerState.hand.toSet()) {
            // TODO build base input array once and then copy and modify for each card choice?
            val input = playerState.toDuelInput(
                playedCard = card,
                involvedPlayers = involvedPlayers,
                previousRounds = previousRounds,
            )

            val prediction = model.predictSoftly(input)[0]

            if (maxPrediction == null || prediction > maxPrediction) {
                maxPrediction = prediction
                bestCard = card
                finalInput = input
            }
        }

        _states?.add(requireNotNull(finalInput))

        return playerState.hand.indexOf(requireNotNull(bestCard))
    }
}
