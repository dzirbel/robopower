package com.dzirbel.robopower

/**
 * Defines the game API available to [Player]s. This is separate from [GameImpl] to make it easy to see what fields and
 * functions are available.
 */
interface Game {
    /**
     * The [Deck] used to draw and discard cards; may be used to access the discard pile or size of draw pile.
     */
    val deck: Deck

    /**
     * Number of turns played in the game; 0 before the first turn (for the initial deal), then incremented at the start
     * of each turn (before the [upPlayer] draws).
     */
    val turnCount: Int

    /**
     * The number of players in the game.
     */
    val playerCount: Int

    /**
     * The [Player]s in the game, in the official order (i.e. player indexes are computed relative to this list).
     *
     * This allows access to [Player.handSize], etc.
     *
     * Note that this contains all players, even those who have been eliminated (i.e. have no cards left); use
     * [activePlayers] to get only the players still in the game.
     */
    val players: List<Player>

    /**
     * Index of the player whose turn it is (0 before the first turn starts).
     */
    val upPlayerIndex: Int

    /**
     * [Player] whose turn it is.
     */
    val upPlayer: Player

    /**
     * Index of the player whose turn is next after the [upPlayer]; taking into account resetting to player index 0 and
     * skipping eliminated players.
     */
    val nextPlayerIndex: Int

    /**
     * Number of players left in the game.
     */
    val activePlayerCount: Int

    /**
     * The players still left in the game; returned as [IndexedValue] for access to their nominal index (since their
     * index in this list may be different if some players are eliminated).
     */
    val activePlayers: List<IndexedValue<Player>>

    /**
     * A [List] of all [GameEvent]s occurring so far.
     *
     * Note that this is an immutable snapshot of events; new events are not added to it.
     */
    val eventLog: List<GameEvent>

    /**
     * Registers the [onEvent] callback to be invoked whenever a [GameEvent] occurs.
     */
    fun onEvent(onEvent: (GameEvent) -> Unit)
}

/**
 * A convenience wrapper around [onEvent] to add a listener to only events of type [E].
 */
inline fun <reified E : GameEvent> Game.onEventOfType(crossinline onEvent: (E) -> Unit) {
    onEvent { event ->
        if (event is E) { onEvent(event) }
    }
}
