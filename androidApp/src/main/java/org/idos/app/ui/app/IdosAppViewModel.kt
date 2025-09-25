package org.idos.app.ui.app

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.delayFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import org.idos.app.security.ConnectedAddress
import org.idos.app.security.KeyManager
import org.idos.app.security.LoadingAddress
import org.idos.app.ui.screens.base.BaseViewModel
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toECKeyPair
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

data class IdosAppUiState(
    val ethAddress: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val isConnected: Boolean
        get() = ethAddress.isNotBlank()
}

sealed class IdosAppEvent

class IdosAppViewModel(
    private val keyManager: KeyManager
) : BaseViewModel<IdosAppUiState, IdosAppEvent>() {

    init {
        loadWalletAddress()
    }

    private var addressJob: Job? = null

    private fun loadWalletAddress() {
        // Cancel any existing collection
        addressJob?.cancel()

        addressJob = viewModelScope.launch {
            try {
                keyManager.address.collect { address ->
                    Timber.d("Updating address in IdosAppViewModel: $address")
                    updateState {
                        copy(
                            ethAddress = (address as? ConnectedAddress)?.address ?: "",
                            isLoading = address is LoadingAddress,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error collecting address updates")
                updateState {
                    copy(
                        isLoading = false,
                        error = "Failed to load wallet address: ${e.message}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        addressJob?.cancel()
    }

    fun disconnectWallet() {
        viewModelScope.launch {
            try {
                keyManager.clearStoredKeys()
                updateState { copy(ethAddress = "") }
            } catch (e: Exception) {
                Timber.e(e, "Failed to disconnect wallet")
                updateState {
                    copy(
                        error = "Failed to disconnect wallet: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    override fun onEvent(event: IdosAppEvent) {
        // Handle events if needed
    }

    override fun initialState(): IdosAppUiState = IdosAppUiState()
}
