package com.dzirbel.robopower

import com.dzirbel.robopower.util.indexOfFirstOrNull
import com.dzirbel.robopower.util.indexOfMaxOrNull
import com.dzirbel.robopower.util.indexOfMinOrNull
import com.dzirbel.robopower.util.maxByNullableOrNull
import com.dzirbel.robopower.util.maxKeyByOrNull
import kotlin.random.Random

/**
 */
open class PseudoCodePseudoPlayer(
    playerIndex: Int,
    game: Game,
    private val random: Random = Random.Default,
) : PlayerWithCardTracker(playerIndex, game) {
    private val DuelResult.allCards: Map<Int, List<Card>>
        get() = rounds.fold(mutableMapOf()) { acc, round ->
            for ((playerIndex, card) in round.playedCards) {
                acc.compute(playerIndex) { _, cards -> cards.orEmpty().plus(card) }
            }
            acc
        }
    
    override fun discard(): Int {
        // if (has SpyMaster && another player has <= 2 cards): play SpyMaster
        // if (has SpyMaster && we have <= 4 cards): play SpyMaster
        // if (has Spy && another player has 1 card): play Spy
        // if (has Spy && we have <= 3 cards): play Spy
        // discard lowest
        // discard counteracts
        // discard first trap
    }

    override fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int {
        // if (has Trap && another player has 1 card): play Trap
        // if (no Trap/Counteract played last round, and highest card last round > my highest): play Trap
        // if (has Counteract): play Counteract 50% of the time
        // if (my 2nd highest card > lowest card played last duel): play 2nd highest
        // play highest
        // play remaining counteract/trap
    }

    override fun spy(): Int {
        // spy from player with the fewest cards
    }

    fun handMaxStrength(): Int {
        return hand.map{card: Card -> card.score}.filterNotNull().max()
    }

    fun hasTrap(): Boolean {
        return hand.filter { it.isTrap }.isNotEmpty()
    }

    fun hasCounteract(): Boolean {
        return hand.filter { it.isCounteract }.isNotEmpty()
    }

    companion object : Factory {
        override fun create(playerIndex: Int, game: Game) = PseudoCodePseudoPlayer(playerIndex, game)
    }
}
