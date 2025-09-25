package org.idos.app.ui.screens.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.idos.app.ui.screens.base.BaseViewModel

sealed class SettingsEvent {
    data class UpdatePassword(val password: String) : SettingsEvent()
    data class ToggleBiometrics(val enabled: Boolean) : SettingsEvent()
    object SaveSettings : SettingsEvent()
    object ResetSaveStatus : SettingsEvent()
}

data class SettingsState(
    val password: String = "",
    val biometricsEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isSaveSuccessful: Boolean = false
)

class SettingsViewModel : BaseViewModel<SettingsState, SettingsEvent>() {

    override fun initialState(): SettingsState = SettingsState()

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdatePassword -> updatePassword(event.password)
            is SettingsEvent.ToggleBiometrics -> toggleBiometrics(event.enabled)
            SettingsEvent.SaveSettings -> saveSettings()
            SettingsEvent.ResetSaveStatus -> resetSaveStatus()
        }
    }

    private fun updatePassword(password: String) {
        updateState { copy(password = password) }
    }

    private fun toggleBiometrics(enabled: Boolean) {
        updateState { copy(biometricsEnabled = enabled) }
    }

    private fun saveSettings() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            // Simulate network/database operation
            delay(1000)
            updateState {
                copy(
                    isLoading = false,
                    isSaveSuccessful = true
                )
            }
        }
    }

    private fun resetSaveStatus() {
        updateState { copy(isSaveSuccessful = false) }
    }
}
