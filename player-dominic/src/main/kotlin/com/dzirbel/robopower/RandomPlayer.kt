package com.dzirbel.robopower

import kotlin.random.Random

/**
 * Trivial [Player] which uses [random] to make all of its choices uniformly randomly.
 */
class RandomPlayer(playerState: PlayerState, private val random: Random = Random.Default) : Player(playerState) {

    override fun discard() = random.nextInt(until = hand.size)

    override fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>) = random.nextInt(until = hand.size)

    override fun spy() = gameState.activePlayers.filter { it.index != playerIndex }.random(random).index

    companion object : Factory {
        override fun create(playerState: PlayerState) = RandomPlayer(playerState)
    }
}
