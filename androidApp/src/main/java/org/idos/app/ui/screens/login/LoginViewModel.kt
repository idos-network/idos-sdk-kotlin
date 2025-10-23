package org.idos.app.ui.screens.login

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.idos.app.data.ConnectedUser
import org.idos.app.data.ConnectedWallet
import org.idos.app.data.LoadingUser
import org.idos.app.data.NoUser
import org.idos.app.data.UserError
import org.idos.app.data.repository.UserRepository
import org.idos.app.navigation.NavigationManager
import org.idos.app.navigation.Screen
import org.idos.app.ui.screens.base.BaseViewModel
import timber.log.Timber

data class LoginUiState(
    val showConnectButton: Boolean = false,
    val isConnectedUser: Boolean = false,
    val error: String? = null,
)

sealed class LoginEvent {
    object ConnectWallet : LoginEvent()
}

class LoginViewModel(
    private val userRepository: UserRepository,
    private val navigationManager: NavigationManager,
) : BaseViewModel<LoginUiState, LoginEvent>() {
    init {
        monitorUserState()
    }

    private fun monitorUserState() {
        viewModelScope.launch {
            userRepository.userState.collect { userState ->
                Timber.d("LoginViewModel user state changed: $userState")

                when (userState) {
                    is LoadingUser -> {
                        // Ignore loading state - splash screen handles this
                    }

                    is NoUser -> {
                        updateState {
                            copy(
                                showConnectButton = true,
                                isConnectedUser = false,
                                error = null,
                            )
                        }
                    }

                    is ConnectedWallet -> {
                        // User is connecting - mnemonic screen will handle this
                        // Keep splash visible, no login content needed
                    }

                    is ConnectedUser -> {
                        updateState {
                            copy(
                                showConnectButton = false,
                                isConnectedUser = true,
                                error = null,
                            )
                        }
                    }

                    is UserError -> {
                        updateState {
                            copy(
                                showConnectButton = true,
                                isConnectedUser = false,
                                error = userState.message,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun initialState(): LoginUiState = LoginUiState()

    override fun onEvent(event: LoginEvent) {
        when (event) {
            LoginEvent.ConnectWallet -> connectWallet()
        }
    }

    private fun connectWallet() {
        viewModelScope.launch {
            try {
                // Navigate to mnemonic screen for wallet import/generation
                navigationManager.navigateTo(Screen.Mnemonic)
            } catch (e: Exception) {
                Timber.e(e, "Failed to initiate wallet connection")
                updateState {
                    copy(error = "Failed to connect wallet: ${e.message}")
                }
            }
        }
    }
}
