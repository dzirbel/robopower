package com.dzirbel.robopower

/**
 * Wraps the result of a [game].
 *
 * [playersToPlace] maps player index to their place in a game, e.g. 1 for first, 2 for second, etc. Ties are considered
 * to be the same as an exclusive place, e.g. if players tied for first they will both have place 1.
 */
class GameResult internal constructor(
    val game: Game,
    val playersToPlace: Map<Int, Int>,
) {
    init {
        // player indexes are correct
        assertLazy { playersToPlace.keys == game.gameState.players.mapTo(mutableSetOf()) { it.playerIndex } }
        // at least one player tied for first
        assertLazy { playersToPlace.values.any { it == 1 } }
    }

    /**
     * Whether there was a unique winner of the game.
     */
    val decisive: Boolean by lazy { playersToPlace.count { it.value == 1 } == 1 }

    /**
     * The winning player indexes; may be a single player when [decisive] is true or multiple otherwise.
     */
    val winners: Set<Int> by lazy { playersToPlace.filter { it.value == 1 }.keys }

    /**
     * The player index of the unique winner of the game, or null if the game was tied.
     */
    val winner: Int? by lazy { winners.takeIf { it.size == 1 }?.first() }

    /**
     * The player indexes of the players who tied for first, or null if there was a unique winner.
     */
    val tiedPlayers: Set<Int>? by lazy { winners.takeIf { it.size > 1 } }
}
