package org.idos.kwil.security.signer

import org.idos.kwil.protocol.AccountId
import org.idos.kwil.types.HexString

/**
 * Cryptographic signer interface for KWIL transactions.
 *
 * Implementations provide signing capabilities for different cryptographic schemes
 * (Ethereum Secp256k1, Ed25519, etc.).
 */
interface Signer {
    /**
     * Returns the signer's identifier (typically the address or public key).
     *
     * For Ethereum signers, this is the hex-encoded address.
     * For Ed25519, this is the hex-encoded public key.
     */
    fun getIdentifier(): HexString

    /**
     * Returns the signature type used by this signer.
     */
    fun getSignatureType(): SignatureType

    /**
     * Signs a message and returns the signature.
     *
     * @param msg The message bytes to sign
     * @return The signature bytes
     */
    suspend fun sign(msg: ByteArray): ByteArray

    /**
     * Returns the account identifier for KWIL.
     */
    fun accountId(): AccountId
}
