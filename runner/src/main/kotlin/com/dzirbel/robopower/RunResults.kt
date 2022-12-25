package com.dzirbel.robopower

import com.dzirbel.robopower.util.MultiSet
import kotlin.time.Duration

data class RunResults(
    val input: RunInput,
    val winCounts: MultiSet<Int>, // counts of wins for each player index
    val winCountByPosition: MultiSet<Int>, // count of wins for each position in the game (going first vs last)
    val tieCounts: MultiSet<Int>, // counts of ties for each player index
    val tieCountByPosition: MultiSet<Int>, // count of ties for each position in the game (going first vs last)
    val totalTies: Int, // total number of ties
    val roundCounts: MultiSet<Int>, // counts of how many rounds each game took
    val playerLogicTime: MutableMap<Int, Duration>, // total time spent within player logic for each player index
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
