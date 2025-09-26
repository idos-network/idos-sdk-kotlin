package org.idos.app.data

import org.idos.kwil.KwilActionClient
import org.idos.kwil.actions.getCredentialOwned
import org.idos.kwil.actions.getCredentials
import org.idos.kwil.actions.getUser
import org.idos.kwil.actions.getWallets
import org.idos.kwil.rpc.UuidString
import org.idos.kwil.signer.BaseSigner

class DataProvider(
    url: String,
    signer: BaseSigner,
    chainId: String,
) {
    private val client = KwilActionClient(url, signer, chainId)

    suspend fun getWallets() = client.getWallets()

    suspend fun getCredentials() = client.getCredentials()

    suspend fun getCredential(id: UuidString) = client.getCredentialOwned(id)

    suspend fun getUser() = client.getUser()
}
