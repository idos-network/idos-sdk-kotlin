package org.idos.signer

import org.idos.kwil.protocol.AccountId
import org.idos.kwil.types.HexString

/**
 * Cryptographic signer interface for transactions.
 *
 * Implementations provide signing capabilities for different cryptographic schemes
 * (Ethereum Secp256k1, Ed25519, etc.).
 */
interface Signer {
    /**
     * The type of this signer.
     */
    val type: SignerType

    /**
     * Get the identifier for this signer.
     *
     * - For EVM: Returns the wallet address (e.g., "0x123...")
     * - For NEAR: Returns the public key (e.g., "ed25519:ABC..." or "ABC...")
     * - For XRPL: Returns the public key
     *
     * @return The identifier string
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
