package org.idos.app.security

import org.idos.app.data.StorageManager
import org.idos.crypto.BouncyCastleKeccak256
import org.idos.crypto.eip712.Eip712Utils
import org.idos.crypto.eip712.TypedData
import org.idos.kwil.types.HexString
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.crypto.signMessage
import org.kethereum.crypto.signMessageHash
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toECKeyPair
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign
import org.kethereum.model.PrivateKey

class EthSigner(
    private val keyManager: KeyManager,
    private val storageManager: StorageManager,
) : org.idos.signer.EthSigner(BouncyCastleKeccak256()) {
    override fun getIdentifier(): HexString {
        val address = storageManager.getStoredWallet()?.cleanHex
        requireNotNull(address)
        return address
    }

    override suspend fun sign(msg: ByteArray): ByteArray =
        keyManager.getStoredKey()?.run {
            val signature =
                PrivateKey(this)
                    .toECKeyPair()
                    .signWithEIP191PersonalSign(msg)
                    .toHex()
                    .removePrefix("0x")
                    .hexToByteArray()
            this.fill(0)
            signature
        } ?: byteArrayOf()

    override suspend fun signTypedData(typedData: TypedData): String =
        keyManager.getStoredKey()?.run {
            val keyPair = PrivateKey(this).toECKeyPair()

            // Hash according to EIP-712 using injected utils
            val hash = Eip712Utils.hashTypedData(keccak256, typedData)

            // Sign the raw hash (no personal sign prefix for EIP-712)
            val signature = signMessageHash(hash, keyPair)

            // Clear sensitive data
            this.fill(0)

            // Convert to bytes
            signature.toHex()
        } ?: ""

    companion object {
        /**
         * Standard Ethereum BIP44 derivation path matching iOS
         * m/44'/60'/0'/0/47
         */
        const val DEFAULT_DERIVATION_PATH = "m/44'/60'/0'/0/47"

        fun String.mnemonicToKeypair(derivationPath: String): ByteArray {
            val mnemonic = MnemonicWords(this)
            val seed = mnemonic.toSeed("")
            val key = seed.toKey(derivationPath)
            return key.keyPair.privateKey.key
                .toByteArray()
        }

        fun ByteArray.privateToAddress() =
            PrivateKey(this)
                .toECKeyPair()
                .publicKey
                .toAddress()
    }
}
