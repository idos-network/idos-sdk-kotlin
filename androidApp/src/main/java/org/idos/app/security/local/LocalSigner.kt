package org.idos.app.security.local

import org.idos.crypto.Keccak256Hasher
import org.idos.crypto.eip712.Eip712Utils
import org.idos.crypto.eip712.TypedData
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.crypto.signMessage
import org.kethereum.crypto.signMessageHash
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toECKeyPair
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign
import org.kethereum.model.Address
import org.kethereum.model.PrivateKey

class LocalSigner(
    private val keyManager: KeyManager,
) {
    suspend fun sign(msg: ByteArray): ByteArray =
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

    suspend fun signTypedData(
        typedData: TypedData,
        keccak256: Keccak256Hasher,
    ): String =
        keyManager.getStoredKey()?.run {
            val keyPair = PrivateKey(this).toECKeyPair()
            val hash = Eip712Utils.hashTypedData(keccak256, typedData)
            val signature = signMessageHash(hash, keyPair)
            this.fill(0)
            signature.toHex()
        } ?: ""

    suspend fun getActiveAddress(): Address {
        val privateKey = keyManager.getStoredKey()
            ?: throw IllegalStateException("No key found")

        val address = privateKey.privateToAddress()

        privateKey.fill(0)
        return address
    }

    suspend fun disconnect() {
        keyManager.clearStoredKeys()
    }

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
