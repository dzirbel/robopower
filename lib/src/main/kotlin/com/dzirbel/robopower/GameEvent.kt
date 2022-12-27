package com.dzirbel.robopower

/**
 * Represents some public event in the game, and can be accessed via [Game.onEvent] or [GameState.eventLog].
 */
sealed interface GameEvent {
    /**
     * The [GameState.turnCount] when this event was emitted.
     */
    val turnCount: Int

    /**
     * The [GameState.upPlayer] when this event was emitted.
     */
    val upPlayerIndex: Int

    /**
     * Emitted when [upPlayerIndex] starts their turn, before they draw.
     */
    data class StartTurn(override val turnCount: Int, override val upPlayerIndex: Int) : GameEvent {
        constructor(gameState: GameState) : this(
            turnCount = gameState.turnCount,
            upPlayerIndex = gameState.upPlayerIndex,
        )
    }

    /**
     * Emitted when [upPlayerIndex] ends their turn, after the duel (but before [GameState.upPlayer] is moved to the
     * next player).
     */
    data class EndTurn(override val turnCount: Int, override val upPlayerIndex: Int) : GameEvent {
        constructor(gameState: GameState) : this(
            turnCount = gameState.turnCount,
            upPlayerIndex = gameState.upPlayerIndex,
        )
    }

    /**
     * Emitted when a player is eliminated from the game, either by a spy or in a duel (after the duel has completely
     * finished).
     */
    data class PlayerEliminated(
        override val turnCount: Int,
        override val upPlayerIndex: Int,
        val eliminatedPlayerIndex: Int,
    ) : GameEvent {
        constructor(gameState: GameState, eliminatedPlayerIndex: Int) : this(
            turnCount = gameState.turnCount,
            upPlayerIndex = gameState.upPlayerIndex,
            eliminatedPlayerIndex = eliminatedPlayerIndex,
        )
    }

    /**
     * Emitted when the draw pile is exhausted and the discard pile is reshuffled into it.
     */
    data class DiscardPileReshuffledIntoDrawPile(
        override val turnCount: Int,
        override val upPlayerIndex: Int,

        /**
         * The cards in the discard pile before the reshuffling.
         */
        val previousDiscard: List<Card>,
    ) : GameEvent {
        constructor(gameState: GameState, previousDiscard: List<Card>) : this(
            turnCount = gameState.turnCount,
            upPlayerIndex = gameState.upPlayerIndex,
            previousDiscard = previousDiscard,
        )
    }

    /**
     * Emitted when a player draws a card at the start of their turn (i.e. not during the initial deal or during a
     * duel).
     */
    data class PlayerDraw(override val turnCount: Int, override val upPlayerIndex: Int) : GameEvent {
        constructor(gameState: GameState) : this(
            turnCount = gameState.turnCount,
            upPlayerIndex = gameState.upPlayerIndex,
        )
    }

    /**
     * Emitted when a player discards a card for their turn (i.e. not from losing cards in a duel); prior stealing cards
     * from spies.
     */
    data class PlayerDiscard(
        override val turnCount: Int,
        override val upPlayerIndex: Int,
        val discardedCard: Card,
    ) : GameEvent {
        constructor(gameState: GameState, discardedCard: Card) : this(
            turnCount = gameState.turnCount,
            upPlayerIndex = gameState.upPlayerIndex,
            discardedCard = discardedCard,
        )
    }

    /**
     * Emitted when [upPlayerIndex] steals a card from [spiedPlayerIndex], who now has [remainingCards] in their hand.
     */
    data class Spied(
        override val turnCount: Int,
        override val upPlayerIndex: Int,
        val spiedPlayerIndex: Int,
        val remainingCards: Int,
    ) : GameEvent {
        init {
            assert(upPlayerIndex != spiedPlayerIndex)
        }

        constructor(gameState: GameState, spied: Int, remainingCards: Int) : this(
            turnCount = gameState.turnCount,
            upPlayerIndex = gameState.upPlayerIndex,
            spiedPlayerIndex = spied,
            remainingCards = remainingCards,
        )
    }

    /**
     * Emitted at the conclusion of a duel, whose full sequence of events can be found in the [result].
     */
    data class Duel(
        override val turnCount: Int,
        override val upPlayerIndex: Int,
        val result: DuelResult,
    ) : GameEvent {
        constructor(gameState: GameState, result: DuelResult) : this(
            turnCount = gameState.turnCount,
            upPlayerIndex = gameState.upPlayerIndex,
            result = result,
        )
    }

    /**
     * Emitted after each [round] of a duel.
     */
    data class AfterDuelRound(
        override val turnCount: Int,
        override val upPlayerIndex: Int,
        val round: DuelRound,
    ) : GameEvent {
        constructor(gameState: GameState, round: DuelRound) : this(
            turnCount = gameState.turnCount,
            upPlayerIndex = gameState.upPlayerIndex,
            round = round,
        )
    }
}
