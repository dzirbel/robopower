package com.dzirbel.robopower

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * The base class for agents in the game.
 *
 * To implement a player, override [discard] (to choose a card from the hand to discard each turn), [spy] (to choose a
 * player to steal from when discarding spies), and [duel] (to choose a card to play in a duel or double-duel). To
 * inform their strategy [Player]s may also listen for public [GameEvent]s via [Game.onEvent] (and [Game.onEventOfType])
 * or by accessing the [Game.eventLog] and may override callbacks [onDraw], [onCardStolen], etc. for private events.
 */
abstract class Player(val playerIndex: Int, protected val game: Game) {
    /**
     * Factory wrapper to create a player; this essentially allows a convenient reference to the player constructor or
     * to pass in some easily-configurable parameters.
     *
     * The easiest way to provide a factory is as part of a [Player]'s companion object, i.e.:
     *
     *   companion object : Factory {
     *     override fun create(playerIndex: Int, game: Game) = MyPlayer(playerIndex, game)
     *   }
     *
     * The companion object is referenced when using the class name (similar to how it would be for Java static
     * members), so this allows just doing `val factory = MyPlayer`.
     *
     * To provide some parameters to a [Factory], it can no longer be an `object`, so it may look something like:
     *
     *   class Factory(private val intelligence: Int) : Player.Factory {
     *     override fun create(playerIndex: Int, game: Game) = MyPlayer(playerIndex, game, intelligence)
     *   }
     *
     * which would then be referenced as `val factory = MyPlayer.Factory(intelligence = 0)`.
     */
    interface Factory {
        /**
         * Optionally provides a user-readable name for players created by this factory with the given [playerIndex], by
         * default extracts the simple class name.
         */
        fun playerName(playerIndex: Int): String {
            return this::class.qualifiedName.orEmpty().removeSuffix(".Companion").substringAfterLast('.')
        }

        /**
         * Creates a new [Player] for a new [game] at the given [playerIndex].
         */
        fun create(playerIndex: Int, game: Game): Player
    }

    private val _hand: MutableList<Card> = mutableListOf()

    /**
     * The current [Card]s in this [Player]'s hand.
     *
     * Cards are always added to the end of the list (including when returning them to the hand from a duel).
     * TODO place retained cards back in the hand in the same position they were originally (not trivial)
     */
    protected val hand: List<Card>
        get() = _hand.toList()

    /**
     * Counts of cards in play during a duel so that [isActive] and [handSize] can still be accurate.
     */
    protected var cardsInPlay: Int = 0
        private set

    /**
     * Whether this [Player] is still active in the game, i.e. still has cards in their hand (whether they are in their
     * hand or in-play during a duel).
     */
    val isActive: Boolean
        get() = cardsInPlay > 0 || _hand.isNotEmpty()

    /**
     * Tracks the total time spent in implementation logic.
     */
    var totalPlayerLogicTime: Duration = Duration.ZERO
        private set

    /**
     * Chooses a card from the hand to be discarded, as an index of [hand].
     *
     * If the discarded card is a spy then subsequent call(s) will be made to [spy].
     */
    protected abstract fun discard(): Int

    /**
     * Chooses a player to steal a card from as a result of [discard]ing a [Card.SPY] or [Card.SPY_MASTER], as an index
     * in [Game.players].
     *
     * The chosen player must still be active (have a non-empty hand) and cannot be this [Player]. The spied card will
     * be chosen randomly and passed to [onReceiveSpyCard].
     */
    protected abstract fun spy(): Int

    /**
     * Chooses a card from the hand to be played in a duel among players with indexes [involvedPlayers], which may be a
     * double- (or triple-) duel as indicated by [previousRounds].
     *
     * The initial round of dueling will have [involvedPlayers] just be [Game.activePlayers] and [previousRounds] be
     * empty. If there is a double duel (no counteracts and either a tie among the lowest cards or multiple traps)
     * another call to [duel] will be made. The double-duel may also be in "trapping" mode where the highest card wins
     * and takes the entire field rather than the lowest card losing if any of the previous rounds had traps, i.e.
     * [DuelRoundResult.DoubleDuel.trapping] is true.
     *
     * [duel] is not called if the player has zero cards in their hand, in which case a card will be drawn from the top
     * of the draw pile and played on the behalf of the player. [duel] is also not called if the player has only one
     * card in their hand, in which case that card will always be played.
     */
    protected abstract fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int

    /**
     * Invoked at the start of this [Player]'s turn when the draw a card from the deck, namely [card].
     */
    protected open fun onDraw(card: Card) {}

    /**
     * Invoked when another player (with player index [byPlayerIndex]) discards a spying card and steals [card] from
     * this player.
     */
    protected open fun onCardStolen(card: Card, byPlayerIndex: Int) {}

