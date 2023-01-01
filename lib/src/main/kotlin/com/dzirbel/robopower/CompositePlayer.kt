package com.dzirbel.robopower

import com.dzirbel.robopower.util.mapToFirstOrNull

/**
 * A [Player] which delegates to the given [discardStrategy], [spyStrategy], and [duelStrategy] to make its decisions.
 */
class CompositePlayer(
    playerState: PlayerState,
    private val discardStrategy: DiscardStrategy,
    private val spyStrategy: SpyStrategy,
    private val duelStrategy: DuelStrategy,
) : Player(playerState) {

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
        private val discardStrategy: (playerState: PlayerState) -> DiscardStrategy,
        private val spyStrategy: (playerState: PlayerState) -> SpyStrategy,
        private val duelStrategy: (playerState: PlayerState) -> DuelStrategy,
    ) : Player.Factory {
        override fun playerName(playerIndex: Int) = "Composite"

        override fun create(playerState: PlayerState): Player {
            return CompositePlayer(
                playerState = playerState,
                discardStrategy = discardStrategy(playerState),
                spyStrategy = spyStrategy(playerState),
                duelStrategy = duelStrategy(playerState),
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
                discardStrategy = { _ -> discardStrategy },
                spyStrategy = { _ -> spyStrategy },
                duelStrategy = { _ -> duelStrategy },
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
                discardStrategy = { _ ->
                    DiscardStrategy { playerState ->
                        discardStrategies.mapToFirstOrNull { it.discard(playerState) }
                            ?: error("no discard strategy provided a result")
                    }
                },
                spyStrategy = { _ ->
                    SpyStrategy { playerState ->
                        spyStrategies.mapToFirstOrNull { it.spy(playerState) }
                            ?: error("no spy strategy provided a result")
                    }
                },
                duelStrategy = { _ ->
                    DuelStrategy { playerState, involvedPlayers, previousRounds ->
                        duelStrategies.mapToFirstOrNull { it.duel(playerState, involvedPlayers, previousRounds) }
                            ?: error("no duel strategy provided a result")
                    }
                },
            )
        }
    }
}
