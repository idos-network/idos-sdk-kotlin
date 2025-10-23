package org.idos.signer

import org.idos.crypto.Keccak256Hasher
import org.idos.crypto.eip712.TypedData
import org.idos.kwil.protocol.AccountId

/**
 * Abstract Ethereum signer using Secp256k1.
 *
 * Implementations should provide the actual signing logic for Ethereum-compatible
 * signatures (using libraries like Web3j, Kethereum, etc.).
 *
 * This signer supports both:
 * - EIP-191 personal sign (via [sign])
 * - EIP-712 typed data signing (via [signTypedData])
 *
 * @param keccak256 Keccak256 hash function implementation for EIP-712
 */
abstract class EthSigner(
    protected val keccak256: Keccak256Hasher,
) : Signer {
    override val type: SignerType
        get() = SignerType.EVM

    override fun getSignatureType(): SignatureType = SignatureType.SECP256K1_PERSONAL

    /**
     * Sign EIP-712 typed structured data.
     *
     * This method:
     * 1. Hashes the typed data according to EIP-712 spec
     * 2. Signs the resulting hash with the private key
     * 3. Returns the signature bytes (typically 65 bytes: r + s + v)
     *
     * @param typedData The EIP-712 typed data to sign
     * @return The signature hex
     */
    abstract suspend fun signTypedData(typedData: TypedData): String

    override fun accountId(): AccountId =
        AccountId(
            identifier = getIdentifier(),
            keyType = KeyType.SECP256K1,
        )
}
