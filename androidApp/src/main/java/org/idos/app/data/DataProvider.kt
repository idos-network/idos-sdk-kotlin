package org.idos.app.data

import org.idos.IdosClient
import org.idos.get
import org.idos.getAll
import org.idos.getOwned
import org.idos.hasProfile
import org.idos.kwil.security.signer.Signer
import org.idos.kwil.types.HexString
import org.idos.kwil.types.UuidString

class DataProvider(
    url: String,
    signer: Signer,
    chainId: String,
) {
    private val client = IdosClient.create(url, chainId, signer).getOrThrow()

    suspend fun getWallets() = client.wallets.getAll()

    suspend fun getCredentials() = client.credentials.getAll()

    suspend fun getCredential(id: UuidString) = client.credentials.getOwned(id)

    suspend fun getUser() = client.users.get()

    suspend fun hasUserProfile(address: HexString) = client.users.hasProfile(address)
}
