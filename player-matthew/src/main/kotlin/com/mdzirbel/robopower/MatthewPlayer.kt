package com.mdzirbel.robopower

import com.dzirbel.robopower.*
import com.dzirbel.robopower.util.indexOfFirstOrNull
import com.dzirbel.robopower.util.indexOfMaxOrNull
import com.dzirbel.robopower.util.indexOfMinOrNull
import com.dzirbel.robopower.util.maxByNullableOrNull
import com.dzirbel.robopower.util.maxKeyByOrNull
import kotlin.random.Random

// config
val spyDesireThreshold = 10
val trapThreshold = 23

class PlayerKnowledge(
) {
    val highCard = 0
    val lowCard = 0

    val handQuality = 0

    // How much we want to steal from them
    fun spyDesire() {

    }
}

/**
 * The best player with the best strategies. Beats Dominic's strategies by a lot
 */
class MatthewPlayer(
    playerIndex: Int,
    game: Game,
    private val random: Random = Random.Default,
) : PlayerWithCardTracker(playerIndex, game) {

    private var lastDuel: GameEvent.Duel? = null

    init {
        // on duel, update known cards with all the results
        game.onEventOfType<GameEvent.Duel> { event ->
            lastDuel = event
        }
    }

    override fun discard(): Int {
        return hand.indexOfMinOrNull { it.score } // discard the smallest score (ignoring traps and counteracts)
            ?: hand.indexOfFirstOrNull { it.isCounteract } // discard any counteracts
            ?: 0 // there are only traps so discard the first one
    }

    // todo account for double duel things
    override fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int {

        // if we have 2 cards, play a counteract if we have one
        if (hand.size == 2) {
            hand.indexOfFirstOrNull { it.isCounteract }?.let { return it }
        }

        // Check what our desire to play a trap is
        // todo don't check trap desire unless I have a trap or counteract
        val trapDesire = trapDesire()
        if (trapDesire >= trapThreshold) {
            // Play a trap if we have one
            hand.indexOfFirstOrNull { it.isTrap }?.let { return it }
            // Play a counteract otherwise
            hand.indexOfFirstOrNull { it.isCounteract }?.let { return it }
        }

        // Play a normal card if we have one
        return hand.indexOfMaxOrNull { if (it.isNormal) it.score else null } // play the highest normal card
            ?: hand.indexOfFirstOrNull { it.isCounteract } // play any counteracts
            ?: hand.indexOfFirstOrNull { it.isTrap } // play any traps
            ?: 0 // only spies, play the first one

        // If we only have traps, counteracts, spies: play in order counteract, trap, spy, spymaster

    }

    override fun spy(): Int {
        // spy from the player with the highest known card
        return cardTracker.knownCards.maxKeyByOrNull { cards -> cards.maxByNullableOrNull { it.score } }
            ?: game.activePlayers.filter { it.index != playerIndex }.random(random).index // spy a random player
    }


    // what our expected haul from throwing a trap is
    // Looks at last round
    private fun trapDesire(
    ): Double {
        var trapDesire = 0.0
        // num players with one or two cards in their hand
        // todo weight small number of cards players when we know their hand
        // so for bad hands only add weight for the fact that it's a card, good hands raise desire a lot
        for ((_, player) in game.activePlayers) {
            if (player.handSize() == 1) {
                trapDesire += 13
            }
            else if (player.handSize() == 2) {
                trapDesire += 3
            }
        }

        // Add desire to play trap if we have multiple traps
        trapDesire += (hand.count { it.isTrap } - 1) * 5

        // quality of cards thrown in last round. Traps & counteracts aren't counted and it's
        // divided by number of players, so they wind up counting against
        // todo maybe think about how to adjust this for traps?
        val playedCards = lastDuel?.result?.rounds?.first()?.playedCards
        playedCards
            ?.entries
            ?.sumOf { (player, card) ->
                if (player != playerIndex) { card.scoreRank ?: 0 } else 0
            }
            ?.let { trapDesire += it / (playedCards.size - 1) }

        // todo round counter (maybe divide by # players?)

        return trapDesire
    }

    // What our expected value for spying is and the best player to do so from
    private fun spyDesire() {

    }

    companion object : Factory {
        override fun create(playerIndex: Int, game: Game) = MatthewPlayer(playerIndex, game)
    }
}
