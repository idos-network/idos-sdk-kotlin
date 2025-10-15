package org.idos.enclave.crypto

import org.idos.crypto.Keccak256Hasher
import kotlin.random.Random

/**
 * Utility for blinding/unblinding secret shares.
 *
 * Blinding adds 32 random bytes to the beginning of each share before upload.
 * This provides additional security by obscuring the actual share data.
 */
object ShareBlinding {
    private const val BLINDING_BYTES = 32

    /**
     * Blind a share by prepending random bytes.
     *
     * @param share The original share bytes
     * @return Blinded share (32 random bytes + original share)
     */
    fun blind(share: ByteArray): ByteArray {
        val random = Random.Default.nextBytes(BLINDING_BYTES)
        return random + share
    }

    /**
     * Remove blinding from a share.
     *
     * @param blindedShare The blinded share bytes
     * @return Original share with blinding removed
     * @throws IllegalArgumentException if the blinded share is too short
     */
    fun unblind(blindedShare: ByteArray): ByteArray {
        require(blindedShare.size > BLINDING_BYTES) {
            "Blinded share too short: ${blindedShare.size} bytes (expected > $BLINDING_BYTES)"
        }
        return blindedShare.copyOfRange(BLINDING_BYTES, blindedShare.size)
    }

    /**
     * Compute Keccak256 commitment of a blinded share.
     *
     * @param hasher The Keccak256 hasher implementation
     * @param blindedShare The blinded share bytes
     * @return Hex string of Keccak256 hash (with 0x prefix)
     */
    fun computeCommitment(
        hasher: Keccak256Hasher,
        blindedShare: ByteArray,
    ): String {
        val hash = hasher.digest(blindedShare)
        return "0x" + hash.toHexString()
    }
}
