package org.idos.signer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cryptographic signature types supported by signers.
 */
@Serializable
enum class SignatureType {
    @SerialName("invalid")
    SIGNATURE_TYPE_INVALID,

    /** Secp256k1 with Ethereum personal sign */
    @SerialName("secp256k1_ep")
    SECP256K1_PERSONAL,

    /** Ed25519 signature */
    @SerialName("ed25519")
    ED25519,
}

/**
 * Cryptographic key types supported by signers.
 */
@Serializable
enum class KeyType {
    /** Secp256k1 elliptic curve (Ethereum) */
    @SerialName("secp256k1")
    SECP256K1,

    /** Ed25519 elliptic curve */
    @SerialName("ed25519")
    ED25519,
}
