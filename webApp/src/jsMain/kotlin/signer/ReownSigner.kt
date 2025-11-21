package signer

import kotlinx.coroutines.await
import org.idos.crypto.JsKeccak256
import org.idos.crypto.eip712.TypedData
import org.idos.kwil.types.HexString
import org.idos.signer.EthSigner
import kotlin.js.Promise

/**
 * Ethereum signer implementation that uses Reown WalletConnect for signing operations.
 *
 * This signer delegates signing operations to a connected wallet via WalletConnect,
 * making it suitable for web applications where users connect their external wallets.
 *
 * @param walletAddress The connected wallet address (without 0x prefix)
 * @param provider The WalletConnect ethers provider for signing operations
 */
class ReownSigner(
    private val walletAddress: String,
    private val provider: dynamic  // ethers BrowserProvider from Reown
) : EthSigner(JsKeccak256()) {

    /**
     * Get the wallet address (without 0x prefix).
     */
    override fun getIdentifier(): HexString {
        return walletAddress.removePrefix("0x")
    }

    /**
     * Sign a message using the connected wallet via WalletConnect.
     *
     * Uses EIP-191 personal sign format.
     *
     * @param msg The message bytes to sign
     * @return Signature bytes (65 bytes: r + s + v)
     */
    override suspend fun sign(msg: ByteArray): ByteArray {
        // Convert ByteArray to UTF-8 string for signing
        val message = msg.decodeToString()

        console.log("=== ReownSigner.sign() ===")
        console.log("Message bytes length:", msg.size)
        console.log("Message string:", message)

        // Get signer from provider (async call)
        val signerPromise: Promise<dynamic> = provider.getSigner().unsafeCast<Promise<dynamic>>()
        val signer = signerPromise.await()

        // Sign the message using personal_sign (async call)
        // signMessage expects a string or Uint8Array, not hex
        val signaturePromise: Promise<String> = signer.signMessage(message).unsafeCast<Promise<String>>()
        val signatureHex = signaturePromise.await()

        console.log("Signature hex:", signatureHex)

        // Convert signature hex string to ByteArray
        val signatureBytes = signatureHex.removePrefix("0x").hexToByteArray()
        console.log("Signature bytes length:", signatureBytes.size)
        console.log("Signature bytes (first 10):", signatureBytes.take(10).joinToString(","))

        return signatureBytes
    }

    /**
     * Sign EIP-712 typed data using the connected wallet.
     *
     * Uses eth_signTypedData_v4 RPC method, sending the full JSON typed data
     * to the wallet for proper EIP-712 signing (wallet performs the hashing).
     *
     * @param typedData The structured data to sign
     * @return Signature string with 0x prefix
     */
    override suspend fun signTypedData(typedData: TypedData): String {
        console.log("=== ReownSigner.signTypedData() ===")

        // Serialize typed data to JSON for eth_signTypedData_v4
        val typedDataJson = typedData.toJsonString()
        console.log("TypedData JSON:", typedDataJson)

        // The address must have 0x prefix for eth_signTypedData_v4
        val addressWithPrefix = if (walletAddress.startsWith("0x")) walletAddress else "0x$walletAddress"
        console.log("Signing address:", addressWithPrefix)

        // Call eth_signTypedData_v4 via the provider
        // This sends the full JSON to the wallet, which performs EIP-712 hashing internally
        val signaturePromise: Promise<String> = provider.send(
            "eth_signTypedData_v4",
            arrayOf(addressWithPrefix, typedDataJson)
        ).unsafeCast<Promise<String>>()

        val signature = signaturePromise.await()
        console.log("Signature:", signature)

        return signature
    }
}

/**
 * Extension function to convert hex string to ByteArray.
 */
private fun String.hexToByteArray(): ByteArray {
    val hex = this.removePrefix("0x")
    return ByteArray(hex.length / 2) {
        hex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}
