package com.dzirbel.robopower

/**
 * Contains the full set of information for what happened in a duel (including any double-duels).
 */
data class DuelResult(
    /**
     * The individual [DuelRound]s of the duel; in each round all involved players play a card and a [DuelRoundResult]
     * will be calculated.
     */
    val rounds: List<DuelRound>,

    /**
     * Map from player index to the cards they played which will ultimately be discarded (the lowest card(s) from losing
     * a duel, exhausted Traps, and counteracted cards).
     */
    val discardedCards: Map<Int, List<Card>> = emptyMap(),

    /**
     * Map from player index to the cards they played which will ultimately be returned to their hand (generally cards
     * other than the lowest card).
     */
    val retainedCards: Map<Int, List<Card>> = emptyMap(),

    /**
     * Map from player index of player(s) (although there can only be one) who have successfully trapped cards to the
     * card(s) they have trapped, as a map from player index of the players who originally played the trapped cards to
     * the cards which were trapped.
     */
    val trappedCards: Map<Int, Map<Int, List<Card>>> = emptyMap(),

    /**
     * Map from player index to the cards which they played in this duel which were drawn from the deck (these values
     * are also present in [discardedCards], etc).
     */
    val drawnCards: Map<Int, List<Card>> = emptyMap(),
) {
    init {
        assertLazy { rounds.isNotEmpty() }
        assertLazy { rounds.last().result !is DuelRoundResult.DoubleDuel }
        assertLazy { rounds.dropLast(1).all { it.result is DuelRoundResult.DoubleDuel } }
        assertLazy { discardedCards.values.all { it.isNotEmpty() } }
        assertLazy { retainedCards.values.all { it.isNotEmpty() } }
        assertLazy { trappedCards.values.all { cardsByPlayer -> cardsByPlayer.isNotEmpty() } }
        assertLazy { trappedCards.values.all { cardsByPlayer -> cardsByPlayer.values.all { it.isNotEmpty() } } }
    }
}

/**
 * Represents a single round of dueling in which each involved player plays a card.
 */
data class DuelRound(
    /**
     * Map from player index to the [Card] they played, for players still involved in this dueling round.
     */
    val playedCards: Map<Int, Card>,

    /**
     * Map from player index to the [Card] which was drawn from the deck to be played, for players who drew their card
     * from the deck this round (values are also present in [playedCards]).
     */
    val drawnCards: Map<Int, Card>,

    /**
     * The outcome of the round.
     */
    val result: DuelRoundResult,
) {
    init {
        assert(playedCards.size >= 2)
    }
}

/**
 * Enumerates the possible outcomes for a round of dueling.
 */
sealed interface DuelRoundResult {
    /**
     * The case where all cards played were regular cards (not Traps or Counteracts) and the lowest card(s) lost.
     *
     * In most cases there is a single losing card and [losers] will have a single value, however in the case where
     * there were 3+ Traps involved in a previous round, instead of eliminating the lowest card the round is instead
     * resolved by finding the highest card (which is awarded the win and Traps all the other cards); in this case all
     * the other cards are considered [losers].
     */
    data class LowestLost(
        /**
         * Map from player index to the [Card] they played which was either the unique lowest or, when resolving a Trap
         * double-duel, not the highest.
         */
        val losers: Map<Int, Card>,

        /**
         * Map from player index to the [Card] they played which was either not the lowest or, when resolving a Trap
         * double-duel, the unique highest.
         */
        val winners: Map<Int, Card>,
    ) : DuelRoundResult {
        init {
            assert(losers.isNotEmpty())
        }
    }

    /**
     * The case where one or more counteracts were played and all in-play cards are discarded.
     */
    data class Counteracted(
        /**
         * The player indexes of player(s) who played a [Card.COUNTERACT].
         */
        val counteracters: Set<Int>,
    ) : DuelRoundResult {
        init {
            assert(counteracters.isNotEmpty())
        }
    }

    /**
     * The case where a single Trap was played, trapping all the cards in play.
     */
    data class Trapped(
        /**
         * The player index of the player who played the Trap.
         */
        val trapper: Int,
    ) : DuelRoundResult

    /**
     * The case where either (a) there is a tie for lowest value card or (b) multiple traps are played; in this case
     * (and only this case) another round of the duel will proceed among [doubleDuelers].
     */
    data class DoubleDuel(
        /**
         * Whether this double-duel is the result of multiple Traps in *this* round.
         */
        val trapping: Boolean,

        /**
         * Map from player index to the now-tied card played for only those players involved in this double-duel.
         */
        val doubleDuelers: Map<Int, Card>,
    ) : DuelRoundResult {
        init {
            assertLazy { doubleDuelers.size >= 2 }
            assertLazy { doubleDuelers.all { it.value.isTrap == trapping } }
        }
    }
}
