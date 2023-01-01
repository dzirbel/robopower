package com.dzirbel.robopower

import com.dzirbel.robopower.util.MultiSet

/**
 * Provides bookkeeping for known cards in the game from public [GameEvent]s and private [PlayerEvent]s.
 *
 * TODO this is not perfect and misses some higher-order logic; for example if a player has two cards (both known) and
 *  one is spied away, then when they play a card in a duel it is known which was spied and this card can be deduced in
 *  the hand of the player who stole it (but currently is not)
 */
class CardTracker(
    gameState: GameState,
    private val deck: Deck,
    private val trackingPlayerIndex: Int,
    private val getHand: () -> List<Card>,
) {
    /**
     * The currently known cards in the game, as a map from player index to the known cards in their hand. This
     * [trackingPlayerIndex] is not included in the map.
     */
    val knownCards: Map<Int, List<Card>>
        get() = _knownCards.toMap()

    private val _knownCards: MutableMap<Int, MutableList<Card>> = List(gameState.playerCount) { it }
        .minus(trackingPlayerIndex)
        .associateWith { mutableListOf<Card>() }
        .toMutableMap()

    private var unknownCards: MultiSet<Card>? = null

    init {
        // on discard, remove one copy of the discarded card from the known cards
        gameState.onEventOfType<GameEvent.PlayerDiscard> { event ->
            if (event.upPlayerIndex != trackingPlayerIndex) {
                _knownCards.getValue(event.upPlayerIndex).remove(event.discardedCard)
                unknownCards = null
            }
        }

        // on duel, update known cards with all the results
        gameState.onEventOfType<GameEvent.Duel> { event ->
            val result = event.result

            for ((player, discarded) in result.discardedCards) {
                if (player != trackingPlayerIndex) {
                    _knownCards.getValue(player).removeEach(discarded)
                    unknownCards = null
                }
            }

            // for retained cards, these could have already been known, so only add those in addition to what we already
            // knew
            for ((player, retained) in result.retainedCards) {
                if (player == trackingPlayerIndex) continue

                val retainedCardCounts = mutableMapOf<Card, Int>()
                for (card in retained) {
                    retainedCardCounts.compute(card) { _, count -> (count ?: 0) + 1 }
                }
                val playerCards = _knownCards.getValue(player)

                for ((card, retainedCount) in retainedCardCounts) {
                    // if we have seen more cards of this type retained than we previously knew, add the extra to the
                    // known list. if we see the same number of fewer than previously known, do nothing
                    // TODO this (very rarely) loses some information for retained cards which were drawn from the deck
                    val knownCount = playerCards.count { it == card }
                    val newlySeenCount = retainedCount - knownCount
                    if (newlySeenCount > 0) {
                        repeat(newlySeenCount) {
                            playerCards.add(card)
                        }
                        unknownCards = null
                    }
                }
            }

            // for trapped cards, remove them from the player they were trapped from and add them to the player who
            // trapped them (always)
            for ((trapper, trappedMap) in result.trappedCards) {
                val trapperCards = _knownCards[trapper] // may be null when trapper is the tracking player
                for ((trappedFrom, trappedCards) in trappedMap) {
                    if (trappedFrom != trackingPlayerIndex) {
                        _knownCards.getValue(trappedFrom).removeEach(trappedCards)
                    }

                    trapperCards?.addAll(trappedCards)
                    unknownCards = null
                }
            }
        }

        // TODO for spy master, if they spy two cards from a player with only two cards then we can know what they are
        //  (currently only deduces this for the second card)
        gameState.onEventOfType<GameEvent.Spied> { event ->
            // ignore spies from or to the tracking player since they will be accounted for via onReceiveSpyCard and
            // onCardStolen instead
            if (event.upPlayerIndex != trackingPlayerIndex && event.spiedPlayerIndex != trackingPlayerIndex) {
                // if the player is now out of cards (only had one), and we knew what it was then we can add it to the
                // known cards of the player who received it
                if (event.remainingCards == 0) {
                    _knownCards.getValue(event.spiedPlayerIndex).firstOrNull()?.let { spiedCard ->
                        _knownCards.getValue(event.upPlayerIndex).add(spiedCard)
                    }
                }

                // we now know nothing about the player who lost a card since it could have been any card stolen
                _knownCards.getValue(event.spiedPlayerIndex).clear()

                unknownCards = null
            }
        }

        // if the discard pile is reshuffled in a one-on-one, we know exactly the cards held by the other player
        // TODO also true if there are e.g. 3 players and we know all the cards held by one of them
        gameState.onEventOfType<GameEvent.DiscardPileReshuffledIntoDrawPile> { event ->
            val active = gameState.activePlayers.filter { it.index != trackingPlayerIndex }
            if (active.size == 1) {
                val activePlayer = active.first().index
                _knownCards[activePlayer] = Card.deck.toMutableList().apply {
                    removeEach(event.previousDiscard)
                    removeEach(getHand())
                }
            }
        }
    }

    /**
     * A callback which must be invoked whenever this [trackingPlayerIndex] receives [card] via spy from
     * [fromPlayerIndex], typically from [Player.onReceiveSpyCard].
     */
    internal fun onReceiveSpyCard(card: Card, fromPlayerIndex: Int) {
        _knownCards.getValue(fromPlayerIndex).remove(card)
    }

    /**
     * A callback which must be invoked whenever this [trackingPlayerIndex] has a card [card] stolen via spy by
     * [byPlayerIndex], typically from [Player.onCardStolen].
     */
    internal fun onCardStolen(card: Card, byPlayerIndex: Int) {
        _knownCards.getValue(byPlayerIndex).add(card)
    }

    /**
     * Returns a list of the [Card]s which are not accounted for - either in the hands of other players or the draw
     * pile.
     *
     * This value is memoized between calls until there is any change in the set of known cards.
     *
     * TODO could be optimized to just update [unknownCards] on each change rather than recomputing, at least sometimes
     */
    fun unknownCards(): MultiSet<Card> {
        unknownCards?.let { return it }

        val cards = MultiSet(elements = Card.values().associateWith { it.multiplicity })
        cards.removeAll(getHand())
        cards.removeAll(deck.discardPile)
        for ((_, knownCards) in _knownCards) {
            cards.removeAll(knownCards)
        }
        return cards.also { unknownCards = it }
    }

    /**
     * Returns the number of [card]s which are unaccounted for.
     */
    fun countUnknown(card: Card): Int = unknownCards().count(card)

    /**
     * Returns the number of [card]s which have been seen.
     */
    fun countKnown(card: Card): Int = card.multiplicity - countUnknown(card)

    /**
     * Returns the number of cards satisfying [predicate] which are unaccounted for.
     */
    fun countUnknownWhere(predicate: (Card) -> Boolean): Int = unknownCards().count(predicate)

    /**
     * Returns the number of cards satisfying [predicate] which have been seen.
     */
    fun countKnownWhere(predicate: (Card) -> Boolean): Int = Card.deck.size - countUnknownWhere(predicate)

    /**
     * Removes all one copy of each of [elements] from this collection, as opposed to [removeAll] which removes all
     * copies.
     */
    private fun <T> MutableCollection<T>.removeEach(elements: Collection<T>) {
        for (element in elements) {
            remove(element)
        }
    }
}
