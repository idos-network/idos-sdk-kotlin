package org.idos.crypto

import org.bouncycastle.jcajce.provider.digest.Keccak

/**
 * Android implementation of Keccak256 using Bouncy Castle.
 */
class BouncyCastleKeccak256 : Keccak256Hasher {
    /**
     * Compute Keccak256 hash using Bouncy Castle's implementation.
     *
     * @param data Input bytes to hash
     * @return 32-byte Keccak256 hash
     */
    override fun digest(data: ByteArray): ByteArray {
        val keccak = Keccak.Digest256()
        keccak.update(data)
        return keccak.digest()
    }
}
