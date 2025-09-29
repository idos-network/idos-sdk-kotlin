package org.idos.app.security

import org.idos.app.data.StorageManager
import org.idos.kwil.rpc.HexString
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toECKeyPair
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign
import org.kethereum.model.PrivateKey

class EthSigner(
    private val keyManager: KeyManager,
    private val storageManager: StorageManager,
) : org.idos.kwil.signer.EthSigner() {
    override fun getIdentifier(): HexString {
        val address = storageManager.getStoredWallet()
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

    companion object {
        fun String.mnemonicToKeypair(derivationPath: String = "m/44'/60'/0'/0/47"): ByteArray {
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
                .hex
    }
}
