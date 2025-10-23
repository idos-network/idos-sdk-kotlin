package org.idos.app.ui.screens.wallets

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.idos.app.data.model.Wallet
import org.idos.app.data.repository.WalletRepository
import org.idos.app.ui.screens.base.BaseViewModel
import timber.log.Timber

sealed class WalletsEvent {
    object LoadWallets : WalletsEvent()
    object ClearError : WalletsEvent()
}

data class WalletsState(
    val wallets: List<Wallet> = emptyList(), val isLoading: Boolean = true, val error: String? = null
)

class WalletsViewModel(
    private val walletRepository: WalletRepository
) : BaseViewModel<WalletsState, WalletsEvent>() {

    override fun initialState(): WalletsState = WalletsState()

    override fun onEvent(event: WalletsEvent) {
        when (event) {
            is WalletsEvent.LoadWallets -> loadWallets()
            WalletsEvent.ClearError -> clearError()
        }
    }

    private fun loadWallets() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            try {
                walletRepository.getWallets().collectLatest { wallets ->
                    Timber.d("have wallets {$wallets}")
                    updateState {
                        copy(
                            wallets = wallets, isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.d("no wallets, have exception")
                updateState {
                    copy(
                        isLoading = false, error = "Failed to load wallets: ${e.message}"
                    )
                }
            }
        }
    }

    private fun clearError() {
        updateState { copy(error = null) }
    }
}
