package com.dzirbel.robopower

import com.dzirbel.robopower.util.MultiSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit

object Runner {
    /**
     * Runs games based on [input].
     *
     * @param input specifies the parameters for the games
     * @param printIncrementPercent whole-number percentage of [RunInput.games] when a progress message should be
     *  printed, e.g. 5 = 5%
     */
    @OptIn(FlowPreview::class)
    fun run(
        input: RunInput,
        @Suppress("MagicNumber")
        printIncrementPercent: Int? = if (input.verbose) 5 else 20,
    ): RunResults {
        val winCounts = MultiSet<Int>()
        val winCountByPosition = MultiSet<Int>()
        val tieCounts = MultiSet<Int>()
        val tieCountByPosition = MultiSet<Int>()
        val playerLogicTime = mutableMapOf<Int, Duration>()
        var totalTies = 0
        val roundCounts = MultiSet<Int>()

        val playerChoiceExceptions = MultiSet<Int>()
        val playerChoiceExceptionExamples = mutableMapOf<Int, Throwable>()
        val playerThrownExceptions = MultiSet<Int>()
        val playerThrownExceptionExamples = mutableMapOf<Int, Throwable>()
        var engineExceptions = 0
        var engineExceptionExample: Throwable? = null

        println("Running ${input.games} ${input.players.size}-player games with parallelism of ${input.concurrency}")

        val elapsed = withProgress(total = input.games, incrementPercent = printIncrementPercent) {
            runBlocking(Dispatchers.Default) {
                flow {
                    repeat(input.games) { emit(Unit) }
                }
                    .flatMapMerge(concurrency = input.concurrency) {
                        flow {
                            val factories = if (input.randomizeOrder) {
                                input.players.withIndex().shuffled()
                            } else {
                                input.players.withIndex().toList()
                            }

                            val result = runCatching {
                                requireNotNull(Game(playerFactories = factories.map { it.value }).run())
                            }

                            emit(Pair(factories, result))
                        }
                    }
                    .collect { (factories, result) ->
                        result
                            .onSuccess { gameResult ->
                                factories.forEachIndexed { gameIndex, (originalIndex, _) ->
                                    val player = gameResult.game.gameState.players[gameIndex]
                                    playerLogicTime.compute(originalIndex) { _, duration ->
                                        (duration ?: Duration.ZERO) + player.totalPlayerLogicTime
                                    }
                                }

                                roundCounts.add(gameResult.game.gameState.turnCount)
                                when (gameResult) {
                                    is GameResult.Winner -> {
                                        winCounts.add(factories[gameResult.winner].index)
                                        winCountByPosition.add(gameResult.winner)
                                    }
                                    is GameResult.Tied -> {
                                        totalTies++
                                        tieCounts.addAll(gameResult.tiedPlayers.map { factories[it].index })
                                        tieCountByPosition.addAll(gameResult.tiedPlayers)
                                    }
                                }
                            }
                            .onFailure { throwable ->
                                when (throwable) {
                                    is PlayerChoiceException -> {
                                        val playerIndex = throwable.player.playerIndex
                                        val factoryIndex = factories[playerIndex].index
                                        playerChoiceExceptionExamples.putIfAbsent(factoryIndex, throwable)
                                        playerChoiceExceptions.add(factoryIndex)
                                    }

                                    is PlayerThrownException -> {
                                        val playerIndex = throwable.player.playerIndex
                                        val factoryIndex = factories[playerIndex].index
                                        playerThrownExceptionExamples.putIfAbsent(factoryIndex, throwable)
                                        playerThrownExceptions.add(factoryIndex)
                                    }

                                    else -> {
                                        if (engineExceptionExample == null) {
                                            engineExceptionExample = throwable
                                        }
                                        engineExceptions++
                                    }
                                }
                            }

                        onProgress()
                    }
            }
        }

        val gamesPerSecond = (input.games / elapsed.toDouble(DurationUnit.SECONDS)).roundToInt()
        println("Done in $elapsed (avg ${elapsed / input.games} per game; $gamesPerSecond games/s)")
        println()

        return RunResults(
            input = input,
            winCounts = winCounts,
            winCountByPosition = winCountByPosition,
            tieCounts = tieCounts,
            tieCountByPosition = tieCountByPosition,
            totalTies = totalTies,
            roundCounts = roundCounts,
            playerLogicTime = playerLogicTime,
            playerChoiceExceptions = playerChoiceExceptions,
            playerChoiceExceptionExamples = playerChoiceExceptionExamples,
            playerThrownExceptions = playerThrownExceptions,
            playerThrownExceptionExamples = playerThrownExceptionExamples,
            engineExceptions = engineExceptions,
            engineExceptionExample = engineExceptionExample,
        )
    }
}
