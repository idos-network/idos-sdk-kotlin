package org.idos.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.idos.app.data.DataProvider
import org.idos.app.data.model.Wallet
import org.idos.kwil.domain.generated.view.GetWalletsResponse

interface WalletRepository {
    fun getWallets(): Flow<List<Wallet>>
}

class WalletRepositoryImpl(
    private val dataProvider: DataProvider,
) : WalletRepository {
    override fun getWallets(): Flow<List<Wallet>> =
        flow {
            emit(
                dataProvider
                    .getWallets()
                    .map { it.toWallet() },
            )
        }
}

fun GetWalletsResponse.toWallet(): Wallet = Wallet(address = this.address, network = this.walletType)
