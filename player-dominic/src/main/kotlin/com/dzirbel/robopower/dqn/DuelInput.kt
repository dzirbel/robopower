package com.dzirbel.robopower.dqn

import com.dzirbel.robopower.Card
import com.dzirbel.robopower.DuelRound
import com.dzirbel.robopower.Game
import com.dzirbel.robopower.GameEvent
import com.dzirbel.robopower.PlayerState
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input

private const val NUM_DUELS_INCLUDED_IN_INPUT = 5
val DUEL_BUFFER_SIZE = Card.values().size.let { cards ->
    6 + cards + (Game.MAX_PLAYERS - 1) * (cards + 1) + cards + NUM_DUELS_INCLUDED_IN_INPUT * cards
}

val DuelInput = Input(DUEL_BUFFER_SIZE.toLong(), name = "duel input")

fun PlayerState.toDuelInput(playedCard: Card, involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): FloatArray {
    val cards = Card.values()
    return buildFloatArray(DUEL_BUFFER_SIZE) {
        // card played in the duel; note that this is not part of the game state but corresponds to the "action"
        // dimension in Q-learning
        add(playedCard.ordinal)

        // number of active players
        add(gameState.activePlayerCount)

        // number of players involved in this duel round (an approximation of the player indexes themselves)
        add(involvedPlayers.size)

        // number of turns until this player is up
        add(roundsUntilUp)

        // sizes of draw and discard piles (an approximation of the cards in the discard pile)
        add(gameState.deck.drawPileSize)
        add(gameState.deck.discardPileSize)

        // cards in the player's hand, as a mapping from the card ordinal to the number of that card in the hand
        for (card in cards) {
            add(hand.count { it == card })
        }

        // known cards in other players hands, repeated for all other players in the game (buffered with zeros up to the
        // maximum number in the game), again as a mapping from card ordinal to number in the hand
        repeat(Game.MAX_PLAYERS - 1) { playerIndex ->
            add(gameState.players.getOrNull(playerIndex)?.handSize() ?: 0)

            val knownCards = cardTracker.knownCards[playerIndex]
            if (knownCards == null) {
                repeat(cards.size) { add(0) }
            } else {
                for (card in cards) {
                    add(knownCards.count { it == card })
                }
            }
        }

        // cards currently involved in this duel; an approximation of the duel state
        for (card in cards) {
            // TODO not correct, does not remove cards which have already been retained
            val count = previousRounds.sumOf { round -> round.playedCards.count { it.value == card } }
            add(count)
        }

        // cards played in the last N duels
        val duels = gameState.eventLog
            .filterIsInstance<GameEvent.Duel>()
            .reversed()
            .take(NUM_DUELS_INCLUDED_IN_INPUT)

        duels.forEach { duel ->
            for (card in cards) {
                val count = duel.result.rounds.sumOf { round -> round.playedCards.count { it.value == card } }
                add(count)
            }
        }

        // pad for missing past duels (in the first few rounds)
        if (duels.size < NUM_DUELS_INCLUDED_IN_INPUT) {
            repeat(NUM_DUELS_INCLUDED_IN_INPUT - duels.size) {
                repeat(cards.size) { add(0) }
            }
        }
    }
}
