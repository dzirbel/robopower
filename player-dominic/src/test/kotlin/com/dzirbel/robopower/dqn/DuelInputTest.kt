package com.dzirbel.robopower.dqn

import com.dzirbel.robopower.Card
import com.dzirbel.robopower.GameState
import com.dzirbel.robopower.PlayerState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DuelInputTest {
    @Test
    fun `empty state has expected input buffer size`() {
        val gameState = GameState(playerFactories = listOf())
        val playerState = PlayerState(playerIndex = 0, gameState = gameState, hand = listOf(Card.ROBO_STRIKER))

        val input = playerState.toDuelInput(
            playedCard = Card.ROBO_STRIKER,
            involvedPlayers = setOf(),
            previousRounds = listOf(),
        )

        assertEquals(duelBufferSize, input.size)
    }
}
