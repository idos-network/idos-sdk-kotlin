package org.idos.app.ui.screens.base

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

data class KeyGenerationOptions(
    val label: String,
    val duration: Duration,
) {
    companion object {
        val DEFAULT_OPTIONS =
            listOf(
                KeyGenerationOptions("30s", 30.seconds),
//            KeyGenerationOptions("1 Day", 1.days),
                KeyGenerationOptions("1 Week", 7.days),
                KeyGenerationOptions("1 Month", 30.days),
            )
    }
}
