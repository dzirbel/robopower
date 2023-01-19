package com.dzirbel.robopower

import com.dzirbel.robopower.dqn.DQNDuelStrategy
import com.dzirbel.robopower.dqn.DuelInput
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlinx.dl.api.core.GraphTrainableModel
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.WritingMode
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.reshaping.Flatten
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.api.core.optimizer.Optimizer
import org.jetbrains.kotlinx.dl.api.inference.TensorFlowInferenceModel
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

// number of players in each training game
private const val PLAYERS = 4

// number of games played in each batch, after which the model is re-fit with the data from these games
private const val BATCH_SIZE = 5

// number of batches to run
private const val BATCHES = 50

private const val WIN_REWARD = 5
private const val TIE_REWARD = 2

private const val FIT_EPOCHS = 5
private const val FIT_BATCH_SIZE = 32

// delegate player used for non-dueling strategies while training
private val trainingBasePlayer: Player.Factory = LessSimplePlayer

private val modelLayers: Sequential = Sequential.of(
    DuelInput,
    Flatten(),
    Dense(500),
    Dense(200),
    Dense(50),
)

private val optimizer: Optimizer = Adam(learningRate = 0.00025f)

// player providing non-duel strategies when validating model
private val validationBasePlayer: Player.Factory = PseudoCodePseudoPlayer

// other players in the game when validating model
private val validationPlayers = listOf(
    PseudoCodePseudoPlayer,
    SimplePlayer,
    RandomPlayer,
)

// number of games to run after training to see performance, set to 0 to skip
private const val VALIDATION_GAMES = 1_000

// integer percentage at which progress reports are printed during training and validation
private const val PROGRESS_INCREMENT_PERCENT = 10

private data class TrainingData(val inputs: List<FloatArray>, val result: Int)

fun main() {
    modelLayers.use { model ->
        initModel(model)
        trainModel(model)
        val modelFile = saveModel(model = model)
        validateModel(modelFile)
    }
}

@OptIn(ExperimentalTime::class)
private fun initModel(model: GraphTrainableModel) {
    print("Initializing model... ")
    val start = TimeSource.Monotonic.markNow()

    // TODO extract these above
    model.compile(
        optimizer = optimizer,
        loss = Losses.SOFT_MAX_CROSS_ENTROPY_WITH_LOGITS,
        metric = Metrics.MSE,
    )

    model.init()

    @Suppress("SpreadOperator")
    model.reshape(*DuelInput.packedDims)

    println("done in ${start.elapsedNow()}.")
}

private fun trainModel(model: GraphTrainableModel) {
    println("Running training games: $BATCHES batches of $BATCH_SIZE games each...")
    val duration = withProgress(total = BATCHES, incrementPercent = PROGRESS_INCREMENT_PERCENT) {
        repeat(BATCHES) {
            val data = mutableListOf<TrainingData>()

            repeat(BATCH_SIZE) { data.addAll(runGame(model = model)) }

            fitData(model = model, data = data)

            onProgress()
        }
    }
    println("Training complete in $duration!")
}

private fun runGame(model: GraphTrainableModel): List<TrainingData> {
    val baseStrategies = List(PLAYERS) { DQNDuelStrategy(model = model) }
    val trainingStrategies = baseStrategies.map { DuelTrainingStrategy(it) }

    val game = Game(trainingStrategies.map { trainingBasePlayer.withStrategies(duelStrategy = it) })
    val result = requireNotNull(game.run())

    val data = mutableListOf<TrainingData>()

    repeat(PLAYERS) { playerIndex ->
        // TODO gradual rewards for placing second, etc
        val reward = when (result) {
            is GameResult.Winner -> if (result.winner == playerIndex) WIN_REWARD else 0
            is GameResult.Tied -> if (playerIndex in result.tiedPlayers) TIE_REWARD else 0
        }

        data.add(TrainingData(inputs = trainingStrategies[playerIndex].inputs, result = reward))
    }

    return data
}

private fun fitData(model: GraphTrainableModel, data: List<TrainingData>) {
    val dataset = OnHeapDataset.create(
        features = data.flatMap { (inputs, _) -> inputs }.toTypedArray(),
        labels = data.flatMap { (inputs, reward) -> List(inputs.size) { reward.toFloat() } }.toFloatArray(),
    )

    model.fit(dataset = dataset, epochs = FIT_EPOCHS, batchSize = FIT_BATCH_SIZE)
}

@OptIn(ExperimentalTime::class)
private fun saveModel(model: GraphTrainableModel): File {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
    val filename = "models/duel_${now.format(formatter)}"
    val file = File(filename)

    print("Saving model to $filename... ")
    val start = TimeSource.Monotonic.markNow()

    model.save(file, writingMode = WritingMode.FAIL_IF_EXISTS)

    println("done in ${start.elapsedNow()}.")

    return file
}

private fun validateModel(modelFile: File) {
    @Suppress("KotlinConstantConditions")
    if (VALIDATION_GAMES == 0) return

    val results = TensorFlowInferenceModel.load(modelFile).use { model ->
        @Suppress("SpreadOperator")
        model.reshape(*DuelInput.packedDims)

        val player = validationBasePlayer.withStrategies(duelStrategy = DQNDuelStrategy(model = model))
        val input = RunInput(
            games = VALIDATION_GAMES,
            concurrency = 1,
            verbose = true,
            players = validationPlayers.plus(player),
        )

        println()
        println("Running validation games...")
        runBlocking { Runner.run(input, printIncrementPercent = PROGRESS_INCREMENT_PERCENT) }
    }

    val playerIndex = validationPlayers.size

    if (results.successfulGames == results.input.games) {
        println("All games completed successfully.")
    } else {
        val exceptions: Int = results.playerChoiceExceptions.count(playerIndex) +
            results.playerThrownExceptions.count(playerIndex)
        println("Exceptions thrown by model: $exceptions")
    }

    println("Model win rate: ${formatPercent(results.winCounts.count(playerIndex), results.successfulGames)}")

    println("Other player win rates:")
    for ((otherPlayerIndex, otherPlayer) in validationPlayers.withIndex()) {
        val winrate = formatPercent(results.winCounts.count(otherPlayerIndex), results.successfulGames)
        println("  ${otherPlayer.playerName(otherPlayerIndex)} : $winrate")
    }
}
