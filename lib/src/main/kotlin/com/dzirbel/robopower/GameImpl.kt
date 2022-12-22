package com.dzirbel.robopower

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

// TODO consider exceptions and invalid choices to be resignations instead of aborting the game?
// TODO document more
// TODO unit tests
class GameImpl(
    playerFactories: List<Player.Factory>,
    private val random: Random = Random.Default,
) : Game {
    override val deck = Deck(random = random)

    override var turnCount = 0
        private set

    override var upPlayerIndex = 0
        private set

    override val playerCount = playerFactories.size

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

    override val activePlayerCount: Int
        get() = players.count { it.isActive }

    override val activePlayers: List<IndexedValue<Player>>
        get() = players.withIndex().filter { it.value.isActive }

    private val _eventLog: MutableList<GameEvent> = mutableListOf()
    override val eventLog: List<GameEvent>
        get() = _eventLog.toList()

    private val eventListeners: MutableList<(GameEvent) -> Unit> = mutableListOf()

    private val playerCardSuppliers: List<Dueler.PlayerCardSupplier.FromPlayerAndDeck> by lazy {
        players.map {
            Dueler.PlayerCardSupplier.FromPlayerAndDeck(
                player = it, deck = deck,
                onDeckReshuffled = { previousDiscard ->
                    emitEvent(GameEvent.DiscardPileReshuffledIntoDrawPile(this, previousDiscard))
                },
            )
        }
    }

    private val started: AtomicBoolean = AtomicBoolean(false)

    init {
        assert(playerFactories.size in 2..MAX_PLAYERS)
    }

    override fun onEvent(onEvent: (GameEvent) -> Unit) {
        eventListeners.add(onEvent)
    }

    fun run(): GameResult {
        require(!started.getAndSet(true)) { "game is already running" }

        // deal starting hands; intentionally does not fire events since they are not actionable
        repeat(STARTING_CARDS) {
            for (player in players) {
                val (card, previousDiscard) = deck.draw()
                assert(previousDiscard == null)
                player.draw(card)
            }
        }

        while (true) {
            turnCount++
            emitEvent(GameEvent.StartTurn(game = this))

            draw()

            discard()
            // impossible for a tie after a discard so no need to provide previouslyActivePlayers
            checkGameResult(previouslyActivePlayers = null)?.let { return it }

            val playersBeforeDuel = players.withIndex().filter { it.value.isActive }.mapTo(mutableSetOf()) { it.index }
            duel()
            checkGameResult(previouslyActivePlayers = playersBeforeDuel)?.let { return it }

            emitEvent(GameEvent.EndTurn(game = this))
            upPlayerIndex = nextPlayerIndex
        }
    }

    private fun draw() {
        val (card, previousDiscard) = deck.draw()
        previousDiscard?.let {
            emitEvent(GameEvent.DiscardPileReshuffledIntoDrawPile(game = this, previousDiscard = previousDiscard))
        }

        upPlayer.draw(card)

        emitEvent(GameEvent.PlayerDraw(game = this))
        assertGameInvariants()
    }

    private fun discard() {
        val card = upPlayer.doDiscard()
        deck.discard(card)

        emitEvent(GameEvent.PlayerDiscard(game = this, discardedCard = card))
        assertGameInvariants()

        repeat(card.spyCount) { spy() }
    }

    private fun spy() {
        // abort spy if only one player left; this can happen when spy master's first spy ends the game
        if (activePlayerCount == 1) return

        val spiedPlayerIndex = upPlayer.doSpy()

        val (spiedCard, eliminated) = players[spiedPlayerIndex]
            .stealRandomCard(random = random, byPlayerIndex = upPlayerIndex)
        upPlayer.receiveSpyCard(card = spiedCard, fromPlayerIndex = spiedPlayerIndex)

        emitEvent(
            GameEvent.Spied(
                game = this,
                spied = spiedPlayerIndex,
                remainingCards = players[spiedPlayerIndex].handSize(),
            ),
        )
        if (eliminated) {
            emitEvent(GameEvent.PlayerEliminated(game = this, eliminatedPlayerIndex = spiedPlayerIndex))
        }
        assertGameInvariants()
    }

    private fun duel() {
        val previouslyActivePlayers = activePlayers.mapTo(mutableSetOf()) { it.index }

        val duelResult = Dueler.duel(
            players = playerCardSuppliers,
            onRound = { emitEvent(GameEvent.AfterDuelRound(game = this, round = it)) },
        )

        deck.discardAll(duelResult.discardedCards.flatMap { it.value })

        players.forEachIndexed { index, player ->
            player.postDuel(
                retainedCards = duelResult.retainedCards[index],
                trappedCards = duelResult.trappedCards[index]?.flatMap { it.value },
            )
        }

        val currentlyActivePlayers = activePlayers.mapTo(mutableSetOf()) { it.index }
        for (eliminatedPlayer in previouslyActivePlayers.minus(currentlyActivePlayers)) {
            emitEvent(GameEvent.PlayerEliminated(game = this, eliminatedPlayerIndex = eliminatedPlayer))
        }

        emitEvent(GameEvent.Duel(game = this, result = duelResult))
        assertGameInvariants()
    }

    private fun checkGameResult(previouslyActivePlayers: Set<Int>?): GameResult? {
        return when (activePlayerCount) {
            0 -> GameResult.Tied(game = this, tiedPlayers = requireNotNull(previouslyActivePlayers))
            1 -> GameResult.Winner(game = this, winner = players.indexOfFirst { it.isActive })
            else -> null
        }
    }

    private fun emitEvent(event: GameEvent) {
        _eventLog.add(event)
        eventListeners.forEach { it(event) }
    }

    private fun assertGameInvariants() {
        assert(Card.deck.size == deck.discardPile.size + deck.drawPileSize + players.sumOf { it.handSize() })
    }

    companion object {
        const val STARTING_CARDS = 6
        const val MAX_PLAYERS = 10
    }
}
