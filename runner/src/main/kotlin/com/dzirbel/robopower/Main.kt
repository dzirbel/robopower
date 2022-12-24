package com.dzirbel.robopower

import kotlinx.coroutines.runBlocking

val input = Runner.RunInput(
    games = 1_000_000,
    players = listOf(
        SimplePlayer,
        RandomPlayer,
        SimplePlayer,
        RandomPlayer,
    ),
)

fun main() {
    val results = runBlocking { Runner.run(input) }
    Runner.printResults(results)
}
