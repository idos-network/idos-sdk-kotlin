package org.idos.signer

/**
 * Signer type for authentication and signing.
 * Determines the address format and signing method.
 */
enum class SignerType(
    val prefix: String,
) {
    /** Ethereum/EVM-compatible wallets using EIP-712 signatures */
    EVM("eip712"),

    /** NEAR Protocol wallets */
    NEAR("NEAR"),

    /** XRP Ledger wallets */
    XRPL("XRPL"),
}
