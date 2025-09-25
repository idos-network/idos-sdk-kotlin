package org.idos.app.ui.screens.credentials

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.idos.app.data.model.Credential
import org.idos.app.data.repository.CredentialsRepository
import org.idos.app.ui.screens.base.BaseViewModel
import timber.log.Timber

// Events that can be triggered from the UI
sealed class CredentialsEvent {
    object LoadCredentials : CredentialsEvent()
    data class CredentialSelected(val credential: Credential) : CredentialsEvent()
    object ClearError : CredentialsEvent()
}

// UI State for the Credentials screen
data class CredentialsUiState(
    val isLoading: Boolean = false,
    val credentials: List<Credential> = emptyList(),
    val error: String? = null,
)

class CredentialsViewModel(
    private val credentialsRepository: CredentialsRepository
) : BaseViewModel<CredentialsUiState, CredentialsEvent>() {

    override fun initialState(): CredentialsUiState = CredentialsUiState()

    override fun onEvent(event: CredentialsEvent) {
        when (event) {
            is CredentialsEvent.LoadCredentials -> loadCredentials()
            is CredentialsEvent.CredentialSelected -> onCredentialSelected(event.credential)
            is CredentialsEvent.ClearError -> clearError()
        }
    }

    private fun loadCredentials() {
        viewModelScope.launch {
            try {
                updateState { copy(isLoading = true, error = null) }
                credentialsRepository.getCredentials().collect { credentials ->
                    Timber.d("got credentials {$credentials}")
                    updateState { copy(credentials = credentials, isLoading = false) }
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        error = "Failed to load credentials: ${e.message}"
                    )
                }
            }
        }
    }

    private fun onCredentialSelected(credential: Credential) {
        // Handle credential selection
        // This could navigate to a detail screen or perform an action
    }

    private fun clearError() {
        updateState { copy(error = null) }
    }
}
