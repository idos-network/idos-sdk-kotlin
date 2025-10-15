package org.idos.crypto

/**
 * Keccak256 hash function interface.
 *
 * Implementations must provide the original Keccak-256 (not SHA3-256).
 * Used for EIP-712 signing and share commitments.
 *
 * Example implementations:
 * - JVM/Android: Use Bouncy Castle's Keccak.Digest256()
 * - iOS: Use CryptoSwift or native implementation
 */
interface Keccak256Hasher {
    /**
     * Compute Keccak256 hash of the input data.
     *
     * @param data Input bytes to hash
     * @return 32-byte Keccak256 hash
     */
    fun digest(data: ByteArray): ByteArray
}

/**
 * Utility object for Keccak256 hashing operations.
 */
object Keccak256 {
    /**
     * Hash data using the provided hasher.
     *
     * @param hasher The Keccak256 hasher implementation
     * @param data Input bytes to hash
     * @return 32-byte Keccak256 hash
     */
    fun hash(hasher: Keccak256Hasher, data: ByteArray): ByteArray = hasher.digest(data)
}
