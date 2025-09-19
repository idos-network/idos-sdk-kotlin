package org.idos.kwil.signer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SignatureType(
    val value: String,
) {
    @SerialName("invalid")
    SIGNATURE_TYPE_INVALID("invalid"),

    @SerialName("secp256k1_ep")
    SECP256K1_PERSONAL("secp256k1_ep"),

    @SerialName("ed25519")
    ED25519("ed25519"),
}

@Serializable
enum class KeyType(
    val value: String,
) {
    @SerialName("secp256k1")
    SECP256K1("secp256k1"),

    @SerialName("ed25519")
    ED25519("ed25519"),
}
