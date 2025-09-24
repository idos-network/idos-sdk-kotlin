package org.idos.kwil.auth

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.core.toByteArray
import org.idos.kwil.rpc.ApiClient
import org.idos.kwil.rpc.Base64String
import org.idos.kwil.signer.BaseSigner

// https://github.com/trufnetwork/kwil-js/blob/main/src/auth/auth.ts#L35
class Auth(
    private val client: ApiClient,
    private val version: String = "1",
) {
    private var cookie: String = ""

    // https://github.com/trufnetwork/kwil-js/blob/main/src/auth/auth.ts#L54
    suspend fun authenticateKGW(signer: BaseSigner) {
        val authParam = client.authParam()
        val msg = AuthMessage(authParam, client.baseUrl, version, client.chainId)
        val sigData = msg.buildMessage().toByteArray()
        // should this be extracted to user level signature approval?
        val signature = signer.sign(sigData)

        // KGW rpc call
        val response =
            this.client.authn(
                authParam.nonce,
                signer.getIdentifier(),
                Base64String(signature),
                signer.getSignatureType(),
            )

        cookie =
            response.headers["Set-Cookie"]
                ?.split(";")
                ?.get(0)
                .orEmpty()
    }

    fun applyAuth(builder: HttpRequestBuilder) {
        builder.headers { append(HttpHeaders.Cookie, cookie) }
    }
}
