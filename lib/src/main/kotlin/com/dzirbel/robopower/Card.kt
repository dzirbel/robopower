package com.dzirbel.robopower

/**
 * Represents a card in the game.
 *
 * @param cardName the user-readable name; in most cases [toString] should be preferred
 * @param score the value of the card used for dueling; higher scores are better
 * @param spyCount the number of times a player may "spy" a card from an opponent when discarding
 * @param multiplicity the number of times this card appears in a standard deck
 */
enum class Card(val cardName: String, val score: Int?, val spyCount: Int = 0, val multiplicity: Int) {
    COUNTERACT(cardName = "Counteract", score = null, multiplicity = 3),
    TRAP(cardName = "Trap", score = null, multiplicity = 10),
    SPY_MASTER(cardName = "Spy Master", score = 1, spyCount = 2, multiplicity = 1),
    SPY(cardName = "Spy", score = 3, spyCount = 1, multiplicity = 4),
    BUZZY(cardName = "Buzzy", score = 10, multiplicity = 6),
    WIND(cardName = "Wind", score = 25, multiplicity = 7),
    SHOCK(cardName = "Shock", score = 30, multiplicity = 7),
    ROCK(cardName = "Rock", score = 35, multiplicity = 7),
    LIGHTOR(cardName = "Lightor", score = 40, multiplicity = 8),
    ZIP(cardName = "Zip", score = 50, multiplicity = 4),
    HAIRY(cardName = "Hairy", score = 51, multiplicity = 6),
    GRAPPLE(cardName = "Grapple", score = 60, multiplicity = 8),
    BRAINY(cardName = "Brainy", score = 65, multiplicity = 4),
    BLADE(cardName = "Blade", score = 70, multiplicity = 3),
    ALX(cardName = "Alx", score = 75, multiplicity = 3),
    BRAINIAC(cardName = "Brainiac", score = 85, multiplicity = 3),
    CRUSHER(cardName = "Crusher", score = 85, multiplicity = 3),
    RAM(cardName = "Ram", score = 120, multiplicity = 4),
    SLICE(cardName = "Slice", score = 160, multiplicity = 3),
    UN_BEAT(cardName = "Un-Beat", score = 200, multiplicity = 2),
    COPY(cardName = "Copy", score = 205, multiplicity = 2),
    ROBO_STRIKER(cardName = "Robo Striker", score = 210, multiplicity = 1),
    ;

    /**
     * Whether this card is the special Trap card; if so its [score] should be ignored.
     */
    val isTrap: Boolean
        get() = this == TRAP

    /**
     * Whether this card is the special Counteract card; if so its [score] should be ignored.
     */
    val isCounteract: Boolean
        get() = this == COUNTERACT

    /**
     * Determines whether this is a "normal" unit, i.e not a Trap, Counteract, or Spy.
     */
    val isNormal: Boolean
        get() = score != null && spyCount == 0

    /**
     * Determines whether this is a "normal" unit or a spy, i.e. not a Trap or Counteract.
     */
    val isNormalOrSpy
        get() = score != null

    /**
     * The relative rank of this [Card] based on its [score], with [TRAP] as higher than any normal card and
     * [COUNTERACT] above [TRAP].
     */
    val rank by lazy {
        when (this) {
            COUNTERACT -> requireNotNull(Card.values().maxBy { it.score ?: 0 }.scoreRank) + 2
            TRAP -> requireNotNull(Card.values().maxBy { it.score ?: 0 }.scoreRank) + 1
            else -> requireNotNull(scoreRank)
        }
    }

    /**
     * The relative rank of this [Card] based on its [score], or null if its [score] is null.
     *
     * This is a normalized [score] where the lowest card ([BUZZY]) is rank 0 and the highest card ([ROBO_STRIKER]) has
     * the highest rank. [TRAP] and [COUNTERACT] have null [scoreRank].
     */
    val scoreRank: Int? by lazy {
        if (score != null) Card.values().count { it.score != null && score > it.score } else null
    }

    /**
     * The number of normal cards (including their [multiplicity]) that have a strictly higher [score] than this [Card];
     * null for [TRAP] and [COUNTERACT].
     */
    val countStronger: Int? by lazy {
        if (score != null) {
            Card.values().sumOf { if (it.score != null && it.score > score) it.multiplicity else 0 }
        } else {
            null
        }
    }

    /**
     * The number of normal cards (including their [multiplicity]) that have an equal or higher [score] than this [Card]
     * (including this [Card] and its copies), null for [TRAP] and [COUNTERACT].
     */
    val countStrongerOrEqual: Int? by lazy {
        if (score != null) {
            Card.values().sumOf { if (it.score != null && it.score >= score) it.multiplicity else 0 }
        } else {
            null
        }
    }

    /**
     * The number of normal cards (including their [multiplicity]) that have a strictly lower [score] than this [Card];
     * null for [TRAP] and [COUNTERACT].
     */
    val countWeaker: Int? by lazy {
        if (score != null) {
            Card.values().sumOf { if (it.score != null && it.score < score) it.multiplicity else 0 }
        } else {
            null
        }
    }

    /**
     * The number of normal cards (including their [multiplicity]) that have an equal or lower [score] than this [Card];
     * null for [TRAP] and [COUNTERACT].
     */
    val countWeakerOrEqual: Int? by lazy {
        if (score != null) {
            Card.values().sumOf { if (it.score != null && it.score <= score) it.multiplicity else 0 }
        } else {
            null
        }
    }

    override fun toString(): String {
        return if (isTrap || isCounteract) cardName else "$cardName [$score]"
    }

    companion object {
        /**
         * A [Comparator] for [Card]s which compares based on the relative [score]s (Traps and Counteracts are counted
         * as the lowest).
         */
        val comparatorByScore: Comparator<Card> = Comparator.comparingInt { it.score ?: 0 }

        /**
         * A [List] of [Card]s in arbitrary (unshuffled) order where each [Card] is present [multiplicity] times.
         */
        val deck: List<Card> by lazy {
            values().flatMap { card ->
                List(card.multiplicity) { card }
            }
        }
    }
}
