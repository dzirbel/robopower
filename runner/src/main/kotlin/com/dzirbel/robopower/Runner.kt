package com.dzirbel.robopower

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.times

// TODO calculate relative advantage for each position when randomizing order
object Runner {
    @OptIn(FlowPreview::class)
    data class RunInput(
        val games: Int,
        val players: List<Player.Factory>,
        val randomizeOrder: Boolean = true,
        val concurrency: Int = DEFAULT_CONCURRENCY,
    )

    data class Results(
        val input: RunInput,
        val winCounts: MultiSet<Int>, // counts of wins for each player index
        val tieCounts: MultiSet<Int>, // counts of ties for each player index
        val totalTies: Int, // total number of ties
        val playerChoiceExceptions: MultiSet<Int>, // number of times each player index threw a choice exception
        val playerChoiceExceptionExamples: MutableMap<Int, Throwable>, // single example of choice exceptions
        val playerThrownExceptions: MultiSet<Int>, // number of times each player index threw an exception
        val playerThrownExceptionExamples: MutableMap<Int, Throwable>, // single example of player exception
        val engineExceptions: Int, // number of times the engine threw an exception
        val engineExceptionExample: Throwable?, // example of enging exception
    ) {
        val successfulGames
            get() = winCounts.total + totalTies
    }

    @OptIn(FlowPreview::class, ExperimentalTime::class)
    suspend fun run(input: RunInput, printIncrementPercent: Int? = 5): Results {
        val winCounts = MultiSet<Int>()
        val tieCounts = MultiSet<Int>()
        var totalTies = 0

        val playerChoiceExceptions = MultiSet<Int>()
        val playerChoiceExceptionExamples = mutableMapOf<Int, Throwable>()
        val playerThrownExceptions = MultiSet<Int>()
        val playerThrownExceptionExamples = mutableMapOf<Int, Throwable>()
        var engineExceptions = 0
        var engineExceptionExample: Throwable? = null

        val increment = printIncrementPercent?.let { (input.games * it) / 100 }
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
                            when (gameResult) {
                                is GameResult.Winner -> winCounts.add(factories[gameResult.winner].index)
                                is GameResult.Tied -> {
                                    totalTies++
                                    tieCounts.addAll(gameResult.tiedPlayers.map { factories[it].index })
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
        println("Done in $elapsed (avg ${elapsed / input.games} per game)")
        println()

        return Results(
            input = input,
            winCounts = winCounts,
            tieCounts = tieCounts,
            totalTies = totalTies,
            playerChoiceExceptions = playerChoiceExceptions,
            playerChoiceExceptionExamples = playerChoiceExceptionExamples,
            playerThrownExceptions = playerThrownExceptions,
            playerThrownExceptionExamples = playerThrownExceptionExamples,
            engineExceptions = engineExceptions,
            engineExceptionExample = engineExceptionExample,
        )
    }

    // TODO extract
    fun printResults(results: Results) {
        val successfulGames = results.successfulGames

        println(
            "$successfulGames / ${results.input.games} games completed successfully " +
                    "(${formatPercent(successfulGames, results.input.games)})"
        )
        println("${results.engineExceptions} engine exceptions")
        results.engineExceptionExample?.let { example ->
            println()
            println("Example:")
            println(example.stackTraceToString())
            println()
        }
        println()

        println("Results:")
        results.input.players.forEachIndexed { playerIndex, player ->
            val wins = results.winCounts.count(playerIndex)
            val ties = results.tieCounts.count(playerIndex)
            val playerName = player.playerName(playerIndex)
            println(
                "  Player ${playerIndex + 1} ($playerName) : $wins wins (${formatPercent(wins, successfulGames)}); " +
                        "$ties ties (${formatPercent(ties, successfulGames)})",
            )

            println("  > Invalid choices:   ${results.playerChoiceExceptions.count(playerIndex)}")
            results.playerChoiceExceptionExamples[playerIndex]?.let { example ->
                println()
                println("    Example:")
                println(example.stackTraceToString())
                println()
            }

            println("  > Exceptions thrown: ${results.playerThrownExceptions.count(playerIndex)}")
            results.playerThrownExceptionExamples[playerIndex]?.let { example ->
                println()
                println("    Example:")
                println(example.stackTraceToString())
            }

            println()
        }
        println("Total ties : ${results.totalTies} (${formatPercent(results.totalTies, successfulGames)})")
    }

    private fun formatPercent(numerator: Int, denominator: Int): String {
        return String.format("%.2f%%", 100 * (numerator.toDouble() / denominator))
    }
}
