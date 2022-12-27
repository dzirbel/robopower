package com.dzirbel.robopower

fun interface DiscardStrategy : OptionalDiscardStrategy {
    override fun discard(playerState: PlayerState): Int
}

fun interface OptionalDiscardStrategy {
    fun discard(playerState: PlayerState): Int?
}

fun interface SpyStrategy : OptionalSpyStrategy {
    override fun spy(playerState: PlayerState): Int
}

fun interface OptionalSpyStrategy {
    fun spy(playerState: PlayerState): Int?
}

fun interface DuelStrategy : OptionalDuelStrategy {
    override fun duel(playerState: PlayerState, involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int
}

fun interface OptionalDuelStrategy {
    fun duel(playerState: PlayerState, involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int?
}
