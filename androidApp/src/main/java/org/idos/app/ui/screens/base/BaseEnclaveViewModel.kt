package org.idos.app.ui.screens.base

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.idos.app.data.DataProvider
import org.idos.app.navigation.NavigationCommand
import org.idos.app.navigation.NavigationManager
import org.idos.enclave.*
import timber.log.Timber
import kotlin.random.Random
import kotlin.time.Duration

abstract class BaseEnclaveViewModel<State : Any, Event>(
    private val enclave: Enclave,
    private val dataProvider: DataProvider,
    private val navigationManager: NavigationManager,
) : BaseViewModel<State, Event>() {
    private val _enclaveUiState = MutableStateFlow<EnclaveUiState>(EnclaveUiState.Loading)
    val enclaveUiState: StateFlow<EnclaveUiState> = _enclaveUiState.asStateFlow()
    var enclaveJob: Job? = null

    /**
     * Main method for ViewModels to use when they need an enclave.
     * This will check enclave state and only execute action if available.
     */
    protected fun requireEnclave(action: suspend (Enclave) -> Unit) {
        enclaveJob?.cancel()
        // on first call, check if enclave is available to trigger flow
        checkEnclaveAvailability()

        Timber.d("requireEnclave")
        enclaveJob =
            viewModelScope.launch {
                _enclaveUiState.filterIsInstance(EnclaveUiState.Available::class).collect {
                    Timber.d("Enclave available, performing action")
                    action(it.enclave)
                }
            }
    }

    /**
     * Handle key generation events from the UI
     */
    fun onEnclaveEvent(event: EnclaveEvent) {
        when (event) {
            is EnclaveEvent.GenerateKey -> generateKey(event.password, event.expiration)
            is EnclaveEvent.Retry -> checkEnclaveAvailability()
            is EnclaveEvent.Dismiss -> onKeyGenerationDismissed()
        }
    }

    fun deleteKey() {
        viewModelScope.launch {
            try {
                enclave.deleteKey()
                _enclaveUiState.value = EnclaveUiState.RequiresKey
            } catch (e: Exception) {
            }
        }
    }

    protected fun checkEnclaveAvailability() {
        Timber.d("checkEnclaveAvailability")
        _enclaveUiState.value = EnclaveUiState.Loading
        viewModelScope.launch {
            try {
//                require(Random.nextBoolean()) { "test" }
                // Check if enclave has a valid key without performing encryption
                enclave.hasValidKey()

                Timber.d("checkEnclaveAvailability available")
                _enclaveUiState.value = EnclaveUiState.Available(enclave)
            } catch (e: NoKeyError) {
                Timber.d("No key present, requiring key generation")
                _enclaveUiState.value = EnclaveUiState.RequiresKey
            } catch (e: KeyExpiredError) {
                Timber.d("Key expired, requiring key generation")
                _enclaveUiState.value = EnclaveUiState.RequiresKey
            } catch (e: Exception) {
                Timber.e(e, "Error checking enclave availability")
                _enclaveUiState.value = EnclaveUiState.Error("Failed to access encryption: ${e.message}")
            }
        }
    }

    private fun generateKey(
        password: String,
        expiration: Duration,
    ) {
        Timber.d("generateKey")
        _enclaveUiState.value = EnclaveUiState.Generating

        viewModelScope.launch {
            try {
                val user = dataProvider.getUser()
                enclave.generateKey(user.id, password, expiration.inWholeMilliseconds)
                Timber.d("Key generated successfully")
                _enclaveUiState.value = EnclaveUiState.Available(enclave)
//                onEnclaveAvailable(enclave)
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate key")
                _enclaveUiState.value = EnclaveUiState.KeyGenerationError("Failed to generate key: ${e.message}")
            }
        }
    }

    private fun onEnclaveError(message: String) {
        _enclaveUiState.value = EnclaveUiState.Error(message)
    }

    /**
     * Called when user dismisses key generation dialog
     * Override this in child ViewModels to handle the dismissal appropriately
     */
    protected open fun onKeyGenerationDismissed() {
        viewModelScope.launch {
            val err = EnclaveUiState.Error("Encryption key is required to continue", canRetry = true)
            if (_enclaveUiState.value != err) {
                _enclaveUiState.value = err
            } else {
                _enclaveUiState.value = EnclaveUiState.Loading
                navigationManager.navigate(NavigationCommand.NavigateUp)
            }
        }
    }
}
