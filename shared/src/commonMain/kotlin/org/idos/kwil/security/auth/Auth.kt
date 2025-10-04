package org.idos.kwil.security.auth

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.core.toByteArray
import org.idos.kwil.protocol.KwilProtocol
import org.idos.kwil.security.signer.Signer
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString

/**
 * KWIL Gateway authentication handler.
 *
 * Manages challenge-response authentication flow with KWIL Gateway.
 * See: https://github.com/trufnetwork/kwil-js/blob/main/src/auth/auth.ts
 */
internal class Auth(
    private val client: KwilProtocol,
    private val version: String,
) {
    private var cookie: String = ""

    /**
     * Authenticates with KWIL Gateway using challenge-response flow.
     *
     * Flow:
     * 1. Get authentication parameters (nonce, statement, etc.) from gateway
     * 2. Build Sign-In with Ethereum (SIWE) message
     * 3. Sign the message with user's private key
     * 4. Submit signed message to gateway
     * 5. Store session cookie for subsequent requests
     *
     * @param signer The cryptographic signer to use
     */
    suspend fun authenticateKGW(signer: Signer) {
        // 1. Get auth parameters from gateway
        val authParam = client.authParam()

        // 2. Build SIWE-format message
        val msg = AuthMessage(authParam, client.baseUrl, version, client.chainId)
        val sigData = msg.buildMessage().toByteArray()

        // 3. Sign the message
        val signature = signer.sign(sigData)

        // 4. Submit to gateway
        val response =
            client.authn(
                authParam.nonce,
                signer.getIdentifier(),
                Base64String(signature),
                signer.getSignatureType(),
            )

        // 5. Store session cookie
        cookie =
            response.headers["Set-Cookie"]
                ?.split(";")
                ?.firstOrNull()
                .orEmpty()
    }

    /**
     * Applies authentication cookie to HTTP request.
     *
     * @param builder The HTTP request builder
     */
    fun applyAuth(builder: HttpRequestBuilder) {
        if (cookie.isNotEmpty()) {
            builder.headers {
                append(HttpHeaders.Cookie, cookie)
            }
        }
    }
}
