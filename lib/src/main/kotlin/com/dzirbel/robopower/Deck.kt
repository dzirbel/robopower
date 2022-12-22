package com.dzirbel.robopower

import kotlin.random.Random

/**
 * Manages the draw and discard piles of a deck of [Card]s.
 */
class Deck internal constructor(
    drawPile: List<Card>,
    discardPile: List<Card> = emptyList(),
    private val random: Random = Random.Default,
) {
    constructor(random: Random = Random.Default) : this(drawPile = Card.deck.shuffled(random), random = random)

    private val drawPile: MutableList<Card> = drawPile.toMutableList()
    private val _discardPile: MutableList<Card> = discardPile.toMutableList()

    /**
     * The number of cards in the draw pile.
     */
    val drawPileSize: Int
        get() = drawPile.size

    /**
     * The cards in the discard pile, in order, with the most recently added cards last.
     */
    val discardPile: List<Card>
        get() = _discardPile.toList()

    /**
     * The number of cards in the discard pile.
     */
    val discardPileSize: Int
        get() = _discardPile.size

    /**
     * Draws a [Card] from the deck, removing it from the draw pile.
     *
     * If the draw pile is empty, the discard pile is reshuffled into the deck. If there are no values in either the
     * draw or discard pile a [DeckExhaustedException] is thrown (this should only happen in extremely exceptional
     * cases).
     *
     * @return the drawn [Card] and a the list of cards in the discard pile if it was reshuffled into the draw pile
     */
    internal fun draw(): Pair<Card, List<Card>?> {
        val previousDiscard = if (drawPile.isEmpty()) {
            if (_discardPile.isEmpty()) {
                throw DeckExhaustedException()
            }

            shuffleDiscardIntoDraw()
        } else {
            null
        }

        return Pair(drawPile.removeLast(), previousDiscard)
    }

    /**
     * Adds [card] to the top (end) of the discard pile.
     */
    internal fun discard(card: Card) {
        _discardPile.add(card)
    }

    /**
     * Adds all the [Card]s in [cards] to the top (end) of the discard pile.
     */
    internal fun discardAll(cards: Collection<Card>) {
        _discardPile.addAll(cards)
    }

    /**
     * Reshuffles the current [discardPile] into the [drawPile], returning the cards previously in [discardPile].
     */
    private fun shuffleDiscardIntoDraw(): List<Card> {
        val previousDiscard = _discardPile.toList()
        drawPile.addAll(_discardPile.shuffled(random))
        _discardPile.clear()
        return previousDiscard
    }

    class DeckExhaustedException internal constructor(): Throwable("There are no cards in the draw or discard piles")
}
