package org.idos.kwil.auth

import org.idos.kwil.rpc.KGWAuthInfo

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/auth.ts

data class AuthMessage(
    val authParams: KGWAuthInfo,
    val domain: String,
    val version: String,
    val chainId: String,
) {
    fun buildMessage(): String {
        verify()

        return buildString {
            val domain = if (domain.endsWith("/")) domain.dropLast(1) else domain
            append("$domain wants you to sign in with your account:\n")
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
