package com.dzirbel.robopower

/**
 * Represents some public event in the game, and can be accessed via [Game.onEvent] or [Game.eventLog].
 */
sealed interface GameEvent {
    /**
     * The [Game.turnCount] when this event was emitted.
     */
    val turnCount: Int

    /**
     * The [Game.upPlayer] when this event was emitted.
     */
    val upPlayerIndex: Int

    /**
     * Emitted when [upPlayerIndex] starts their turn, before they draw.
     */
    data class StartTurn(override val turnCount: Int, override val upPlayerIndex: Int) : GameEvent {
        constructor(game: Game) : this(turnCount = game.turnCount, upPlayerIndex = game.upPlayerIndex)
    }

    /**
     * Emitted when [upPlayerIndex] ends their turn, after the duel (but before [Game.upPlayer] is moved to the next player).
     */
    data class EndTurn(override val turnCount: Int, override val upPlayerIndex: Int) : GameEvent {
        constructor(game: Game) : this(turnCount = game.turnCount, upPlayerIndex = game.upPlayerIndex)
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
        constructor(game: Game, eliminatedPlayerIndex: Int) : this(
            turnCount = game.turnCount,
            upPlayerIndex = game.upPlayerIndex,
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
        constructor(game: Game, previousDiscard: List<Card>) : this(
            turnCount = game.turnCount,
            upPlayerIndex = game.upPlayerIndex,
            previousDiscard = previousDiscard,
        )
    }

    /**
     * Emitted when a player draws a card at the start of their turn (i.e. not during the initial deal or during a
     * duel).
     */
    data class PlayerDraw(override val turnCount: Int, override val upPlayerIndex: Int) : GameEvent {
        constructor(game: Game) : this(turnCount = game.turnCount, upPlayerIndex = game.upPlayerIndex)
    }

    /**
     * Emitted when a player discards a card for their turn (i.e. not from losing cards in a duel).
     */
    data class PlayerDiscard(
        override val turnCount: Int,
        override val upPlayerIndex: Int,
        val discardedCard: Card,
    ) : GameEvent {
        constructor(game: Game, discardedCard: Card) : this(
            turnCount = game.turnCount,
            upPlayerIndex = game.upPlayerIndex,
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
        constructor(game: Game, spied: Int, remainingCards: Int) : this(
            turnCount = game.turnCount,
            upPlayerIndex = game.upPlayerIndex,
            spiedPlayerIndex = spied,
            remainingCards = remainingCards,
        )

        init {
            assert(upPlayerIndex != spiedPlayerIndex)
        }
    }

    /**
     * Emitted at the conclusion of a duel, whose full sequence of events can be found in the [result].
     */
    data class Duel(
        override val turnCount: Int,
        override val upPlayerIndex: Int,
        val result: DuelResult,
    ) : GameEvent {
        constructor(game: Game, result: DuelResult) : this(
            turnCount = game.turnCount,
            upPlayerIndex = game.upPlayerIndex,
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
        constructor(game: Game, round: DuelRound) : this(
            turnCount = game.turnCount,
            upPlayerIndex = game.upPlayerIndex,
            round = round,
        )
    }
}
