package org.idos.app.ui.screens.login

import androidx.lifecycle.viewModelScope
import com.reown.foundation.network.ConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.idos.app.data.ConnectedUser
import org.idos.app.data.ConnectedWallet
import org.idos.app.data.LoadingUser
import org.idos.app.data.NoUser
import org.idos.app.data.UserError
import org.idos.app.data.model.WalletType
import org.idos.app.data.repository.UserRepository
import org.idos.app.navigation.NavigationManager
import org.idos.app.navigation.Screen
import org.idos.app.security.UnifiedSigner
import org.idos.app.security.external.ReownDelegate
import org.idos.app.security.external.ReownWalletManager
import org.idos.app.ui.screens.base.BaseViewModel
import com.reown.appkit.client.Modal
import timber.log.Timber

data class LoginUiState(
    val showConnectButton: Boolean = false,
    val isConnectedUser: Boolean = false,
    val error: String? = null,
)

sealed class LoginEvent {
    object ImportMnemonic : LoginEvent()
}

class LoginViewModel(
    private val userRepository: UserRepository,
    private val navigationManager: NavigationManager,
    private val unifiedSigner: UnifiedSigner,
) : BaseViewModel<LoginUiState, LoginEvent>() {
    init {
        monitorUserState()
        monitorWalletConnection()
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

    private fun monitorWalletConnection() {
        viewModelScope.launch {
            ReownDelegate.appKitEvents.collect { event ->
                Timber.d("AppKit event: $event")
                // When session is approved, fetch user profile
                when (event) {
                    is Modal.Model.ApprovedSession -> {
                        handleWalletConnected()
                    }
                    else -> {
                        // Ignore other events
                    }
                }
            }
        }
    }

    override fun onEvent(event: LoginEvent) {
        when (event) {
            LoginEvent.ImportMnemonic -> importMnemonic()
        }
    }

    private fun importMnemonic() {
        viewModelScope.launch {
            try {
                // Navigate to mnemonic screen for wallet import/generation
                navigationManager.navigateTo(Screen.Mnemonic)
            } catch (e: Exception) {
                Timber.e(e, "Failed to initiate mnemonic import")
                updateState {
                    copy(error = "Failed to import mnemonic: ${e.message}")
                }
            }
        }
    }

    private fun handleWalletConnected() {
        viewModelScope.launch {
            try {
                // Activate remote signer
                unifiedSigner.activateRemoteSigner()

                Timber.d("Wallet connected")

                userRepository.fetchAndStoreUser(WalletType.REMOTE)
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle wallet connection")
                updateState {
                    copy(error = "Failed to connect: ${e.message}")
                }
            }
        }
    }
}
