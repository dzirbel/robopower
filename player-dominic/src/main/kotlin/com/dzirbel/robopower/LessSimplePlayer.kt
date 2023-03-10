package com.dzirbel.robopower

import com.dzirbel.robopower.util.indexOfFirstOrNull
import com.dzirbel.robopower.util.maxByNullableOrNull
import kotlin.random.Random

/**
 * Slight improvement over [SimplePlayer] which uses hardcoded heuristics of card score to determine its play.
 */
@Suppress("UnsafeCallOnNullableType", "ReturnCount", "UnnecessaryLet")
class LessSimplePlayer(playerState: PlayerState, random: Random = Random.Default) : SimplePlayer(playerState, random) {
    override fun discard(): Int {
        val spyIndex = hand.indexOfFirstOrNull { it.spyCount > 0 }
        if (spyIndex != null) {
            val spies = hand.count { it.spyCount > 0 }
            if (hand.size - spies == 1) return spyIndex

            cardTracker.knownCards.mapValues { (playerIndex, knownCards) ->
                val handSize = gameState.players[playerIndex].handSize()
                if (knownCards.size == handSize) knownCards.minByOrNull { it.rank } else null
            }
                .filterValues { it != null }
                .maxByOrNull { it.value!!.rank }
                ?.takeIf { it.value!!.rank >= Card.RAM.rank }
                ?.let { return spyIndex }
        }

        return super.discard()
    }

    override fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int {
        // play traps in double duels
        if (previousRounds.isNotEmpty()) {
            hand.indexOfFirstOrNull { it.isTrap }?.let { return it }
        }

        val highestScoreKnown = cardTracker.knownCards
            .flatMap { it.value }
            .maxByNullableOrNull { it.score }
            ?.score
            ?: Card.ALX.score!!

        val lastDuel = gameState.eventLog.lastOrNull { it is GameEvent.Duel } as? GameEvent.Duel
        val highestRankLastDuel: Int? = lastDuel?.result?.allCards?.entries
            ?.maxByNullableOrNull { it.value.maxByOrNull { card -> card.rank } }
            ?.value
            ?.maxByOrNull { card -> card.rank }
            ?.rank

        if (highestScoreKnown > Card.UN_BEAT.score!! &&
            highestRankLastDuel != null &&
            highestRankLastDuel >= Card.BRAINY.rank
        ) {
            hand.indexOfFirstOrNull { it.isTrap }?.let { return it }
        }

        val normalCards = hand.withIndex().filter { it.value.isNormal }
        val sorted = normalCards.sortedBy { it.value.score!! }
        sorted.firstOrNull { it.value.score!! >= Card.RAM.score!! }?.let { return it.index }

        return super.duel(involvedPlayers, previousRounds)
    }

    companion object : Factory {
        override fun create(playerState: PlayerState) = LessSimplePlayer(playerState)
    }
}
