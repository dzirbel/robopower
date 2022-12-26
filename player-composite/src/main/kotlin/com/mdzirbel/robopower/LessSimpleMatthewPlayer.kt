package com.mdzirbel.robopower

import com.dzirbel.robopower.Card
import com.dzirbel.robopower.DuelResult
import com.dzirbel.robopower.DuelRound
import com.dzirbel.robopower.Game
import com.dzirbel.robopower.GameEvent
import com.dzirbel.robopower.onEventOfType
import com.dzirbel.robopower.util.indexOfFirstOrNull
import com.dzirbel.robopower.util.maxByNullableOrNull
import kotlin.random.Random

// config
val trapThreshold = 19

@Suppress("UnsafeCallOnNullableType", "ReturnCount", "UnnecessaryLet")
class LessSimpleMatthewPlayer(
    playerIndex: Int,
    game: Game,
    random: Random = Random.Default,
) : SimplePlayer(playerIndex, game, random) {


    private var lastDuel: GameEvent.Duel? = null

    init {
        // on duel, update known cards with all the results
        game.onEventOfType<GameEvent.Duel> { event ->
            lastDuel = event
        }
    }

    // TODO add to DuelResult
    private val DuelResult.allCards: Map<Int, List<Card>>
        get() = rounds.fold(mutableMapOf()) { acc, round ->
            for ((playerIndex, card) in round.playedCards) {
                acc.compute(playerIndex) { _, cards -> cards.orEmpty().plus(card) }
            }
            acc
        }

    override fun discard(): Int {
        val spyIndex = hand.indexOfFirstOrNull { it.spyCount > 0 }
        if (spyIndex != null) {
            val spies = hand.count { it.spyCount > 0 }
            if (hand.size - spies == 1) return spyIndex

            cardTracker.knownCards.mapValues { (playerIndex, knownCards) ->
                val handSize = game.players[playerIndex].handSize()
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

        val lastDuel = game.eventLog.lastOrNull { it is GameEvent.Duel } as? GameEvent.Duel
        val highestRankLastDuel: Int? = lastDuel?.result?.allCards?.entries
            ?.maxByNullableOrNull { it.value.maxByOrNull { card -> card.rank } }
            ?.value
            ?.maxByOrNull { card -> card.rank }
            ?.rank

        // Added code to play traps specifically
        val trapDesire = trapDesire()
        if (trapDesire >= trapThreshold) {
            // Play a trap if we have one
            hand.indexOfFirstOrNull { it.isTrap }?.let { return it }
            // Play a counteract otherwise
            hand.indexOfFirstOrNull { it.isCounteract }?.let { return it }
        }

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

    // what our expected haul from throwing a trap is
    // Looks at last round
    private fun trapDesire(
    ): Double {
        var trapDesire = 0.0
        // num players with one or two cards in their hand
        // todo weight small number of cards players when we know their hand
        // so for bad hands only add weight for the fact that it's a card, good hands raise desire a lot
        for ((i, player) in game.activePlayers) {
            if (player.handSize() == 1) {
                val cardRank = cardTracker.knownCards[i]?.getOrNull(0).let { it?.rank }
                trapDesire += cardRank ?: 13
            }
            else if (player.handSize() == 2) {
                val cardRank = cardTracker.knownCards[i]?.getOrNull(0).let { it?.rank }
                trapDesire += cardRank ?: 3
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

        // we want traps more the further in the game we are
        trapDesire += game.turnCount / (game.players.size * 4)

        return trapDesire
    }

    companion object : Factory {
        override fun create(playerIndex: Int, game: Game) = LessSimpleMatthewPlayer(playerIndex, game)
    }
}
