package com.dzirbel.robopower

import com.dzirbel.robopower.util.indexOfFirstOrNull
import com.dzirbel.robopower.util.indexOfMaxOrNull
import com.dzirbel.robopower.util.indexOfMinOrNull
import com.dzirbel.robopower.util.maxByNullableOrNull
import com.dzirbel.robopower.util.maxKeyByOrNull
import kotlin.random.Random

/**
 * Simple [Player] with very basic, hard-coded strategies. Beats [RandomPlayer] about 4:1.
 */
open class SimplePlayer(playerState: PlayerState, private val random: Random = Random.Default) : Player(playerState) {
    override fun discard(): Int {
        return hand.indexOfMinOrNull { it.score } // discard the smallest score (ignoring traps and counteracts)
            ?: hand.indexOfFirstOrNull { it.isCounteract } // discard any counteracts
            ?: 0 // there are only traps so discard the first one
    }

    override fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int {
        return hand.indexOfFirstOrNull { it.isCounteract } // play any counteracts
            ?: hand.indexOfMaxOrNull { if (it.isNormal) it.score else null } // play the highest normal card
            ?: hand.indexOfFirstOrNull { it.isTrap } // play any traps
            ?: 0 // only spies, play the first one
    }

    override fun spy(): Int {
        // spy from the player with the highest known card
        return cardTracker.knownCards.maxKeyByOrNull { cards -> cards.maxByNullableOrNull { it.score } }
            ?: gameState.activePlayers.filter { it.index != playerIndex }.random(random).index // spy a random player
    }

    companion object : Factory {
        override fun create(playerState: PlayerState) = SimplePlayer(playerState)
    }
}
