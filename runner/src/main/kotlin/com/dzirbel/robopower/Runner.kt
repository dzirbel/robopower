package com.dzirbel.robopower

import com.dzirbel.robopower.util.MultiSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.times

object Runner {
    @OptIn(FlowPreview::class, ExperimentalTime::class)
    suspend fun run(input: RunInput, printIncrementPercent: Int? = 5): RunResults {
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

        val increment = printIncrementPercent?.let { it * input.games / 100 }
        var gamesFinished = 0

        println("Running ${input.games} ${input.players.size}-player games with parallelism of ${input.concurrency}")
        val start = TimeSource.Monotonic.markNow()

        withContext(Dispatchers.Default) {
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
                            requireNotNull(GameImpl(playerFactories = factories.map { it.value }).run())
                        }

                        emit(Pair(factories, result))
                    }
                }
                .collect { (factories, result) ->
                    result
                        .onSuccess { gameResult ->
                            factories.forEachIndexed { gameIndex, (originalIndex, _) ->
                                val player = gameResult.game.players[gameIndex]
                                playerLogicTime.compute(originalIndex) { _, duration ->
                                    (duration ?: Duration.ZERO) + player.totalPlayerLogicTime
                                }
                            }

                            roundCounts.add(gameResult.game.turnCount)
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
                                    playerChoiceExceptionExamples.putIfAbsent(playerIndex, throwable)
                                    playerChoiceExceptions.add(playerIndex)
                                }

                                is PlayerThrownException -> {
                                    val playerIndex = throwable.player.playerIndex
                                    playerThrownExceptionExamples.putIfAbsent(playerIndex, throwable)
                                    playerThrownExceptions.add(playerIndex)
                                }

                                else -> {
                                    if (engineExceptionExample == null) {
                                        engineExceptionExample = throwable
                                    }
                                    engineExceptions++
                                }
                            }
                        }

                    if (increment != null) {
                        gamesFinished++
                        if (gamesFinished > 0 && gamesFinished < input.games && gamesFinished % increment == 0) {
                            val elapsed = start.elapsedNow()
                            val timeRemaining = (input.games - gamesFinished) * (elapsed / gamesFinished)
                            println("$gamesFinished / ${input.games} [$elapsed, approx $timeRemaining left]")
                        }
                    }
                }
        }

        val elapsed = start.elapsedNow()
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
