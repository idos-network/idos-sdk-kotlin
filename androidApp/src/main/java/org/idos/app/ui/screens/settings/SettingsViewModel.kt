package org.idos.app.ui.screens.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.idos.app.ui.screens.base.BaseViewModel
import org.idos.app.ui.screens.credentials.EnclaveUiState
import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.EnclaveOrchestrator
import org.idos.enclave.EnclaveState
import org.idos.enclave.KeyMetadata
import org.idos.enclave.MetadataStorage
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class SettingsEvent {
    object CheckKeyStatus : SettingsEvent()

    object OnEncryptionStatusClick : SettingsEvent()

    object DeleteEncryptionKey : SettingsEvent()

    object ConfirmDelete : SettingsEvent()

    object CancelDelete : SettingsEvent()

    object ClearError : SettingsEvent()

    object DismissSnackbar : SettingsEvent()
}

sealed class EnclaveUiStatus {
    data class Unlocked(
        val metadata: KeyMetadata,
        val formattedExpiration: String,
    ) : EnclaveUiStatus()

    data class Locked(
        val metadata: KeyMetadata?,
        val formattedExpiration: String?,
    ) : EnclaveUiStatus()

    object NotAvailable : EnclaveUiStatus()

    object Unlocking : EnclaveUiStatus()
}

data class SettingsState(
    val hasEncryptionKey: Boolean = false,
    val enclaveStatus: EnclaveUiStatus = EnclaveUiStatus.NotAvailable,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val snackbarMessage: String? = null,
    val error: String? = null,
)

class SettingsViewModel(
    val enclaveOrchestrator: EnclaveOrchestrator,
    private val metadataStorage: MetadataStorage,
) : BaseViewModel<SettingsState, SettingsEvent>() {
    init {
        observeEnclaveState()
    }

    override fun initialState(): SettingsState = SettingsState()

    override fun onEvent(event: SettingsEvent) {
        Timber.d("Event: $event")
        when (event) {
            SettingsEvent.CheckKeyStatus -> checkKeyStatus()
            SettingsEvent.OnEncryptionStatusClick -> onEncryptionStatusClick()
            SettingsEvent.DeleteEncryptionKey -> showDeleteConfirmation()
            SettingsEvent.ConfirmDelete -> confirmDelete()
            SettingsEvent.ClearError -> clearError()
            SettingsEvent.CancelDelete -> cancelDelete()
            SettingsEvent.DismissSnackbar -> dismissSnackbar()
        }
    }

    private fun observeEnclaveState() {
        viewModelScope.launch {
            enclaveOrchestrator.state.collect { state ->
                when (state) {
                    is EnclaveState.Unlocked -> {
                        val metadata = fetchMetadata()
                        val status =
                            metadata?.let {
                                EnclaveUiStatus.Unlocked(it, formatExpiration(it))
                            } ?: EnclaveUiStatus.NotAvailable
                        updateState { copy(hasEncryptionKey = true, enclaveStatus = status) }
                    }

                    is EnclaveState.Unlocking -> {
                        updateState { copy(enclaveStatus = EnclaveUiStatus.Unlocking) }
                    }

                    is EnclaveState.Locked -> {
                        val metadata = fetchMetadata()
                        val status = EnclaveUiStatus.Locked(metadata, metadata?.let { formatExpiration(it) })
                        updateState { copy(hasEncryptionKey = false, enclaveStatus = status) }
                    }

                    is EnclaveState.NotAvailable -> {
                        updateState {
                            copy(
                                hasEncryptionKey = false,
                                enclaveStatus = EnclaveUiStatus.NotAvailable,
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchMetadata(): KeyMetadata? =
        try {
            // Try USER type first, then MPC
            metadataStorage.get(EnclaveKeyType.USER) ?: metadataStorage.get(EnclaveKeyType.MPC)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch metadata")
            null
        }

    private fun formatExpiration(metadata: KeyMetadata): String {
        val expiresAtValue = metadata.expiresAt
        return when {
            expiresAtValue != null -> {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                "Expires: ${dateFormat.format(Date(expiresAtValue))}"
            }

            metadata.expirationType == org.idos.enclave.ExpirationType.SESSION -> {
                "Expires: End of session"
            }

            metadata.expirationType == org.idos.enclave.ExpirationType.ONE_SHOT -> {
                "Expires: After first use"
            }

            else -> "No expiration"
        }
    }

    private fun onEncryptionStatusClick() {
        viewModelScope.launch {
            val message =
                when (val status = currentState.enclaveStatus) {
                    is EnclaveUiStatus.Unlocked -> buildMetadataMessage(status.metadata, "UNLOCKED")
                    is EnclaveUiStatus.Locked -> {
                        status.metadata?.let { buildMetadataMessage(it, "LOCKED") }
                            ?: "Enclave is locked\nNo metadata available (key may have expired)"
                    }
                    EnclaveUiStatus.NotAvailable -> "Enclave not available\nNo encryption key has been generated"
                    EnclaveUiStatus.Unlocking -> "Enclave is unlocking..."
                }
            updateState { copy(snackbarMessage = message) }
        }
    }

    private fun buildMetadataMessage(
        metadata: KeyMetadata,
        statusLabel: String,
    ): String =
        """
        Status: $statusLabel
        Type: ${metadata.type}
        Expiration Type: ${metadata.expirationType}
        ${formatExpiration(metadata)}
        Created: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(metadata.createdAt))}
        Last Used: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(metadata.lastUsedAt))}
        Public Key: ${metadata.publicKey.toString().take(16)}...
        """.trimIndent()

    private fun dismissSnackbar() {
        updateState { copy(snackbarMessage = null) }
    }

    private fun checkKeyStatus() {
        viewModelScope.launch {
            enclaveOrchestrator.checkStatus()
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
