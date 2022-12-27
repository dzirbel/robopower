package com.dzirbel.robopower

/**
 * Contains the (mutable) private state of the game from the point of view of the player with index [playerIndex], such
 * as the player's [hand] and a [cardTracker].
 */
@Suppress("UseDataClass")
class PlayerState internal constructor(
    /**
     * The index in [GameState.players] of the player whose point of view is being captured.
     */
    val playerIndex: Int,

    /**
     * A reference to the public [GameState] (so that [PlayerState] can be the only input to strategies like
     * [DuelStrategy] and still provide information on the public state).
     */
    val gameState: GameState,

    game: Game,
) {
    /**
     * A [CardTracker] which does bookkeeping on known cards in other players' hands.
     */
    val cardTracker: CardTracker = CardTracker(
        game = game,
        deck = gameState.deck,
        trackingPlayerIndex = playerIndex,
        getHand = { _hand },
    )

    @Suppress("VariableNaming") // TODO refactor to make private
    internal val _hand: MutableList<Card> = mutableListOf()

    /**
     * The current [Card]s in this [Player]'s hand.
     *
     * Cards are always added to the end of the list (including when returning them to the hand from a duel). It might
     * be better to insert them at the index they were taken, but this can be very difficult to do (since only some of
     * the played cards might be retained, with traps discarded) and the benefit is minimal.
     */
    val hand: List<Card>
        get() = _hand.toList()
}
