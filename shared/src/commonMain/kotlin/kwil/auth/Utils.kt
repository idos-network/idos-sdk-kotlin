package org.idos.kwil.auth

import org.idos.kwil.rpc.KGWAuthInfo

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/auth.ts#L92
fun removeTrailingSlash(url: String): String = if (url.endsWith("/")) url.dropLast(1) else url

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/auth.ts#L54C1-L75C2
fun composeAuthMsg(
    authParam: KGWAuthInfo,
    domain: String,
    version: String,
    chainId: String,
): String {
    var msg = ""
    msg += "$domain wants you to sign in with your account:\n"
    msg += "\n"
    if (authParam.statement !== "") {
        msg += "${authParam.statement}\n"
    }
    msg += "\n"
    msg += "URI: ${authParam.uri}\n"
    msg += "Version: ${version}\n"
    msg += "Chain ID: ${chainId}\n"
    msg += "Nonce: ${authParam.nonce}\n"
    msg += "Issue At: ${authParam.issueAt}\n"
    msg += "Expiration Time: ${authParam.expirationTime}\n"
    return msg
}

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/auth.ts#L99C1-L114C2
fun verifyAuthProperties(
    authParams: KGWAuthInfo,
    domain: String,
    version: String,
    chainId: String,
) {
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
