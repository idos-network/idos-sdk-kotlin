package org.idos.app.ui.screens.base

import org.idos.enclave.Enclave
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

sealed class EnclaveUiState {
    data object Loading : EnclaveUiState()

    data object Close : EnclaveUiState()

    data class Available(
        val enclave: Enclave,
    ) : EnclaveUiState()

    data object RequiresKey : EnclaveUiState()

    data object Generating : EnclaveUiState()

    data class KeyGenerationError(
        val message: String,
    ) : EnclaveUiState()

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
    ) : EnclaveUiState()
}

sealed class EnclaveEvent {
    data class GenerateKey(
        val password: String,
        val expiration: Duration,
    ) : EnclaveEvent()

    data object Retry : EnclaveEvent()

    data object Dismiss : EnclaveEvent()
}

data class KeyGenerationOptions(
    val label: String,
    val duration: Duration,
) {
    companion object {
        val DEFAULT_OPTIONS =
            listOf(
                KeyGenerationOptions("1 Day", 10.seconds),
//            KeyGenerationOptions("1 Day", 1.days),
                KeyGenerationOptions("1 Week", 7.days),
                KeyGenerationOptions("1 Month", 30.days),
            )
    }
}
