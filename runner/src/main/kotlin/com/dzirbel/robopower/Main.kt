package com.dzirbel.robopower

import com.mdzirbel.robopower.LessSimpleMatthewPlayer
import com.mdzirbel.robopower.MatthewPlayer
import com.mdzirbel.robopower.MatthewTrapStrategy
import kotlinx.coroutines.runBlocking

// constants specifying the game to be run; try to avoid committing changes to reduce merge conflicts
val input = RunInput(
    games = 100_000,
    players = listOf(
        SimplePlayer,
        LessSimplePlayer,
        LessSimpleMatthewPlayer,
        BoomChickaBoomPlayer,
        MatthewPlayer,
        CompositePlayer.fromPlayers(
            discarder = SimplePlayer,
            spier = SimplePlayer,
            dueler = MatthewPlayer,
        ),
        SimplePlayer.withStrategies(
            duelStrategy = MatthewTrapStrategy,
        ),
    ),
)

fun main() {
    val results = runBlocking { Runner.run(input) }
    ResultsPrinter.printResults(results)
}
