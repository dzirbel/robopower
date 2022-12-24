package com.dzirbel.robopower

/**
 * Trivial [Player] who discards and duels the first card in their hand each round and spies the lowest-index player.
 */
class InOrderPlayer(playerIndex: Int, game: Game) : Player(playerIndex, game) {
    override fun discard() = 0
    override fun spy() = game.activePlayers.first { it.index != playerIndex && it.value.isActive }.index
    override fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>) = 0

    companion object : Factory {
        override fun create(playerIndex: Int, game: Game) = InOrderPlayer(playerIndex, game)
    }
}
