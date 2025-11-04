package org.idos.app.ui.screens.mnemonic

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.idos.app.BuildConfig
import org.idos.app.data.repository.NoProfileException
import org.idos.app.data.repository.UserRepository
import org.idos.app.security.EthSigner
import org.idos.app.security.EthSigner.Companion.mnemonicToKeypair
import org.idos.app.security.EthSigner.Companion.privateToAddress
import org.idos.app.security.KeyManager
import org.idos.app.ui.screens.base.BaseViewModel
import timber.log.Timber

sealed class MnemonicEvent {
    data class UpdateMnemonic(
        val mnemonic: String,
    ) : MnemonicEvent()

    data class UpdateDerivationPath(
        val derivationPath: String,
    ) : MnemonicEvent()

    object GenerateWallet : MnemonicEvent()

    object ResetSuccess : MnemonicEvent()

    object ClearError : MnemonicEvent()
}

data class MnemonicState(
    val mnemonic: String = "",
    val derivationPath: String = EthSigner.DEFAULT_DERIVATION_PATH,
    val isGenerateButtonEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
)

class MnemonicViewModel(
    private val keyManager: KeyManager,
    private val userRepository: UserRepository,
) : BaseViewModel<MnemonicState, MnemonicEvent>() {
    override fun initialState(): MnemonicState {
        val initialMnemonic =
            if (BuildConfig.DEBUG && BuildConfig.MNEMONIC_WORDS.isNotBlank()) {
                BuildConfig.MNEMONIC_WORDS
            } else {
                ""
            }
        return MnemonicState(
            mnemonic = initialMnemonic,
            isGenerateButtonEnabled = initialMnemonic.isNotBlank(),
        )
    }

    override fun onEvent(event: MnemonicEvent) {
        Timber.d("Event: $event")
        when (event) {
            is MnemonicEvent.UpdateMnemonic -> updateMnemonic(event.mnemonic)
            is MnemonicEvent.UpdateDerivationPath -> updateDerivationPath(event.derivationPath)
            MnemonicEvent.GenerateWallet -> generateWallet()
            MnemonicEvent.ResetSuccess -> resetSuccess()
            MnemonicEvent.ClearError -> updateState { copy(error = null) }
        }
    }

    private fun updateMnemonic(mnemonic: String) {
        updateState {
            copy(
                mnemonic = mnemonic,
                isGenerateButtonEnabled = mnemonic.trim().isNotBlank(),
            )
        }
    }

    private fun updateDerivationPath(derivationPath: String) {
        updateState { copy(derivationPath = derivationPath) }
    }

    private fun generateWallet() {
        viewModelScope.launch {
            try {
                updateState { copy(isLoading = true, error = null) }

                // Generate mnemonic and derive key pair
                val mnemonic = currentState.mnemonic.trim()
                val derivationPath = currentState.derivationPath

                // Store key in KeyManager with custom derivation path
                keyManager.generateAndStoreKey(mnemonic, derivationPath)
                Timber.d("Generated wallet with derivation path: $derivationPath")

                updateState {
                    copy(
                        isLoading = false,
                        isSuccess = true,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate wallet")
                updateState {
                    copy(
                        isLoading = false,
                        error = e.message ?: "Failed to generate wallet",
                    )
                }
            }
        }
    }

    private fun resetSuccess() {
        viewModelScope.launch {
            try {
                // Keep dialog open and show loading
                updateState { copy(isLoading = true, error = null) }

                // Fetch user profile - this can throw NoProfileException
                userRepository.fetchAndStoreUser()

                // Success - dialog will auto-dismiss when UserRepository navigates
            } catch (e: NoProfileException) {
                val address =
                    keyManager.getStoredKey()?.privateToAddress()
                        ?: "No address found"

                Timber.w(e, "User profile '$address' does not exist")
                updateState {
                    copy(
                        isLoading = false,
                        isSuccess = false,
                        error = "Profile for address: '$address' not found. Please create your profile first.",
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch user profile")
                updateState {
                    copy(
                        isLoading = false,
                        isSuccess = false,
                        error = "Failed to load profile: ${e.message}",
                    )
                }
            }
        }
    }
}
