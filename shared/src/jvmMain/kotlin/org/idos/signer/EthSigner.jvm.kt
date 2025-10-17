package org.idos.signer

import org.idos.crypto.BouncyCastleKeccak256
import org.idos.crypto.eip712.Eip712Utils
import org.idos.crypto.eip712.TypedData
import org.idos.kwil.types.HexString
import org.kethereum.crypto.signMessageHash
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign
import org.kethereum.model.ECKeyPair

/**
 * JVM implementation of Ethereum signer.
 *
 * Supports:
 * - EIP-191 personal sign (prefixed with "\x19Ethereum Signed Message:\n")
 * - EIP-712 typed data signing (structured data hashing)
 */
class JvmEthSigner(
    private val keyPair: ECKeyPair,
) : EthSigner(BouncyCastleKeccak256()) {
    val address get() = keyPair.toAddress()

    override fun getIdentifier(): HexString = keyPair.toAddress().cleanHex

    /**
     * Sign a message using EIP-191 personal sign.
     * Adds the prefix: "\x19Ethereum Signed Message:\n" + len(message)
     */
    override suspend fun sign(msg: ByteArray): ByteArray =
        keyPair
            .signWithEIP191PersonalSign(msg)
            .toHex()
            .removePrefix("0x")
            .hexToByteArray()

    /**
     * Sign EIP-712 typed data.
     *
     * This:
     * 1. Hashes the typed data according to EIP-712 spec
     * 2. Signs the raw hash (no EIP-191 prefix)
     * 3. Returns the signature with recovery ID
     */
    override suspend fun signTypedData(typedData: TypedData): String {
        // Hash according to EIP-712 using static utils
        val hash = Eip712Utils.hashTypedData(keccak256, typedData)

        // Sign the raw hash (no personal sign prefix for EIP-712)
        val signature = signMessageHash(hash, keyPair)

        // Convert to bytes (r + s + v format)
        return signature
            .toHex()
    }
}
