package com.dzirbel.robopower

import com.dzirbel.robopower.util.mapToFirstOrNull

/**
 * A [Player] which delegates to the given [discardStrategy], [spyStrategy], and [duelStrategy] to make its decisions.
 */
class CompositePlayer(
    playerIndex: Int,
    game: Game,
    private val discardStrategy: DiscardStrategy,
    private val spyStrategy: SpyStrategy,
    private val duelStrategy: DuelStrategy,
) : Player(playerIndex, game) {

    override fun discard() = discardStrategy.discard(playerState)

    override fun spy() = spyStrategy.spy(playerState)

    override fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int {
        return duelStrategy.duel(
            playerState = playerState,
            involvedPlayers = involvedPlayers,
            previousRounds = previousRounds,
        )
    }

    private class Factory(
        private val discardStrategy: (playerIndex: Int, game: Game) -> DiscardStrategy,
        private val spyStrategy: (playerIndex: Int, game: Game) -> SpyStrategy,
        private val duelStrategy: (playerIndex: Int, game: Game) -> DuelStrategy,
    ) : Player.Factory {
        override fun playerName(playerIndex: Int) = "Composite"

        override fun create(playerIndex: Int, game: Game): Player {
            return CompositePlayer(
                playerIndex = playerIndex,
                game = game,
                discardStrategy = discardStrategy(playerIndex, game),
                spyStrategy = spyStrategy(playerIndex, game),
                duelStrategy = duelStrategy(playerIndex, game),
            )
        }
    }

    companion object {
        /**
         * Creates a [Factory] of [CompositePlayer]s which delegates to players created by each of the given factories
         * for their respective operations.
         *
         * Note that each of these strategies creates a separate [Player], so [Player]-specific state is not shared
         * between them.
         */
        fun fromPlayers(discarder: Player.Factory, spier: Player.Factory, dueler: Player.Factory): Player.Factory {
            return Factory(
                discardStrategy = discarder::create,
                spyStrategy = spier::create,
                duelStrategy = dueler::create,
            )
        }

        /**
         * Creates a [Factory] of [CompositePlayer]s which delegates to the given respective strategies.
         */
        fun fromStrategies(
            discardStrategy: DiscardStrategy,
            spyStrategy: SpyStrategy,
            duelStrategy: DuelStrategy,
        ): Player.Factory {
            return Factory(
                discardStrategy = { _, _ -> discardStrategy },
                spyStrategy = { _, _ -> spyStrategy },
                duelStrategy = { _, _ -> duelStrategy },
            )
        }

        /**
         * Creates a [Factory] of [CompositePlayer]s which delegates to the given respective strategies, in order; e.g.
         * uses the first [discardStrategy] which provides a non-null [OptionalDiscardStrategy.discard].
         */
        fun fromOptionalStrategies(
            discardStrategies: List<OptionalDiscardStrategy>,
            spyStrategies: List<OptionalSpyStrategy>,
            duelStrategies: List<OptionalDuelStrategy>,
        ): Player.Factory {
            return Factory(
                discardStrategy = { _, _ ->
                    DiscardStrategy { playerState ->
                        discardStrategies.mapToFirstOrNull { it.discard(playerState) }
                            ?: error("no discard strategy provided a result")
                    }
                },
                spyStrategy = { _, _ ->
                    SpyStrategy { playerState ->
                        spyStrategies.mapToFirstOrNull { it.spy(playerState) }
                            ?: error("no spy strategy provided a result")
                    }
                },
                duelStrategy = { _, _ ->
                    DuelStrategy { playerState, involvedPlayers, previousRounds ->
                        duelStrategies.mapToFirstOrNull { it.duel(playerState, involvedPlayers, previousRounds) }
                            ?: error("no duel strategy provided a result")
                    }
                },
            )
        }
    }
}
