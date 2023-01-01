package com.dzirbel.robopower.dqn

import com.dzirbel.robopower.Game
import com.dzirbel.robopower.GameResult
import com.dzirbel.robopower.SimplePlayer
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.WritingMode
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.reshaping.Flatten
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import java.io.File

private const val PLAYERS = 4
private const val BATCH_SIZE = 5
private const val BATCHES = 100

private const val WIN_REWARD = 5
private const val TIE_REWARD = 2

private val modelLayers: Sequential = Sequential.of(
    DuelInput,
    Flatten(),
    Dense(300),
    Dense(100),
    Dense(10),
)

@Suppress("MagicNumber")
fun main() {
    modelLayers.use { model ->
        model.compile(
            optimizer = Adam(learningRate = 0.00025f),
            loss = Losses.SOFT_MAX_CROSS_ENTROPY_WITH_LOGITS,
            metric = Metrics.ACCURACY,
        )

        model.init()

        // TODO is this necessary every time?
        @Suppress("SpreadOperator")
        model.reshape(*DuelInput.packedDims)

        println("Compiled model")

        repeat(BATCHES) { batchIndex ->
            println("Running batch ${batchIndex + 1} / $BATCHES")

            val choices = mutableListOf<Pair<List<FloatArray>, Int>>()

            repeat(BATCH_SIZE) { _ ->
                val strategies = List(PLAYERS) { DQNDuelStrategy(model = model, training = true) }

                val game = Game(strategies.map { SimplePlayer.withStrategies(duelStrategy = it) })
                val result = requireNotNull(game.run())

                repeat(PLAYERS) { playerIndex ->
                    // TODO gradual rewards for placing second, etc
                    val reward = when (result) {
                        is GameResult.Winner -> if (result.winner == playerIndex) WIN_REWARD else 0
                        is GameResult.Tied -> if (playerIndex in result.tiedPlayers) TIE_REWARD else 0
                    }

                    val states = requireNotNull(strategies[playerIndex].states)
                    choices.add(states to reward)
                }
            }

            val dataset = OnHeapDataset.create(
                features = choices.flatMap { (inputs, _) -> inputs }.toTypedArray(),
                labels = choices.flatMap { (inputs, reward) -> List(inputs.size) { reward.toFloat() } }.toFloatArray(),
            )

            // TODO reconsider fitting params
            model.fit(dataset = dataset, epochs = 10, batchSize = 10)
        }

        // TODO add date etc to filename
        val filename = "models/duel_model"

        println("Complete! Saving to $filename")

        model.save(File(filename), writingMode = WritingMode.OVERRIDE)
    }
}
