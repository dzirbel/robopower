package com.dzirbel.robopower

import com.dzirbel.robopower.util.indexOfFirstOrNull
import com.dzirbel.robopower.util.indexOfMinOrNull
import com.dzirbel.robopower.util.maxByNullableOrNull
import com.dzirbel.robopower.util.minByNullableOrNull
import kotlin.random.Random

/**
 * Player originally written as psuedo-code by Alex.
 */
class PseudoCodePseudoPlayer(
    playerState: PlayerState,
    private val random: Random = Random.Default,
) : Player(playerState) {
    override fun discard(): Int {
        // if (has SpyMaster && another player has <= 2 cards): play SpyMaster
        // if (has SpyMaster && we have <= 4 cards): play SpyMaster
        if (hand.size <= 4 || gameState.players.any { it.handSize() <= 2 }) {
            hand.indexOfFirstOrNull { it == Card.SPY_MASTER }?.let { return it }
        }

        // if (has Spy && another player has 1 card): play Spy
        // if (has Spy && we have <= 3 cards): play Spy
        if (hand.size <= 3 || gameState.players.any { it.handSize() <= 3 }) {
            hand.indexOfFirstOrNull { it == Card.SPY }?.let { return it }
        }

        // discard lowest
        hand.indexOfMinOrNull { if (it.spyCount == 0) it.score else null }?.let { return it }

        // discard counteracts
        hand.indexOfMinOrNull { it.isCounteract }?.let { return it }

        // discard first trap
        hand.indexOfMinOrNull { it.isTrap }?.let { return it }

        // only spies; discard the first card
        return 0
    }

    override fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int {
        val trapIndex = hand.indexOfFirstOrNull { it.isTrap }
        val highest = hand.withIndex().maxByNullableOrNull { if (it.value.spyCount == 0) it.value.score else null }

        // if (double duel): play traps -> high card -> counteracts
        if (previousRounds.isNotEmpty()) {
            trapIndex?.let { return it }
            highest?.let { return it.index }
            hand.indexOfFirstOrNull { it.isCounteract }?.let { return it }
        }

        val lastDuel = gameState.eventLog.lastOrNull { it is GameEvent.Duel } as? GameEvent.Duel

        if (trapIndex != null) {
            // if (has Trap && another player has 1 card): play Trap
            if (gameState.activePlayers.any { it.value.handSize(includeCardsInPlay = true) == 1 }) {
                return trapIndex
            }

            // if (no Trap/Counteract played last round, and highest card last round > my highest): play Trap
            val maxScoreLastRound: Int? = lastDuel?.result?.allCards
                ?.values
                ?.flatten()
                ?.maxByNullableOrNull { it.score }
                ?.score
            val maxScoreInHand: Int? = hand.maxByNullableOrNull { it.score }?.score
            if (lastDuel != null &&
                lastDuel.result.allCards.none { it.value.any { card -> card.isTrap || card.isCounteract } } &&
                (maxScoreLastRound != null && maxScoreInHand != null && maxScoreLastRound > maxScoreInHand)
            ) {
                return trapIndex
            }
        }

        // if (has Counteract): play Counteract 50% of the time
        val counteractIndex = hand.indexOfFirstOrNull { it.isCounteract }
        if (counteractIndex != null && random.nextBoolean()) {
            return counteractIndex
        }

        val minScoreLastRound: Int? = lastDuel?.result?.allCards
            ?.values
            ?.flatten()
            ?.minByNullableOrNull { it.score }
            ?.score
        val secondHighest = hand.withIndex()
            .filter { it.index != highest?.index }
            .maxByNullableOrNull { it.value.score }

        // if (my 2nd highest card > lowest card played last duel): play 2nd highest
        if (minScoreLastRound != null && secondHighest != null && secondHighest.value.score!! > minScoreLastRound) {
            return secondHighest.index
        }

        // play highest
        highest?.let { return it.index }

        // play remaining counteract/trap
        trapIndex?.let { return it }
        counteractIndex?.let { return it }

        return 0 // only spies
    }

    override fun spy(): Int {
        // spy from player with the fewest cards
        return gameState.activePlayers.filter { it.index != playerIndex }.minByOrNull { it.value.handSize() }?.index
            ?: error("could not find player to spy")
    }

    companion object : Factory {
        override fun create(playerState: PlayerState) = PseudoCodePseudoPlayer(playerState)
    }
}
