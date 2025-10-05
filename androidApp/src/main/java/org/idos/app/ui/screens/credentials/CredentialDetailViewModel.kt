package org.idos.app.ui.screens.credentials

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.idos.app.data.DataProvider
import org.idos.app.data.model.CredentialDetail
import org.idos.app.data.repository.CredentialsRepository
import org.idos.app.navigation.NavRoute
import org.idos.app.navigation.NavigationCommand
import org.idos.app.navigation.NavigationManager
import org.idos.app.ui.screens.base.BaseViewModel
import org.idos.enclave.DecryptFailure
import org.idos.enclave.Enclave
import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveOrchestrator
import org.idos.enclave.EnclaveState
import org.idos.kwil.serialization.toByteArray
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.UuidString
import timber.log.Timber
import kotlin.time.Duration

sealed class CredentialDetailState {
    data object Loading : CredentialDetailState()

    data class Loaded(
        val credential: CredentialDetail,
        val decryptedContent: JsonElement? = null, // null = still encrypted
    ) : CredentialDetailState()

    data class Error(
        val message: String,
    ) : CredentialDetailState()
}

sealed class CredentialDetailEvent {
    data object LoadCredential : CredentialDetailEvent()

    data object DecryptCredential : CredentialDetailEvent()

    data object Retry : CredentialDetailEvent()

    data object Back : CredentialDetailEvent()

    // Enclave events
    data class UnlockEnclave(
        val password: String,
        val expiration: Duration,
    ) : CredentialDetailEvent()

    data object LockEnclave : CredentialDetailEvent()

    data object DismissEnclave : CredentialDetailEvent()

    data object RetryDecrypt : CredentialDetailEvent()
}

sealed class EnclaveUiState {
    data object Hidden : EnclaveUiState()

    data object Unlocking : EnclaveUiState()

    data object RequiresUnlock : EnclaveUiState()

    data class UnlockError(
        val message: String,
        val canRetry: Boolean = true,
    ) : EnclaveUiState()
}

