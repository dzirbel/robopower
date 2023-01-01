package com.dzirbel.robopower

/**
 * Contains the (mutable) public state of a [Game], such as whose turn it is ([upPlayerIndex]) and a log of past
 * [GameEvent]s ([eventLog]).
 *
 * The [GameState] is mutable: it is the same object instance whose values (like [turnCount]) are updated as the game
 * progresses. While immutable data structures are easier to reason about, having [GameState] be mutable increases
 * performance (avoiding allocating new objects every time the state changes) and avoids issues with references like
 * [players] that cannot be immutable.
 */
class GameState(
    playerFactories: List<Player.Factory>,

    /**
     * The [Deck] used to draw and discard cards; may be used to access the discard pile or size of draw pile.
     */
    val deck: Deck = Deck(),
) {
    internal val playerStates: List<PlayerState> by lazy {
        List(playerFactories.size) { playerIndex ->
            PlayerState(playerIndex = playerIndex, gameState = this)
        }
    }

    /**
     * The [Player]s in the game, in the official order (i.e. player indexes are computed relative to this list).
     *
     * This allows access to [Player.handSize], etc.
     *
     * Note that this contains all players, even those who have been eliminated (i.e. have no cards left); use
     * [activePlayers] to get only the players still in the game.
     */
    val players: List<Player> by lazy {
        playerFactories.mapIndexed { playerIndex, factory ->
            val player = factory.create(playerStates[playerIndex])
            if (player.playerIndex != playerIndex) {
                throw PlayerThrownException(player, AssertionError("playerIndex has been tampered with"))
            }
            player
        }
    }

    /**
     * The number of players in the game.
     */
    val playerCount: Int = playerFactories.size

    /**
     * Number of turns played in the game; 0 before the first turn (for the initial deal), then incremented at the start
     * of each turn (before the [upPlayer] draws).
     */
    var turnCount: Int = 0
        internal set

    /**
     * Index of the player whose turn it is (0 before the first turn starts).
     */
    var upPlayerIndex: Int = 0
        internal set

    /**
     * [Player] whose turn it is.
     */
    val upPlayer: Player
        get() = players[upPlayerIndex]

    /**
     * Index of the player whose turn is next after the [upPlayer]; taking into account resetting to player index 0 and
     * skipping eliminated players.
     */
    val nextPlayerIndex: Int
        get() {
            var playerIndex = upPlayerIndex
            repeat(players.size) {
                playerIndex = (playerIndex + 1) % players.size
                if (players[playerIndex].isActive) return playerIndex
            }
            error("no active players")
        }

    /**
     * Number of players left in the game.
     */
    var activePlayerCount: Int = playerCount
        internal set

    /**
     * The players still left in the game; returned as [IndexedValue] for access to their nominal index (since their
     * index in this list may be different if some players are eliminated).
     */
    val activePlayers: Iterable<IndexedValue<Player>>
        get() = players.withIndex().filter { it.value.isActive }

    /**
     * A [List] of all [GameEvent]s occurring so far.
     *
     * Note that this is an immutable snapshot of events; new events are not added to it.
     */
    val eventLog: List<GameEvent>
        get() = _eventLog.toList() // TODO lazy iterable?

    private val _eventLog: MutableList<GameEvent> = mutableListOf()

    private val eventListeners: MutableList<(GameEvent) -> Unit> = mutableListOf()

    /**
     * Registers the [onEvent] callback to be invoked whenever a [GameEvent] occurs.
     */
    fun onEvent(onEvent: (GameEvent) -> Unit) {
        eventListeners.add(onEvent)
    }

    /**
     * A convenience wrapper around [onEvent] to add a listener to only events of type [E].
     */
    inline fun <reified E : GameEvent> onEventOfType(crossinline onEvent: (E) -> Unit) {
        onEvent { event ->
            if (event is E) { onEvent(event) }
        }
    }

    internal fun onEvent(event: GameEvent) {
        _eventLog.add(event)
        eventListeners.forEach { it(event) }
    }

    internal fun assertGameInvariants(afterDuel: Boolean = false) {
        ifAssertionsEnabled {
            // TODO add more game invariants: for each GameEvent, etc

            val reshuffled = _eventLog.any { it is GameEvent.DiscardPileReshuffledIntoDrawPile }

            // total cards in play is constant
            assert(Card.deck.size == deck.discardPileSize + deck.drawPileSize + players.sumOf { it.handSize() })

            // discard pile is at least the number of turns-1 * 2 (one discard, one duel; subtract one from turns in
            // case this happens before discards this turn); until it has been reshuffled
            assert(reshuffled || deck.discardPileSize >= 2 * (turnCount - 1))

            // player whose is up is still in the game (if there are any active players and this is not immediately
            // after the duel where they may have been eliminated)
            assert(afterDuel || activePlayerCount == 0 || upPlayer.isActive)

            // number of StartTurn events is equal to the turnCount
            assert(_eventLog.count { it is GameEvent.StartTurn } == turnCount)

            // number of EndTurn events is equal to the turnCount minus 1
            assert(_eventLog.count { it is GameEvent.EndTurn } == (turnCount - 1).coerceAtLeast(0))

            // activePlayerCount is accurate
            assert(activePlayerCount == players.count { it.isActive })

            // all players are either active or a PlayerEliminated event was emitted
            assert(activePlayerCount + _eventLog.count { it is GameEvent.PlayerEliminated } == playerCount)
        }
    }
}
