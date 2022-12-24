package com.dzirbel.robopower

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * The actual game logic backing a [Game]; separate just to keep the public API of [Game] clean and obvious.
 */
class GameImpl(
    playerFactories: List<Player.Factory>,
    private val random: Random = Random.Default,
    override val deck: Deck = Deck(random = random),
) : Game {
    override var turnCount = 0
        private set

    override var upPlayerIndex = 0
        private set

    override val playerCount = playerFactories.size

    /**
     * The concrete [Player]s, created from [Player.Factory] factories. This is `by lazy` so that initialization of the
     * [GameImpl] is finished before player constructors are called, in case they need to reference [playerCount],
     * [deck], etc.
     */
    override val players: List<Player> by lazy {
        playerFactories.mapIndexed { index, factory -> factory.create(index, this) }
    }

    override val upPlayer: Player
        get() = players[upPlayerIndex]

    override val nextPlayerIndex: Int
        get() {
            var playerIndex = upPlayerIndex
            repeat(players.size) {
                playerIndex = (playerIndex + 1) % players.size
                if (players[playerIndex].isActive) return playerIndex
            }
            error("no active players")
        }

    override var activePlayerCount: Int = playerCount
        private set

    override val activePlayers: List<IndexedValue<Player>>
        get() = players.withIndex().filter { it.value.isActive }

    private val _eventLog: MutableList<GameEvent> = mutableListOf()
    override val eventLog: List<GameEvent>
        get() = _eventLog.toList()

    private val eventListeners: MutableList<(GameEvent) -> Unit> = mutableListOf()

    private val playerCardSuppliers: Iterable<IndexedValue<Dueler.PlayerCardSupplier.FromPlayerAndDeck>> by lazy {
        players.map { player ->
            Dueler.PlayerCardSupplier.FromPlayerAndDeck(
                player = player,
                deck = deck,
                onDeckReshuffled = { previousDiscard ->
                    emitEvent(GameEvent.DiscardPileReshuffledIntoDrawPile(this, previousDiscard))
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

    override fun onEvent(onEvent: (GameEvent) -> Unit) {
        eventListeners.add(onEvent)
    }

    /**
     * Runs the game to its conclusion, returning a [GameResult] with the final result.
     *
     * This function may onl be called once.
     */
    fun run(maxRounds: Int? = null): GameResult? {
        require(!started.getAndSet(true)) { "game is already running" }

        // deal starting hands; intentionally does not fire events since they are not actionable
        repeat(STARTING_CARDS) {
            for (player in players) {
                val (card, previousDiscard) = deck.draw()
                assert(previousDiscard == null) // deck should not be reshuffled during the initial deal
                player.draw(card)
            }
        }

        while (maxRounds == null || turnCount < maxRounds) {
            turnCount++

            emitEvent(GameEvent.StartTurn(game = this))
            assertGameInvariants()

            draw()
            discard()?.let { return it }
            duel()?.let { return it }

            emitEvent(GameEvent.EndTurn(game = this))
            upPlayerIndex = nextPlayerIndex
        }

        return null
    }

    private fun draw() {
        val (card, previousDiscard) = deck.draw()
        if (previousDiscard != null) {
            emitEvent(GameEvent.DiscardPileReshuffledIntoDrawPile(game = this, previousDiscard = previousDiscard))
        }

        upPlayer.draw(card)

        emitEvent(GameEvent.PlayerDraw(game = this))
        assertGameInvariants()
    }

    private fun discard(): GameResult? {
        val card = upPlayer.doDiscard()
        deck.discard(card)

        emitEvent(GameEvent.PlayerDiscard(game = this, discardedCard = card))
        assertGameInvariants()

        repeat(card.spyCount) { _ ->
            spy()?.let { return it }
        }

        return null
    }

    private fun spy(): GameResult? {
        val spiedPlayerIndex = upPlayer.doSpy()

        val (spiedCard, remainingCards) = players[spiedPlayerIndex].stealRandomCard(
            random = random,
            byPlayerIndex = upPlayerIndex,
        )
        upPlayer.receiveSpyCard(card = spiedCard, fromPlayerIndex = spiedPlayerIndex)

        emitEvent(GameEvent.Spied(game = this, spied = spiedPlayerIndex, remainingCards = remainingCards))

        if (remainingCards == 0) {
            activePlayerCount--
            emitEvent(GameEvent.PlayerEliminated(game = this, eliminatedPlayerIndex = spiedPlayerIndex))

            if (activePlayerCount == 1) {
                return GameResult.Winner(game = this, winner = upPlayerIndex)
            }
        }

        assertGameInvariants()
        return null
    }

    private fun duel(): GameResult? {
        val involvedPlayerIndices = activePlayers.mapTo(mutableSetOf()) { it.index }

        // run the duel
        val duelResult = Dueler.duel(
            players = playerCardSuppliers.filter { it.index in involvedPlayerIndices },
            onRound = { emitEvent(GameEvent.AfterDuelRound(game = this, round = it)) },
        )

        // add any discarded cards (losing cards, traps, counteracts) to the discard pile
        duelResult.discardedCards.forEach { (_, cards) ->
            cards.forEach { deck.discard(it) }
        }

        // add retained cards and trapped cards to the receiving players' hands (and reset their cound of cards in play)
        players.forEachIndexed { index, player ->
            if (index in involvedPlayerIndices) {
                player.postDuel(
                    retainedCards = duelResult.retainedCards[index],
                    trappedCards = duelResult.trappedCards[index]?.flatMap { it.value },
                )
            }
        }

        emitEvent(GameEvent.Duel(game = this, result = duelResult))

        // check which players if any have been eliminated by this duel
        val remainingPlayerIndices = buildSet {
            players.forEachIndexed { index, player -> if (player.isActive) add(index) }
        }
        return if (remainingPlayerIndices.size < involvedPlayerIndices.size) {
            val eliminatedPlayers = involvedPlayerIndices.minus(remainingPlayerIndices)
            for (eliminatedPlayer in eliminatedPlayers) {
                emitEvent(GameEvent.PlayerEliminated(game = this, eliminatedPlayerIndex = eliminatedPlayer))
            }
            activePlayerCount -= eliminatedPlayers.size

            // check if the game has ended
            when (activePlayerCount) {
                0 -> GameResult.Tied(game = this, tiedPlayers = involvedPlayerIndices)
                1 -> GameResult.Winner(game = this, winner = remainingPlayerIndices.first())
                else -> null
            }
        } else {
            null
        }
            .also { assertGameInvariants(afterDuel = true) }
    }

    private fun emitEvent(event: GameEvent) {
        _eventLog.add(event)
        eventListeners.forEach { it(event) }
    }

    private fun assertGameInvariants(afterDuel: Boolean = false) {
        ifAssertionsEnabled {
            // TODO add more game invariants: for each GameEvent, etc

            val reshuffled = _eventLog.any { it is GameEvent.DiscardPileReshuffledIntoDrawPile }

            // total cards in play is constant
            assert(Card.deck.size == deck.discardPileSize + deck.drawPileSize + players.sumOf { it.handSize() })

            // discard pile is at least the number of turns-1 * 2 (one discard, one duel; subtract one from turns in
            // case this happens before discards this turn); until it has been reshuffled
            assert(reshuffled || deck.discardPileSize >= 2 * (turnCount - 1))

            // player whose is up is still in the game (if there are any active players and this is not immediately
            // after the duel where they may have been eliminated)
            assert(afterDuel || activePlayerCount == 0 || upPlayer.isActive)

            // number of StartTurn events is equal to the turnCount
            assert(_eventLog.count { it is GameEvent.StartTurn } == turnCount)

            // number of EndTurn events is equal to the turnCount minus 1
            assert(_eventLog.count { it is GameEvent.EndTurn } == (turnCount - 1).coerceAtLeast(0))

            // activePlayerCount is accurate
            assert(activePlayerCount == players.count { it.isActive })

            // all players are either active or a PlayerEliminated event was emitted
            assert(activePlayerCount + _eventLog.count { it is GameEvent.PlayerEliminated } == playerCount)
        }
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
