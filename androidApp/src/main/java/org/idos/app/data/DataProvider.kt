package org.idos.app.data

import org.idos.IdosClient
import org.idos.get
import org.idos.getAll
import org.idos.getOwned
import org.idos.hasProfile
import org.idos.logging.IdosLogConfig
import org.idos.signer.Signer
import org.idos.kwil.types.HexString
import org.idos.kwil.types.UuidString

class DataProvider(
    private val url: String,
    signer: Signer,
    private val chainId: String,
    private val logConfig: IdosLogConfig =
        IdosLogConfig.build {
            platformSink()
        },
) {
    private var client = IdosClient.create(url, chainId, signer, logConfig)

    /**
     * Reinitialize the client with a new signer.
     * Used when switching between local and external signers.
     */
    fun initializeWithSigner(signer: Signer) {
        client = IdosClient.create(url, chainId, signer, logConfig)
    }

    suspend fun getWallets() = client.wallets.getAll()

    suspend fun getCredentials() = client.credentials.getAll()

    suspend fun getCredential(id: UuidString) = client.credentials.getOwned(id)

    suspend fun getUser() = client.users.get()

    suspend fun hasUserProfile(address: HexString) = client.users.hasProfile(address)
}
