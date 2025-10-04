package org.idos.app.ui.screens.credentials

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.idos.app.data.DataProvider
import org.idos.app.data.model.CredentialDetail
import org.idos.app.data.repository.CredentialsRepository
import org.idos.app.navigation.NavRoute
import org.idos.app.navigation.NavigationCommand
import org.idos.app.navigation.NavigationManager
import org.idos.app.ui.screens.base.BaseEnclaveViewModel
import org.idos.enclave.Enclave
import org.idos.enclave.KeyExpiredError
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.UuidString
import org.idos.kwil.serialization.toByteArray
import timber.log.Timber

sealed class CredentialDetailState {
    data object Loading : CredentialDetailState()

    data class Success(
        val credential: CredentialDetail,
        val decryptedContent: JsonElement,
    ) : CredentialDetailState()

    data class Error(
        val message: String,
    ) : CredentialDetailState()
}

sealed class CredentialDetailEvent {
    data object LoadCredential : CredentialDetailEvent()

    data object Retry : CredentialDetailEvent()

    data object ResetKey : CredentialDetailEvent()

    data object CopyToClipboard : CredentialDetailEvent()

    data object ShareCredential : CredentialDetailEvent()

    data object Back : CredentialDetailEvent()
}

class CredentialDetailViewModel(
    private val credentialsRepository: CredentialsRepository,
    private val navigationManager: NavigationManager,
    dataProvider: DataProvider,
    enclave: Enclave,
    savedStateHandle: SavedStateHandle,
) : BaseEnclaveViewModel<CredentialDetailState, CredentialDetailEvent>(enclave, dataProvider, navigationManager) {
    val credentialId: UuidString =
        requireNotNull(
            savedStateHandle.get<String>(NavRoute.CredentialDetail.CREDENTIAL_ID_ARG),
        ).let { UuidString(it) }

    init {
        loadCredential()
    }

    private fun loadCredential() {
        requireEnclave { enclave ->
            Timber.d("action triggered")
            try {
                updateState { CredentialDetailState.Loading }

                credentialsRepository
                    .getCredential(credentialId)
                    .collect { detail ->
                        val decryptedContent = decryptCredential(detail, enclave)
                        val json = Json.parseToJsonElement(decryptedContent)
                        updateState { CredentialDetailState.Success(detail, json) }
                    }
            } catch (e: KeyExpiredError) {
            } catch (e: Exception) {
                Timber.e(e, "Failed to load credential")
                updateState { CredentialDetailState.Error("Failed to load credential: ${e.message}") }
            }
        }
    }

    suspend fun decryptCredential(
        data: CredentialDetail,
        enclave: Enclave,
    ): String {
        val content = Base64String(data.content).toByteArray()
        val pubkey = Base64String(data.encryptorPublicKey).toByteArray()
        val raw = enclave.decrypt(content, pubkey)
        return raw.decodeToString()
    }

    override fun initialState(): CredentialDetailState = CredentialDetailState.Loading

    override fun onEvent(event: CredentialDetailEvent) {
        when (event) {
            is CredentialDetailEvent.LoadCredential -> loadCredential()
            is CredentialDetailEvent.Retry -> loadCredential()
            is CredentialDetailEvent.CopyToClipboard -> handleCopyToClipboard()
            is CredentialDetailEvent.ShareCredential -> handleShareCredential()
            is CredentialDetailEvent.Back -> onBackClick()
            is CredentialDetailEvent.ResetKey -> deleteKey()
        }
    }

    private fun handleCopyToClipboard() {
        val currentState = state.value
        if (currentState is CredentialDetailState.Success) {
            // TODO: Implement clipboard functionality
            // context.copyToClipboard(currentState.decryptedContent)
        }
    }

    private fun handleShareCredential() {
        val currentState = state.value
        if (currentState is CredentialDetailState.Success) {
            // TODO: Implement share functionality
            // context.shareText(currentState.decryptedContent)
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand.NavigateUp)
        }
    }
}
