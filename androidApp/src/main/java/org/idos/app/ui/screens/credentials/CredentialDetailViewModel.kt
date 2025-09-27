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
import org.idos.app.ui.screens.base.BaseViewModel
import org.idos.enclave.AndroidEncryption
import org.idos.enclave.Enclave
import org.idos.kwil.rpc.Base64String
import org.idos.kwil.rpc.UuidString
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

    data object CopyToClipboard : CredentialDetailEvent()

    data object ShareCredential : CredentialDetailEvent()

    data object Back : CredentialDetailEvent()
}

class CredentialDetailViewModel(
    private val credentialsRepository: CredentialsRepository,
    private val navigationManager: NavigationManager,
    private val dataProvider: DataProvider,
    private val encryption: AndroidEncryption,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<CredentialDetailState, CredentialDetailEvent>() {
    private val credentialId: UuidString =
        requireNotNull(
            savedStateHandle.get<String>(NavRoute.CredentialDetail.CREDENTIAL_ID_ARG),
        ).let { UuidString(it) }

    init {
        loadCredential(credentialId)
    }

    private fun loadCredential(credentialId: UuidString) {
        viewModelScope.launch {
            try {
//                updateState { CredentialDetailState.Loading }

                // Fetch credential details
                val credential =
                    credentialsRepository
                        .getCredential(credentialId)
                        .collect { detail ->
                            // In a real app, you would decrypt the content here
                            val decryptedContent = decryptCredential(detail)
                            val json = Json.parseToJsonElement(decryptedContent)
                            updateState { CredentialDetailState.Success(detail, json) }
                        }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load credential")
                updateState { CredentialDetailState.Error("Failed to load credential: ${e.message}") }
            }
        }
    }

    private suspend fun decryptCredential(data: CredentialDetail): String {
        val user = dataProvider.getUser()
        val enclave = Enclave(encryption, user.id, "heslo")
        val content = Base64String(data.content).toByteArray()
        val pubkey = Base64String(data.encryptorPublicKey).toByteArray()
        val raw = enclave.decrypt(content, pubkey)
        return raw.decodeToString()
    }

    override fun initialState(): CredentialDetailState = CredentialDetailState.Loading

    override fun onEvent(event: CredentialDetailEvent) {
        when (event) {
            is CredentialDetailEvent.LoadCredential -> loadCredential(credentialId)
            is CredentialDetailEvent.Retry -> loadCredential(credentialId)
            is CredentialDetailEvent.CopyToClipboard -> handleCopyToClipboard()
            is CredentialDetailEvent.ShareCredential -> handleShareCredential()
            is CredentialDetailEvent.Back -> onBackClick()
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
