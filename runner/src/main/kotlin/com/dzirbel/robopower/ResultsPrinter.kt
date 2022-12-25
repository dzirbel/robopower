package com.dzirbel.robopower

object ResultsPrinter {
    fun printResults(results: RunResults) {
        val successfulGames = results.successfulGames

        println(
            "$successfulGames / ${results.input.games} games completed successfully " +
                "(${formatPercent(successfulGames, results.input.games)})",
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
        println("Total ties : ${results.totalTies} (${formatPercent(results.totalTies, successfulGames)})")

        println()
        println("Winrate by position in game:")
        repeat(results.input.players.size) { position ->
            val wins = results.winCountByPosition.count(position)
            val ties = results.tieCountByPosition.count(position)
            println(
                "  #${position + 1} : $wins wins (${formatPercent(wins, successfulGames)}); " +
                    "$ties ties (${formatPercent(ties, successfulGames)})",
            )
        }

        println()
        val minRounds = results.roundCounts.elements.min()
        val maxRounds = results.roundCounts.elements.max()
        val maxRoundChars = maxRounds.toString().length
        println("Rounds per game (avg: ${results.roundCounts.elements.average().roundTo(places = 2)}):")
        for (roundCount in minRounds..maxRounds) {
            println("  ${roundCount.toString().padStart(maxRoundChars)} : ${results.roundCounts.count(roundCount)}")
        }
    }
}
