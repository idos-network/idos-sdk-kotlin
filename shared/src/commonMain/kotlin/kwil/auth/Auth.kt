package org.idos.kwil.auth

import io.ktor.utils.io.core.toByteArray
import org.idos.kwil.KwilActionClient
import org.idos.kwil.rpc.Base64String
import org.idos.kwil.signer.BaseSigner

// https://github.com/trufnetwork/kwil-js/blob/main/src/auth/auth.ts#L35
class Auth(
    var client: KwilActionClient,
) {
    // https://github.com/trufnetwork/kwil-js/blob/main/src/auth/auth.ts#L54
    suspend fun authenticateKGW(signer: BaseSigner): String? {
        val authParam = client.authParam()

        val domain = removeTrailingSlash(client.baseUrl)
        val version = "1"

        verifyAuthProperties(authParam, domain, version, this.client.chainId)

        val msg = composeAuthMsg(authParam, domain, version, this.client.chainId)

        val signature = signer.sign(msg.toByteArray())

        // KGW rpc call
        return this.client.authn(
            authParam.nonce,
            signer.getIdentifier(),
            Base64String(signature),
            signer.getSignatureType(),
        )
    }
}