    /**
     * Invoked when this player steals [card] from another player (with player index [fromPlayerIndex]) after discarding
     * a spying card.
     */
    protected open fun onReceiveSpyCard(card: Card, fromPlayerIndex: Int) {}

    /**
     * Determines the number of cards this player has in their hand, including cards currently involved in a duel when
     * [includeCardsInPlay] is true (the default).
     */
    fun handSize(includeCardsInPlay: Boolean = true): Int {
        return if (includeCardsInPlay) _hand.size + cardsInPlay else _hand.size
    }

    /**
     * Adds [card] to this player's hand during the initial deal, with no player callbacks.
     */
    internal fun deal(card: Card) {
        _hand.add(card)
    }

    /**
     * Adds [card] to this player's hand from the draw at the beginning of their turn and calls [onDraw].
     */
    internal fun draw(card: Card) {
        _hand.add(card)
        catchingPlayerExceptions { onDraw(card) }
    }

    /**
     * Adds [card] to this player's hand after stealing it from [fromPlayerIndex] and calls [onReceiveSpyCard].
     */
    internal fun receiveSpyCard(card: Card, fromPlayerIndex: Int) {
        _hand.add(card)
        catchingPlayerExceptions { onReceiveSpyCard(card = card, fromPlayerIndex = fromPlayerIndex) }
    }

    /**
     * Removes a random card (as chosen by [random]) from this player's hand and returns the stolen [Card] and the
     * number of cards remaining in this player's hand; and invokes [onCardStolen].
     */
    internal fun stealRandomCard(byPlayerIndex: Int, random: Random = Random.Default): Pair<Card, Int> {
        val stolenCard = _hand.removeAt(index = random.nextInt(until = _hand.size))
        catchingPlayerExceptions { onCardStolen(stolenCard, byPlayerIndex) }
        return Pair(stolenCard, _hand.size)
    }

    /**
     * Chooses a card to be discarded by [discard], validates the choice, removes it from this player's hand, and
     * returns it.
     */
    internal fun doDiscard(): Card {
        val cardIndex = catchingPlayerExceptions { discard() }

        if (cardIndex !in _hand.indices) {
            throw PlayerChoiceException.InvalidDiscard(player = this, cardIndex = cardIndex)
        }

        return _hand.removeAt(cardIndex)
    }

    /**
     * Chooses a player to spy from by [spy], validates the choice, and returns it.
     */
    internal fun doSpy(): Int {
        val spiedPlayerIndex = catchingPlayerExceptions { spy() }
        if (spiedPlayerIndex == playerIndex) {
            throw PlayerChoiceException.InvalidSpy(
                player = this,
                spiedPlayerIndex = spiedPlayerIndex,
                players = game.players.size,
            )
        }

        if (spiedPlayerIndex !in game.players.indices) {
            throw PlayerChoiceException.InvalidSpy(
                player = this,
                spiedPlayerIndex = spiedPlayerIndex,
                players = game.players.size,
            )
        }

        val spiedPlayer = game.players[spiedPlayerIndex]
        if (spiedPlayer.handSize() == 0) {
            throw PlayerChoiceException.SpiedEmptyHand(player = this, spiedPlayerIndex = spiedPlayerIndex)
        }

        return spiedPlayerIndex
    }

    /**
     * Chooses a card to play in a duel by [duel] (or chooses the only card if there is only one), validates the choice,
     * removes it from this player's hand, and returns it.
     */
    internal fun doDuel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Card {
        assert(playerIndex in involvedPlayers)

        val cardIndex = if (_hand.size == 1) {
            0
        } else {
            catchingPlayerExceptions { duel(involvedPlayers, previousRounds) }
        }

        if (cardIndex !in _hand.indices) {
            throw PlayerChoiceException.InvalidDuel(player = this, cardIndex = cardIndex)
        }

        cardsInPlay++
        return _hand.removeAt(cardIndex)
    }

    /**
     * Invoked at the end of a duel to reset player state ([cardsInPlay] in particular), and adds [retainedCards] and
     * [trappedCards] to this player's hand.
     */
    internal fun postDuel(retainedCards: List<Card>?, trappedCards: List<Card>?) {
        cardsInPlay = 0
        retainedCards?.let { _hand.addAll(it) }
        trappedCards?.let { _hand.addAll(it) }
    }

    /**
     * Invokes [block], increasing [totalPlayerLogicTime] based on the time it takes and catching any exceptions thrown,
     * wrapping them in [PlayerThrownException].
     */
    @OptIn(ExperimentalTime::class)
    private fun <T> catchingPlayerExceptions(block: () -> T): T {
        val start = TimeSource.Monotonic.markNow()
        return try {
            block()
        } catch (ex: Throwable) {
            throw PlayerThrownException(player = this, cause = ex)
        } finally {
            totalPlayerLogicTime += start.elapsedNow()
        }
    }
}
