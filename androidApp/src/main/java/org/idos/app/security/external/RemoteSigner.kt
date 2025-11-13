package org.idos.app.security.external

import org.idos.crypto.eip712.TypedData
import org.kethereum.model.Address
import timber.log.Timber

class RemoteSigner(
    private val walletManager: ReownWalletManager,
) {
    suspend fun sign(msg: ByteArray): ByteArray {
        Timber.d("Signing message via external wallet")

        try {
            val signatureHex = walletManager.personalSign(msg)
            val signature = signatureHex.removePrefix("0x").hexToByteArray()

            Timber.d("Message signed successfully, signature length: ${signature.size}")
            return signature
        } catch (e: Exception) {
            Timber.e(e, "Failed to sign message with external wallet")
            throw e
        }
    }

    suspend fun signTypedData(typedData: TypedData): String {
        Timber.d("Signing typed data via external wallet")

        try {
            val signature = walletManager.signTypedData(typedData)
            Timber.d("Typed data signed successfully")
            return signature
        } catch (e: Exception) {
            Timber.e(e, "Failed to sign typed data with external wallet")
            throw e
        }
    }

    fun getActiveAddress(): Address {
        val addressHex = walletManager.getConnectedAddress()
            ?: throw IllegalStateException("No wallet connected")
        return Address(addressHex)
    }

    suspend fun disconnect() {
        walletManager.disconnect()
    }
}
