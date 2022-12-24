package com.dzirbel.robopower

import kotlin.random.Random

// TODO document
// TODO unit tests?
abstract class Player(val playerIndex: Int, protected val game: Game) {
    fun interface Factory {
        fun create(playerIndex: Int, game: Game): Player
    }

    private val _hand: MutableList<Card> = mutableListOf()
    protected val hand: List<Card>
        get() = _hand.toList()

    private var cardsInPlay: Int = 0

    val isActive: Boolean
        get() = cardsInPlay > 0 || _hand.isNotEmpty()

    protected abstract fun discard(): Int

    protected abstract fun spy(): Int

    protected abstract fun duel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Int

    // IS called during initial deal
    protected open fun onDraw(card: Card) {}

    protected open fun onCardStolen(card: Card, byPlayerIndex: Int) {}

    protected open fun onReceiveSpyCard(card: Card, fromPlayerIndex: Int) {}

    fun handSize(includeCardsInPlay: Boolean = true): Int {
        return if (includeCardsInPlay) _hand.size + cardsInPlay else _hand.size
    }

    internal fun draw(card: Card) {
        _hand.add(card)
        catchingPlayerExceptions { onDraw(card) }
    }

    internal fun receiveSpyCard(card: Card, fromPlayerIndex: Int) {
        _hand.add(card)
        catchingPlayerExceptions { onReceiveSpyCard(card = card, fromPlayerIndex = fromPlayerIndex) }
    }

    // returns card + number of cards remaining in the hand
    internal fun stealRandomCard(byPlayerIndex: Int, random: Random = Random.Default): Pair<Card, Int> {
        val stolenCard = _hand.removeAt(index = random.nextInt(until = _hand.size))
        catchingPlayerExceptions { onCardStolen(stolenCard, byPlayerIndex) }
        return Pair(stolenCard, _hand.size)
    }

    internal fun doDiscard(): Card {
        val cardIndex = catchingPlayerExceptions { discard() }

        if (cardIndex !in _hand.indices) {
            throw PlayerChoiceException.InvalidDiscard(player = this, cardIndex = cardIndex)
        }

        return _hand.removeAt(cardIndex)
    }

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

    internal fun doDuel(involvedPlayers: Set<Int>, previousRounds: List<DuelRound>): Card {
        val cardIndex = if (_hand.size == 1) {
            0 // TODO callback for single-card case
        } else {
            catchingPlayerExceptions { duel(involvedPlayers, previousRounds) }
        }

        if (cardIndex !in _hand.indices) {
            throw PlayerChoiceException.InvalidDuel(player = this, cardIndex = cardIndex)
        }

        return _hand.removeAt(cardIndex)
            .also { cardsInPlay++ }
    }

    internal fun postDuel(retainedCards: List<Card>?, trappedCards: List<Card>?) {
        cardsInPlay = 0
        retainedCards?.let { _hand.addAll(it) }
        trappedCards?.let { _hand.addAll(it) }
    }

    private fun <T> catchingPlayerExceptions(block: () -> T): T {
        return try {
            block()
        } catch (ex: Throwable) {
            throw PlayerThrownException(player = this, cause = ex)
        }
    }
}