class CredentialDetailViewModel(
    private val credentialsRepository: CredentialsRepository,
    private val navigationManager: NavigationManager,
    private val dataProvider: DataProvider,
    private val orchestrator: EnclaveOrchestrator,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<CredentialDetailState, CredentialDetailEvent>() {
    val credentialId: UuidString =
        requireNotNull(
            savedStateHandle.get<String>(NavRoute.CredentialDetail.CREDENTIAL_ID_ARG),
        ).let { UuidString(it) }

    // Track enclave UI state (dialog, errors)
    private val _enclaveUiState = MutableStateFlow<EnclaveUiState>(EnclaveUiState.Hidden)
    val enclaveUiState = _enclaveUiState

    init {
        viewModelScope.launch {
            orchestrator.checkStatus()
        }
        loadCredential()
        observeEnclaveState()
    }

    private fun observeEnclaveState() {
        viewModelScope.launch {
            orchestrator.state.collect { state ->
                when (state) {
                    is EnclaveState.Unlocked -> {
                        _enclaveUiState.value = EnclaveUiState.Hidden
                        decryptLoadedCredential()
                    }

                    is EnclaveState.Unlocking -> {
                        _enclaveUiState.value = EnclaveUiState.Unlocking
                    }

                    is EnclaveState.Locked -> {
                        // Don't automatically show dialog - wait for decrypt attempt
                    }
                }
            }
        }
    }

    private fun loadCredential() {
        viewModelScope.launch {
            try {
                updateState { CredentialDetailState.Loading }

                credentialsRepository
                    .getCredential(credentialId)
                    .collect { detail ->
                        updateState { CredentialDetailState.Loaded(detail, decryptedContent = null) }
                        // Try to decrypt if enclave is unlocked
                        if (orchestrator.state.value is EnclaveState.Unlocked) {
                            decryptLoadedCredential()
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load credential")
                updateState { CredentialDetailState.Error("Failed to load credential: ${e.message}") }
            }
        }
    }

    private fun decryptLoadedCredential() {
        val currentState = state.value
        if (currentState !is CredentialDetailState.Loaded) return
        if (currentState.decryptedContent != null) return // Already decrypted

        viewModelScope.launch {
            orchestrator
                .withEnclave { enclave ->
                    decryptCredential(currentState.credential, enclave)
                }.mapCatching { decryptedString ->
                    val json = Json.parseToJsonElement(decryptedString)
                    updateState {
                        (this as CredentialDetailState.Loaded).copy(decryptedContent = json)
                    }
                }.onFailure { error ->
                    when (error) {
                        is EnclaveError.NoKey, is EnclaveError.KeyExpired -> {
                            Timber.i("Enclave locked, prompting for password")
                            _enclaveUiState.value = EnclaveUiState.RequiresUnlock
                        }

                        is EnclaveError.DecryptionFailed -> {
                            val message =
                                when {
                                    error.reason == DecryptFailure.WrongPassword -> "Wrong password - key cannot decrypt this data"
                                    else -> "Decryption failed: ${error.message}"
                                }
                            _enclaveUiState.value = EnclaveUiState.UnlockError(message, canRetry = true)
                        }

                        else -> {
                            Timber.e(error, "Decryption error")
                            updateState { CredentialDetailState.Error("Failed to decrypt: ${error.message}") }
                        }
                    }
                }
        }
    }

    private suspend fun decryptCredential(
        data: CredentialDetail,
        enclave: Enclave,
    ): Result<String> {
        val content = Base64String(data.content).toByteArray()
        val pubkey = Base64String(data.encryptorPublicKey).toByteArray()
        return enclave.decrypt(content, pubkey).map { it.decodeToString() }
    }

    private fun unlockEnclave(
        password: String,
        expiration: Duration,
    ) {
        viewModelScope.launch {
            dataProvider
                .getUser()
                .map {
                    orchestrator
                        .unlock(it.id, password, expiration.inWholeMilliseconds)
                        .onSuccess {
                            Timber.i("Enclave unlocked successfully")
                            // State observer will trigger decrypt
                        }.onFailure { error ->
                            Timber.e(error, "Failed to unlock enclave")
                            _enclaveUiState.value =
                                EnclaveUiState.UnlockError(
                                    error.message ?: "Failed to unlock enclave",
                                    canRetry = false,
                                )
                        }
                }
        }
    }

    private fun lockEnclave() {
        viewModelScope.launch {
            orchestrator.lock()
            updateState {
                if (this is CredentialDetailState.Loaded) {
                    copy(decryptedContent = null)
                } else {
                    this
                }
            }
        }
    }

    private fun dismissEnclave() {
        _enclaveUiState.value = EnclaveUiState.Hidden
//        viewModelScope.launch {
//            navigationManager.navigate(NavigationCommand.NavigateUp)
//        }
    }

    private fun retryDecrypt() {
        _enclaveUiState.value = EnclaveUiState.Hidden
        decryptLoadedCredential()
    }

    override fun initialState(): CredentialDetailState = CredentialDetailState.Loading

    override fun onEvent(event: CredentialDetailEvent) {
        Timber.d("***Event: $event")
        when (event) {
            is CredentialDetailEvent.LoadCredential -> loadCredential()
            is CredentialDetailEvent.DecryptCredential -> {
                // Trigger decrypt flow
                if (orchestrator.state.value is EnclaveState.Unlocked) {
                    decryptLoadedCredential()
                } else {
                    _enclaveUiState.value = EnclaveUiState.RequiresUnlock
                }
            }

            is CredentialDetailEvent.Retry -> loadCredential()
            is CredentialDetailEvent.Back -> {
                viewModelScope.launch {
                    navigationManager.navigate(NavigationCommand.NavigateUp)
                }
            }

            is CredentialDetailEvent.UnlockEnclave -> unlockEnclave(event.password, event.expiration)
            is CredentialDetailEvent.LockEnclave -> lockEnclave()
            is CredentialDetailEvent.DismissEnclave -> dismissEnclave()
            is CredentialDetailEvent.RetryDecrypt -> retryDecrypt()
        }
    }
}
