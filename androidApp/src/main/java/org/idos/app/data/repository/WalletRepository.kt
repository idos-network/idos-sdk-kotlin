package org.idos.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.idos.app.data.ApiClient
import org.idos.app.data.model.Wallet

interface WalletRepository {
    fun getWallets(): Flow<List<Wallet>>
}

class WalletRepositoryImpl(
    private val apiClient: ApiClient,
) : WalletRepository {

    override fun getWallets(): Flow<List<Wallet>> = flow { emit(apiClient.fetchWallets()) }
}
