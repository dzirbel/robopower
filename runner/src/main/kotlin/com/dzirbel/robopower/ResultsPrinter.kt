package com.dzirbel.robopower

import com.dzirbel.robopower.util.MultiSet

object ResultsPrinter {
    fun printResults(results: RunResults) {
        println(
            "${results.successfulGames} / ${results.input.games} games completed successfully " +
                "(${formatPercent(results.successfulGames, results.input.games)})",
        )

        printEngineExceptions(results)
        println()
        printPlayerWinrates(results)
        println()
        printWinrateByPosition(results)
        println()
        printRoundInfo(roundCounts = results.roundCounts, verbose = results.input.verbose)
    }

    private fun printEngineExceptions(results: RunResults) {
        if (results.input.verbose || results.engineExceptions > 0) {
            println("${results.engineExceptions} engine exceptions")
            results.engineExceptionExample?.let { example ->
                println()
                println("Example:")
                println(example.stackTraceToString())
                println()
            }
        }
    }

    private fun printPlayerWinrates(results: RunResults) {
        println("Results:")
        results.input.players.forEachIndexed { playerIndex, player ->
            val wins = results.winCounts.count(playerIndex)
            val ties = results.tieCounts.count(playerIndex)
            val playerName = player.playerName(playerIndex)
            println(
                "  Player ${playerIndex + 1} ($playerName) : $wins wins (${results.percentOfGames(wins)}); " +
                    "$ties ties (${results.percentOfGames(ties)})",
            )

            val timePerGame = results.playerLogicTime.getValue(playerIndex) / results.successfulGames
            println("  > Time spent per game: $timePerGame")

            val invalidChoices = results.playerChoiceExceptions.count(playerIndex)
            if (results.input.verbose || invalidChoices > 0) {
                println("  > Invalid choices:   $invalidChoices")
                results.playerChoiceExceptionExamples[playerIndex]?.let { example ->
                    println()
                    println("    Example:")
                    println(example.stackTraceToString())
                    println()
                }
            }

            val exceptionsThrown = results.playerThrownExceptions.count(playerIndex)
            if (results.input.verbose || exceptionsThrown > 0) {
                println("  > Exceptions thrown: $exceptionsThrown")
                results.playerThrownExceptionExamples[playerIndex]?.let { example ->
                    println()
                    println("    Example:")
                    println(example.stackTraceToString())
                }
            }

            println()
        }
        println("Total ties : ${results.totalTies} (${results.percentOfGames(results.totalTies)})")
    }

    private fun printWinrateByPosition(results: RunResults) {
        println("Winrate by position in game:")
        repeat(results.input.players.size) { position ->
            val wins = results.winCountByPosition.count(position)
            val ties = results.tieCountByPosition.count(position)
            println(
                "  #${position + 1} : $wins wins (${formatPercent(wins, results.successfulGames)}); " +
                    "$ties ties (${formatPercent(ties, results.successfulGames)})",
            )
        }
    }

    @Suppress("MagicNumber")
    private fun printRoundInfo(roundCounts: MultiSet<Int>, verbose: Boolean) {
        val roundMap = roundCounts.map
        val min = roundMap.keys.min()
        val max = roundMap.keys.max()
        val avg = roundMap.entries.sumOf { it.key * it.value }.toDouble() / roundCounts.total

        println("Rounds per game: average ${avg.roundTo(places = 2)}; min = $min; max = $max")
        println("  P01: ${roundMap.percentile(1)}")
        println("  P25: ${roundMap.percentile(25)}")
        println("  P50: ${roundMap.percentile(50)}")
        println("  P75: ${roundMap.percentile(75)}")
        println("  P99: ${roundMap.percentile(99)}")

        if (verbose) {
            val maxChars = max.toString().length
            for (roundCount in min..max) {
                println("  ${roundCount.toString().padStart(maxChars)} : ${roundCounts.count(roundCount)}")
            }
        }
    }

    private fun Map<Int, Int>.percentile(percentile: Int): Int {
        require(percentile in 0..100)

        val total = entries.sumOf { it.value }
        var progress = 0
        for ((round, count) in this.entries.sortedBy { it.key }) {
            progress += count
            if (progress >= total * percentile / 100) {
                return round
            }
        }
        error("did not find percentile $percentile")
    }
}
