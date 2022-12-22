package com.dzirbel.robopower

/**
 * Convenience wrapper around [Player] with callbacks to a [cardTracker]. Note that overriding [onReceiveSpyCard] or
 * [onCardStolen] must call the super method.
 *
 * TODO do this via composition instead of inheritance
 */
abstract class PlayerWithCardTracker(playerIndex: Int, game: Game) : Player(playerIndex, game) {
    protected val cardTracker = CardTracker(game = game, trackingPlayerIndex = playerIndex, getHand = { hand })

    override fun onReceiveSpyCard(card: Card, fromPlayerIndex: Int) {
        cardTracker.onReceiveSpyCard(card, fromPlayerIndex)
    }

    override fun onCardStolen(card: Card, byPlayerIndex: Int) {
        cardTracker.onCardStolen(card, byPlayerIndex)
    }
}
