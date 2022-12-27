package com.dzirbel.robopower

import com.dzirbel.robopower.util.indexOfFirstOrNull

/**
 * Composable strategy to play a Trap on turn 2 if it has one.
 * (Skip turn 1 to avoid Counteracts, but play on turn 2 to get a bigger hand before anyone is knocked out)
 */
object EarlyTrapStrategy : OptionalDuelStrategy {
    override fun duel(playerState: PlayerState, involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int? {
        if (playerState.gameState.turnCount == 2) {
            // play any traps on turn 2
            playerState.hand.indexOfFirstOrNull { it.isTrap }?.let { return it }
        }

        return null
    }
}
