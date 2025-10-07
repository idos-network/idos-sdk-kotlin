package org.idos.app.ui.screens.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.idos.app.ui.screens.base.BaseViewModel
import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveOrchestrator

sealed class SettingsEvent {
    object CheckKeyStatus : SettingsEvent()

    object DeleteEncryptionKey : SettingsEvent()

    object ConfirmDelete : SettingsEvent()

    object CancelDelete : SettingsEvent()

    object ClearError : SettingsEvent()
}

data class SettingsState(
    val hasEncryptionKey: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val error: String? = null,
)

class SettingsViewModel(
    val enclaveOrchestrator: EnclaveOrchestrator,
) : BaseViewModel<SettingsState, SettingsEvent>() {
    override fun initialState(): SettingsState = SettingsState()

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.CheckKeyStatus -> checkKeyStatus()
            SettingsEvent.DeleteEncryptionKey -> showDeleteConfirmation()
            SettingsEvent.ConfirmDelete -> confirmDelete()
            SettingsEvent.ClearError -> clearError()
            SettingsEvent.CancelDelete -> cancelDelete()
        }
    }

    private fun checkKeyStatus() {
        viewModelScope.launch {
            try {
                enclaveOrchestrator.checkStatus()
                updateState { copy(hasEncryptionKey = true) }
            } catch (e: Exception) {
                when (e) {
                    is EnclaveError.NoKey, is EnclaveError.KeyExpired -> {
                        updateState { copy(hasEncryptionKey = true) }
                    }

                    else -> updateState { copy(error = "Failed to check key status: ${e.message}") }
                }
            }
        }
    }

    private fun showDeleteConfirmation() {
        updateState { copy(showDeleteConfirmation = true) }
    }

    private fun cancelDelete() {
        updateState { copy(showDeleteConfirmation = false) }
    }

    private fun confirmDelete() {
        viewModelScope.launch {
            try {
                updateState { copy(isDeleting = true) }
                enclaveOrchestrator.lock()
                updateState {
                    copy(
                        isDeleting = false,
                        showDeleteConfirmation = false,
                        hasEncryptionKey = false,
                    )
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isDeleting = false,
                        error = "Failed to delete encryption key: ${e.message}",
                    )
                }
            }
        }
    }

    private fun clearError() {
        updateState { copy(error = null) }
    }
}
