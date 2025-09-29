package org.idos.app.ui.screens.mnemonic

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.idos.app.BuildConfig
import org.idos.app.data.repository.NoProfileException
import org.idos.app.data.repository.UserRepository
import org.idos.app.security.EthSigner.Companion.mnemonicToKeypair
import org.idos.app.security.KeyManager
import org.idos.app.ui.screens.base.BaseViewModel
import timber.log.Timber

sealed class MnemonicEvent {
    data class UpdateWordCount(
        val count: Int,
    ) : MnemonicEvent()

    data class UpdateWord(
        val index: Int,
        val word: String,
    ) : MnemonicEvent()

    data class UpdatePassphrase(
        val passphrase: String,
    ) : MnemonicEvent()

    object GenerateWallet : MnemonicEvent()

    object ResetSuccess : MnemonicEvent()
}

data class MnemonicState(
    val wordCount: Int = 12,
    val words: List<String> = List(12) { "" },
    val passphrase: String = "",
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
        val initialWords =
            if (BuildConfig.DEBUG && BuildConfig.MNEMONIC_WORDS.isNotBlank()) {
                BuildConfig.MNEMONIC_WORDS.split(" ")
            } else {
                List(12) { "" }
            }
        return MnemonicState(
            wordCount = initialWords.count(),
            words = initialWords,
            isGenerateButtonEnabled = initialWords.all { it.isNotBlank() },
        )
    }

    override fun onEvent(event: MnemonicEvent) {
        Timber.d("Event: $event")
        when (event) {
            is MnemonicEvent.UpdateWordCount -> updateWordCount(event.count)
            is MnemonicEvent.UpdateWord -> updateWord(event.index, event.word)
            is MnemonicEvent.UpdatePassphrase -> updatePassphrase(event.passphrase)
            MnemonicEvent.GenerateWallet -> generateWallet()
            MnemonicEvent.ResetSuccess -> resetSuccess()
        }
    }

    private fun updateWordCount(count: Int) {
        updateState {
            copy(
                wordCount = count,
                words = List(count) { "" },
                isGenerateButtonEnabled = false,
            )
        }
    }

    private fun updateWord(
        index: Int,
        word: String,
    ) {
        val newWords = currentState.words.toMutableList()
        newWords[index] = word.trim()
        updateState {
            copy(
                words = newWords,
                isGenerateButtonEnabled = newWords.all { it.isNotBlank() },
            )
        }
    }

    private fun updatePassphrase(passphrase: String) {
        updateState { copy(passphrase = passphrase) }
    }

    private fun generateWallet() {
        viewModelScope.launch {
            try {
                updateState { copy(isLoading = true, error = null) }

                // Step 1: Generate mnemonic and derive key pair
                val words = currentState.words.joinToString(" ")
                val key = words.mnemonicToKeypair()

                // Step 2: Store key in KeyManager
                keyManager.generateAndStoreKey(key)
                key.fill(0) // Clear key from memory
                Timber.d("Generated wallet")

                // Step 3: Use UserRepository to fetch profile and store combined data

                updateState {
                    copy(
                        isLoading = false,
                        isSuccess = true,
                    )
                }
            } catch (e: NoProfileException) {
                Timber.w(e, "User profile does not exist")
                updateState {
                    copy(
                        isLoading = false,
                        error = "Profile not found. Please create your profile first.",
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
            updateState { copy(isSuccess = false) }
            userRepository.fetchAndStoreUser()
        }
    }
}
