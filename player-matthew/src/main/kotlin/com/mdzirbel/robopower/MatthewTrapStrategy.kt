package com.mdzirbel.robopower

import com.dzirbel.robopower.DuelRound
import com.dzirbel.robopower.GameEvent
import com.dzirbel.robopower.OptionalDuelStrategy
import com.dzirbel.robopower.PlayerState
import com.dzirbel.robopower.util.indexOfFirstOrNull

val trapThreshold = 19

object MatthewTrapStrategy : OptionalDuelStrategy {
    override fun duel(playerState: PlayerState, involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int? {
        val trapDesire = trapDesire(playerState)

        if (trapDesire >= trapThreshold) {
            // Play a trap if we have one
            playerState.hand.indexOfFirstOrNull { it.isTrap }?.let { return it }
            // Play a counteract otherwise
            playerState.hand.indexOfFirstOrNull { it.isCounteract }?.let { return it }
        }

        return null
    }

    // what our expected haul from throwing a trap is
    // Looks at last round
    private fun trapDesire(playerState: PlayerState): Double {
        var trapDesire = 0.0

        // num players with one or two cards in their hand
        // todo weight small number of cards players when we know their hand
        // so for bad hands only add weight for the fact that it's a card, good hands raise desire a lot
        for ((i, player) in playerState.gameState.activePlayers) {
            if (player.handSize() == 1) {
                val cardRank = playerState.cardTracker.knownCards[i]?.getOrNull(0).let { it?.rank }
                trapDesire += cardRank ?: 13
            }
            else if (player.handSize() == 2) {
                val cardRank = playerState.cardTracker.knownCards[i]?.getOrNull(0).let { it?.rank }
                trapDesire += cardRank ?: 3
            }
        }

        // Add desire to play trap if we have multiple traps
        trapDesire += (playerState.hand.count { it.isTrap } - 1) * 5

        val lastDuel = playerState.gameState.eventLog.lastOrNull { it is GameEvent.Duel } as? GameEvent.Duel
        // quality of cards thrown in last round. Traps & counteracts aren't counted and it's
        // divided by number of players, so they wind up counting against
        // todo maybe think about how to adjust this for traps?
        val playedCards = lastDuel?.result?.rounds?.first()?.playedCards
        playedCards
            ?.entries
            ?.sumOf { (player, card) ->
                if (player != playerState.playerIndex) { card.scoreRank ?: 0 } else 0
            }
            ?.let { trapDesire += it / (playedCards.size - 1) }

        // we want traps more the further in the game we are
        trapDesire += playerState.gameState.turnCount / (playerState.gameState.players.size * 4)

        return trapDesire
    }
}
