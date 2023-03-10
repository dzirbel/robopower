package com.dzirbel.robopower

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * Contains game logic triggered by [run] and exposes a [gameState] with the continually-updated state of the game and
 * allows registering [onEvent] callbacks.
 */
class Game(
    playerFactories: List<Player.Factory>,
    private val random: Random = Random.Default,
    deck: Deck = Deck(random = random),
) {
    /**
     * The current state of the game; always the same object reference.
     */
    val gameState: GameState = GameState(deck = deck, playerFactories = playerFactories)

    // map from player index to their place in the game
    // e.g. for a game of 8 players:
    // - player who is eliminated first gets 8th place (8)
    // - two players are eliminated in the game duel both get 6th place (6)
    // - a player is eliminated in the discard phase of a round gets 5th place (5)
    // - a player eliminated in the duel phase of the same round gets a 4th place (4)
    // - another player is eliminated in 3rd place (3)
    // - the two remaining players are eliminated together (by a counteract) and both get first place (1)
    private val places: MutableMap<Int, Int> = mutableMapOf()

    /**
     * The [PlayerState]s for each player; centralized in [Game] so that if there are multiple players for the same
     * index (for composite players which rely on multiple player instances), they all share the same state (and
     * [CardTracker], etc.).
     */
    private val playerStates: List<PlayerState> = gameState.playerStates

    private val playerCardSuppliers: Iterable<IndexedValue<Dueler.PlayerCardSupplier.FromPlayerAndDeck>> by lazy {
        gameState.players.map { player ->
            Dueler.PlayerCardSupplier.FromPlayerAndDeck(
                player = player,
                deck = deck,
                onDeckReshuffled = { previousDiscard ->
                    gameState.onEvent(GameEvent.DiscardPileReshuffledIntoDrawPile(gameState, previousDiscard))
                },
            )
        }
            .withIndex()
    }

    private val started: AtomicBoolean = AtomicBoolean(false)

    init {
        require(playerFactories.size in MIN_PLAYERS..MAX_PLAYERS) {
            "player count must be between $MIN_PLAYERS and $MAX_PLAYERS; was ${playerFactories.size}"
        }
    }

    /**
     * Runs the game to its conclusion, returning a [GameResult] with the final result.
     *
     * This function may only be called once.
     *
     * TODO separate function for partial runs which is nullable; this one can be non-null
     */
    fun run(maxRounds: Int? = null): GameResult? {
        require(!started.getAndSet(true)) { "game is already running" }

        // deal starting hands; intentionally does not fire events since they are not actionable
        repeat(STARTING_CARDS) {
            for (player in gameState.players) {
                val (card, previousDiscard) = gameState.deck.draw()
                assert(previousDiscard == null) // deck should not be reshuffled during the initial deal
                player.deal(card)
            }
        }

        while (maxRounds == null || gameState.turnCount < maxRounds) {
            gameState.turnCount++

            gameState.onEvent(GameEvent.StartTurn(gameState))
            gameState.assertGameInvariants()

            draw()
            discard()?.let { return it }
            duel()?.let { return it }

            gameState.onEvent(GameEvent.EndTurn(gameState))
            gameState.upPlayerIndex = gameState.nextPlayerIndex
        }

        return null
    }

    private fun draw() {
        val (card, previousDiscard) = gameState.deck.draw()
        if (previousDiscard != null) {
            gameState.onEvent(GameEvent.DiscardPileReshuffledIntoDrawPile(gameState, previousDiscard))
        }

        gameState.upPlayer.draw(card)

        gameState.onEvent(GameEvent.PlayerDraw(gameState))
        gameState.assertGameInvariants()
    }

    private fun discard(): GameResult? {
        val card = gameState.upPlayer.doDiscard()
        gameState.deck.discard(card)

        gameState.onEvent(GameEvent.PlayerDiscard(gameState, card))
        gameState.assertGameInvariants()

        repeat(card.spyCount) { _ ->
            spy()?.let { return it }
        }

        return null
    }

    private fun spy(): GameResult? {
        val spiedPlayerIndex = gameState.upPlayer.doSpy()

        val (spiedCard, remainingCards) = gameState.players[spiedPlayerIndex].stealRandomCard(
            random = random,
            byPlayerIndex = gameState.upPlayerIndex,
        )
        gameState.upPlayer.receiveSpyCard(card = spiedCard, fromPlayerIndex = spiedPlayerIndex)

        playerStates[spiedPlayerIndex].cardTracker
            .onCardStolen(card = spiedCard, byPlayerIndex = gameState.upPlayerIndex)
        playerStates[gameState.upPlayerIndex].cardTracker
            .onReceiveSpyCard(card = spiedCard, fromPlayerIndex = spiedPlayerIndex)

        gameState.onEvent(
            GameEvent.Spied(gameState = gameState, spied = spiedPlayerIndex, remainingCards = remainingCards),
        )

        if (remainingCards == 0) {
            onPlayersEliminated(eliminatedPlayers = setOf(spiedPlayerIndex))
                ?.let { return it }
        }

        gameState.assertGameInvariants()
        return null
    }

    private fun duel(): GameResult? {
        val involvedPlayerIndices = gameState.activePlayers.mapTo(mutableSetOf()) { it.index }

        // run the duel
        val duelResult = Dueler.duel(
            players = playerCardSuppliers.filter { it.index in involvedPlayerIndices },
            onRound = { gameState.onEvent(GameEvent.AfterDuelRound(gameState = gameState, round = it)) },
        )

        // add any discarded cards (losing cards, traps, counteracts) to the discard pile
        duelResult.discardedCards.forEach { (_, cards) ->
            cards.forEach { gameState.deck.discard(it) }
        }

        // add retained cards and trapped cards to the receiving players' hands (and reset their cound of cards in play)
        gameState.players.forEachIndexed { index, player ->
            if (index in involvedPlayerIndices) {
                player.postDuel(
                    retainedCards = duelResult.retainedCards[index],
                    trappedCards = duelResult.trappedCards[index]?.flatMap { it.value },
                )
            }
        }

        gameState.onEvent(GameEvent.Duel(gameState = gameState, result = duelResult))

        // check which players if any have been eliminated by this duel
        val remainingPlayerIndices = buildSet {
            gameState.players.forEachIndexed { index, player -> if (player.isActive) add(index) }
        }
        return if (remainingPlayerIndices.size < involvedPlayerIndices.size) {
            onPlayersEliminated(eliminatedPlayers = involvedPlayerIndices.minus(remainingPlayerIndices))
        } else {
            null
        }
            .also { gameState.assertGameInvariants(afterDuel = true) }
    }

    private fun onPlayersEliminated(eliminatedPlayers: Set<Int>): GameResult? {
        gameState.activePlayerCount -= eliminatedPlayers.size
        val remainingPlayers = gameState.activePlayerCount
        for (eliminatedPlayer in eliminatedPlayers) {
            places[eliminatedPlayer] = remainingPlayers + 1
            gameState.onEvent(
                GameEvent.PlayerEliminated(gameState = gameState, eliminatedPlayerIndex = eliminatedPlayer),
            )
        }

        if (remainingPlayers == 1) {
            places[gameState.activePlayers.first().index] = 1
        }

        return if (remainingPlayers <= 1) GameResult(game = this, playersToPlace = places) else null
    }

    companion object {
        /**
         * The number of cards dealt to each player at the start of the game.
         */
        const val STARTING_CARDS = 6

        /**
         * Minimum number of players in a game.
         */
        const val MIN_PLAYERS = 2

        /**
         * Maximum number of players in a game; arbitrarily chosen.
         */
        const val MAX_PLAYERS = 10
    }
}
