package org.idos.kwil.security.auth

import org.idos.kwil.protocol.KGWAuthInfo

/**
 * Builds KWIL Gateway authentication messages.
 *
 * This message format is based on Sign-In with Ethereum (SIWE) standard.
 * See: https://github.com/trufnetwork/kwil-js/blob/main/src/core/auth.ts
 */
internal data class AuthMessage(
    val authParams: KGWAuthInfo,
    val domain: String,
    val version: String,
    val chainId: String,
) {
    /**
     * Builds the authentication message to be signed.
     *
     * Format:
     * ```
     * {domain} wants you to sign in with your account:
     *
     * {statement}
     *
     * URI: {uri}
     * Version: {version}
     * Chain ID: {chainId}
     * Nonce: {nonce}
     * Issue At: {issueAt}
     * Expiration Time: {expirationTime}
     * ```
     *
     * @return The formatted message string
     * @throws IllegalArgumentException if auth params don't match expected values
     */
    fun buildMessage(): String {
        verify()

        return buildString {
            val normalizedDomain = if (domain.endsWith("/")) domain.dropLast(1) else domain
            append("$normalizedDomain wants you to sign in with your account:\n")
            append("\n")
            if (authParams.statement.isNotBlank()) {
                append(authParams.statement)
                append("\n")
            }
            append("\n")
            append("URI: ${authParams.uri}\n")
            append("Version: $version\n")
            append("Chain ID: $chainId\n")
            append("Nonce: ${authParams.nonce}\n")
            append("Issue At: ${authParams.issueAt}\n")
            append("Expiration Time: ${authParams.expirationTime}\n")
        }
    }

    /**
     * Verifies that auth params match expected values.
     *
     * @throws IllegalArgumentException if there's a mismatch
     */
    private fun verify() {
        require(authParams.domain == null || authParams.domain == domain) {
            "Domain mismatch: ${authParams.domain} != $domain"
        }

        require(authParams.version == null || authParams.version == version) {
            "Version mismatch: ${authParams.version} != $version"
        }

        require(authParams.chainId == null || authParams.chainId == chainId) {
            "Chain ID mismatch: ${authParams.chainId} != $chainId"
        }
    }
}
