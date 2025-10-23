package org.idos.enclave.crypto

import kotlinx.serialization.Serializable
import org.idos.kwil.types.Base64String

@Serializable
data class KeyShare(
    val index: Int,
    val share: Base64String,
    val nodeId: String,
    val threshold: Int,
    val totalShares: Int,
)

/**
 * NaCl key pair (Ed25519/Curve25519).
 *
 * @param publicKey Public key bytes (32 bytes)
 * @param secretKey Secret key bytes (32 bytes)
 */
data class KeyPair(
    val publicKey: ByteArray,
    val secretKey: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KeyPair

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!secretKey.contentEquals(other.secretKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + secretKey.contentHashCode()
        return result
    }
}
