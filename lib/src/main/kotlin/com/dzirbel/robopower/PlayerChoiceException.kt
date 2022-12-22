package com.dzirbel.robopower

/**
 * Thrown when a [player] makes an invalid choice (such as out-of-bounds card or player indexes).
 */
sealed class PlayerChoiceException(val player: Player, detail: String) :
    Throwable("Invalid player choice for player ${player.playerIndex} ($player) : $detail") {

    /**
     * Thrown when the discarded card choice was an out-of-bounds index.
     */
    class InvalidDiscard internal constructor(player: Player, cardIndex: Int) : PlayerChoiceException(
        player = player,
        detail = "discard card index $cardIndex with ${player.handSize()} cards in hand",
    )

    /**
     * Thrown when a choice of spied player is invalid, either out-of-bounds or because the player chose themselves.
     */
    class InvalidSpy internal constructor(player: Player, spiedPlayerIndex: Int, players: Int) : PlayerChoiceException(
        player = player,
        detail = "spied player index $spiedPlayerIndex with $players players",
    )

    /**
     * Thrown when a choice of spied player is a player with no cards in their hand.
     */
    class SpiedEmptyHand internal constructor(player: Player, spiedPlayerIndex: Int) : PlayerChoiceException(
        player = player,
        detail = "spied player index $spiedPlayerIndex has no cards in hand",
    )

    /**
     * Thrown when the dueled card choice was an out-of-bounds index.
     */
    class InvalidDuel internal constructor(player: Player, cardIndex: Int) : PlayerChoiceException(
        player = player,
        detail = "duel card index $cardIndex with ${player.handSize(includeCardsInPlay = false)} cards in hand",
    )
}
