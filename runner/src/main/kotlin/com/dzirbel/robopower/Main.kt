package com.dzirbel.robopower

import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

const val totalGames = 100_000
const val increment = 5_000

// TODO allow randomizing player order in each game
val players: List<Player.Factory> = listOf(
    SimplePlayer,
    RandomPlayer,
    SimplePlayer,
    RandomPlayer,
)

// TODO extract logic to a runner class/object
@OptIn(ExperimentalTime::class)
fun main() {
    val winCounts = MultiSet<Int>()
    val tieCounts = MultiSet<Int>()
    var totalTies = 0

    val playerChoiceExceptions = MultiSet<Int>()
    val playerChoiceExceptionExamples = mutableMapOf<Int, Throwable>()
    val playerThrownExceptions = MultiSet<Int>()
    val playerThrownExceptionExamples = mutableMapOf<Int, Throwable>()
    var engineExceptions = 0
    var engineExceptionExample: Throwable? = null

    val start = TimeSource.Monotonic.markNow()

    repeat(totalGames) { round ->
        val gameResult = try {
            GameImpl(playerFactories = players).run()
        } catch (ex: PlayerChoiceException) {
            val playerIndex = ex.player.playerIndex
            playerChoiceExceptionExamples.putIfAbsent(playerIndex, ex)
            playerChoiceExceptions.add(playerIndex)
            null
        } catch(ex: PlayerThrownException) {
            val playerIndex = ex.player.playerIndex
            playerThrownExceptionExamples.putIfAbsent(playerIndex, ex)
            playerThrownExceptions.add(playerIndex)
        } catch (ex: Throwable) {
            if (engineExceptionExample == null) {
                engineExceptionExample = ex
            }
            engineExceptions++
            null
        }

        when (gameResult) {
            is GameResult.Winner -> winCounts.add(gameResult.winner)
            is GameResult.Tied -> {
                totalTies++
                tieCounts.addAll(gameResult.tiedPlayers)
            }

            null -> {}
        }

        if (round > 0 && round % increment == 0) {
            println("$round / $totalGames [${start.elapsedNow()}]")
        }
    }

    val elapsed = start.elapsedNow()

    println()
    println("Ran $totalGames games in $elapsed (${elapsed / totalGames} per game)")
    println("$engineExceptions exceptions from the engine")
    engineExceptionExample?.let { example ->
        println()
        println("Example:")
        println(example.stackTraceToString())
        println()
    }
    println()

    println("Results:")
    repeat(players.size) { playerIndex ->
        val wins = winCounts.count(playerIndex)
        val ties = tieCounts.count(playerIndex)
        println("  Player ${playerIndex + 1} : $wins wins (${formatPercent(wins, totalGames)}); $ties ties (${formatPercent(ties, totalGames)})")

        println("  > Invalid choices:   ${playerChoiceExceptions.count(playerIndex)}")
        playerChoiceExceptionExamples[playerIndex]?.let { example ->
            println()
            println("    Example:")
            println(example.stackTraceToString())
            println()
        }

        println("  > Exceptions thrown: ${playerThrownExceptions.count(playerIndex)}")
        playerThrownExceptionExamples[playerIndex]?.let { example ->
            println()
            println("    Example:")
            println(example.stackTraceToString())
        }

        println()
    }
    println("Total ties : $totalTies (${formatPercent(totalTies, totalGames)})")
}

private fun formatPercent(wins: Int, total: Int): String {
    return String.format("%.2f", 100 * (wins.toDouble() / total)) + "%"
}
