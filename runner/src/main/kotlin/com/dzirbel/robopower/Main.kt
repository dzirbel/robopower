package com.dzirbel.robopower

import kotlinx.coroutines.runBlocking

// constants specifying the game to be run; try to avoid committing changes to reduce merge conflicts
val input = RunInput(
    games = 100_000,
    players = listOf(
        SimplePlayer,
        SimplePlayer,
        RandomPlayer,
        RandomPlayer,
    ),
)

fun main() {
    val results = runBlocking { Runner.run(input) }
    ResultsPrinter.printResults(results)
}
