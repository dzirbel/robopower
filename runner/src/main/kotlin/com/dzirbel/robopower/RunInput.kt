package com.dzirbel.robopower

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY

@OptIn(FlowPreview::class)
data class RunInput(
    val games: Int,
    val players: List<Player.Factory>,
    val randomizeOrder: Boolean = true,
    val concurrency: Int = DEFAULT_CONCURRENCY,
    val verbose: Boolean = false,
)
