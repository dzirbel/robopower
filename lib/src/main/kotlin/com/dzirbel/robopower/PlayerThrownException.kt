package com.dzirbel.robopower

/**
 * Thrown when an exception was raised in functions implemented by [Player], usually meaning an error in its logic.
 */
class PlayerThrownException(val player: Player, cause: Throwable) : Throwable("Exception thrown by player logic", cause)
