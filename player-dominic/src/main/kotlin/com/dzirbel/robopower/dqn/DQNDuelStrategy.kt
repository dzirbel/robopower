package com.dzirbel.robopower.dqn

import com.dzirbel.robopower.Card
import com.dzirbel.robopower.DuelRound
import com.dzirbel.robopower.DuelStrategy
import com.dzirbel.robopower.PlayerState
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.inference.InferenceModel
import org.jetbrains.kotlinx.dl.api.inference.TensorFlowInferenceModel
import java.io.File

val DuelInput = Input(
    (1 + 1 + Card.values().size).toLong(),
    name = "duel input",
)

// TODO also include duel state in the input
fun PlayerState.toInput(card: Card): FloatArray {
    return buildList {
        add(card.ordinal.toFloat())

        add(gameState.activePlayerCount.toFloat())

        addAll(Card.values().map { card -> hand.count { it == card }.toFloat() })
    }.toFloatArray()
}

class DQNDuelStrategy internal constructor(
    private val model: InferenceModel,
    private val training: Boolean,
) : DuelStrategy {
    private val _states = mutableListOf<FloatArray>()
    val states: List<FloatArray>
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
            val input = playerState.toInput(card = card)
            val prediction = model.predictSoftly(input)[0]

            if (maxPrediction == null || prediction > maxPrediction) {
                maxPrediction = prediction
                bestCard = card
                finalInput = input
            }
        }

        if (training) {
            _states.add(requireNotNull(finalInput))
        }

        return playerState.hand.indexOf(requireNotNull(bestCard))
    }
}
