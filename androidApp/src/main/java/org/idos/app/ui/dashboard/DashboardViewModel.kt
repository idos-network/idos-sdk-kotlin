package org.idos.app.ui.dashboard

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.idos.app.data.ConnectedUser
import org.idos.app.data.repository.UserRepository
import org.idos.app.ui.screens.base.BaseViewModel
import timber.log.Timber

data class DashboardUiState(
    val ethAddress: String = "",
    val error: String? = null,
)

sealed class DashboardEvent

class DashboardViewModel(
    private val userRepository: UserRepository,
) : BaseViewModel<DashboardUiState, DashboardEvent>() {
    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                // Fetch user data once since we know user is authenticated
                val userState = userRepository.userState.value
                if (userState is ConnectedUser) {
                    updateState {
                        copy(
                            ethAddress = userState.user.walletAddress.prefixedValue,
                            error = null,
                        )
                    }
                } else {
                    // This shouldn't happen since MainActivity checks authentication
                    Timber.w("IdosAppViewModel loaded but user is not connected: $userState")
                    updateState {
                        copy(error = "User not authenticated")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load user data")
                updateState {
                    copy(error = "Failed to load user data: ${e.message}")
                }
            }
        }
    }

    fun disconnectWallet() {
        viewModelScope.launch {
            try {
                userRepository.clearUserProfile()
                // MainActivity will handle navigation back to login
            } catch (e: Exception) {
                Timber.e(e, "Failed to disconnect wallet")
                updateState {
                    copy(error = "Failed to disconnect wallet: ${e.message}")
                }
            }
        }
    }

    override fun onEvent(event: DashboardEvent) {
        // Handle events if needed
    }

    override fun initialState(): DashboardUiState = DashboardUiState()
}
