package org.idos.signer

import org.idos.crypto.JsKeccak256
import org.idos.crypto.eip712.Eip712Utils
import org.idos.crypto.eip712.TypedData
import org.idos.kwil.types.HexString

/**
 * JavaScript/Browser implementation of Ethereum signer.
 *
 * Uses ethers.js for Ethereum cryptography operations.
 * Supports:
 * - EIP-191 personal sign (prefixed with "\x19Ethereum Signed Message:\n")
 * - EIP-712 typed data signing (structured data hashing)
 *
 * @param privateKey The private key as a 32-byte ByteArray or hex string (with or without 0x prefix)
 */
class JsEthSigner(
    privateKey: ByteArray,
) : EthSigner(JsKeccak256()) {

    private val wallet: dynamic

    init {
        // Convert ByteArray to hex string for ethers.js
        val privateKeyHex = "0x" + privateKey.joinToString("") { byte ->
            val hex = byte.toInt().and(0xFF).toString(16)
            if (hex.length == 1) "0$hex" else hex
        }

        // Create wallet from private key using ethers.js
        wallet = ethers.Wallet(privateKeyHex)
    }

    /**
     * Alternative constructor that accepts hex string private key.
     */
    constructor(privateKeyHex: String) : this(privateKeyHex.removePrefix("0x").hexToByteArray())

    /**
     * Get the Ethereum address (without 0x prefix).
     */
    override fun getIdentifier(): HexString {
        val address = wallet.address as String
        return address.removePrefix("0x")
    }

    /**
     * Get the formatted Ethereum address (with 0x prefix).
     */
    fun getFormattedAddress(): String = wallet.address as String

    /**
     * Sign a message using EIP-191 personal sign.
     * Adds the prefix: "\x19Ethereum Signed Message:\n" + len(message)
     */
    override suspend fun sign(msg: ByteArray): ByteArray {
        // Convert message to hex string
        val messageHex = msg.joinToString("") { byte ->
            val hex = byte.toInt().and(0xFF).toString(16)
            if (hex.length == 1) "0$hex" else hex
        }

        // Sign using ethers.js signMessage (automatically applies EIP-191)
        val signatureHex = wallet.signMessage(messageHex).unsafeCast<String>()

        // Convert signature back to ByteArray (remove 0x prefix)
        return signatureHex.removePrefix("0x").hexToByteArray()
    }

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

        // Convert hash to hex string for ethers.js
        val hashHex = "0x" + hash.joinToString("") { byte ->
            val hex = byte.toInt().and(0xFF).toString(16)
            if (hex.length == 1) "0$hex" else hex
        }

        // Sign the raw hash using ethers.js signDigest (no personal sign prefix for EIP-712)
        // ethers v6: wallet.signingKey.sign(digest)
        val signature = wallet.signingKey.sign(hashHex)

        // Serialize signature to compact form (r + s + v)
        // ethers.Signature has .serialized property
        return signature.serialized as String
    }
}

/**
 * External declarations for ethers.js v6
 */
@JsModule("ethers")
@JsNonModule
external object ethers {
    class Wallet(privateKey: String) {
        val address: String
        val signingKey: SigningKey
        fun signMessage(message: String): dynamic
    }

    interface SigningKey {
        fun sign(digest: String): Signature
    }

    interface Signature {
        val serialized: String
    }
}
