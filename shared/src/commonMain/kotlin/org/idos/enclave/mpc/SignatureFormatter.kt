package org.idos.enclave.mpc

import io.ktor.utils.io.core.toByteArray
import org.idos.signer.EthSigner
import org.idos.signer.Signer
import org.idos.signer.SignerType

/**
 * Utility for formatting wallet addresses for MPC operations.
 *
 * Formats addresses according to the signer type:
 * - EVM: "eip712:<address>"
 * - NEAR: "NEAR:<publicKey>" (strips ed25519: prefix if present)
 * - XRPL: "XRPL:<publicKey>"
 */
object SignatureFormatter {
    /**
     * Format an address/identifier for MPC operations.
     *
     * @param signerType The type of signer
     * @param identifier The address (for EVM) or public key (for NEAR/XRPL)
     * @return Formatted address string
     */
    fun formatAddress(
        signerType: SignerType,
        identifier: String,
    ): String =
        when (signerType) {
            SignerType.NEAR -> "${signerType.prefix}:${identifier.removePrefix("ed25519:")}"
            else -> "${signerType.prefix}:$identifier"
        }

    fun formatSignatureHeader(
        signerType: SignerType,
        signature: String,
    ): String = "${signerType.prefix} $signature"
}

/**
 * Format the current signer's identifier as a recovery address.
 *
 * @return Formatted address (e.g., "eip712:0x...", "NEAR:...", "XRPL:...")
 */
internal fun Signer.getFormattedAddress(): String = SignatureFormatter.formatAddress(this.type, "0x${this.getIdentifier()}")

/**
 * Sign a message using the wallet-specific serialization format.
 *
 * @param message The signature message to sign
 * @return Hex signature string with signature type prefix
 */
internal suspend fun Signer.signMessageAsAuthHeader(message: SignatureMessage<out MpcRequest>): String {
    val sig =
        when (this) {
            is EthSigner -> {
                // Use EIP-712 typed data signing for EVM
                val typedData = message.toTypedData()
                this.signTypedData(typedData)
            }
            else -> {
                // NEAR/XRPL: sign JSON string
                val messageStr = message.toValueJsonString()
                this.sign(messageStr.toByteArray()).toHexString()
            }
        }
    return SignatureFormatter.formatSignatureHeader(this.type, sig)
}
