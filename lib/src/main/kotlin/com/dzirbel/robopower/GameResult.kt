package com.dzirbel.robopower

/**
 * Wraps the result of a [game].
 */
sealed interface GameResult {
    val game: Game

    /**
     * The case where there was a single [winner], as the player index.
     */
    data class Winner(override val game: Game, val winner: Int) : GameResult

    /**
     * The (rare) case where the game resulted in a tie as a result of counteracts eliminating the remaining players at
     * the same time.
     */
    data class Tied(override val game: Game, val tiedPlayers: Set<Int>) : GameResult
}
