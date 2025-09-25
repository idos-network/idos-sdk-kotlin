package org.idos.app.data

import kotlinx.coroutines.delay
import org.idos.app.data.model.Credential
import org.idos.app.data.model.Wallet
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

// Placeholder external API client using suspend functions
class ApiClient {
    suspend fun fetchWallets(): List<Wallet> {
        Timber.d("fetch wallets")
        delay(300.milliseconds)
        if (Math.random() < 0.5) {
            Timber.d("Throw!!!!")
            throw Exception("Failed to fetch wallets")
        }
        return listOf(
            Wallet("0xBf847E2565E3767232C18B5723e957053275B28F", "Ethereum"),
            Wallet("0xc0ffee254729296a45a3885639AC7E10F9d54979", "Base"),
        )
    }
}
