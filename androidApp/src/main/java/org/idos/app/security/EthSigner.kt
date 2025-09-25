package org.idos.app.security

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
) : org.idos.kwil.signer.EthSigner() {
    override fun getIdentifier(): HexString {
        val ethPubKey = (keyManager.address.value as? ConnectedAddress)?.address ?: ""
        // maybe throw if we don't have the key ?
        return HexString.withoutPrefix(ethPubKey)
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
        fun String.mnemonicToKeypair(derivationPath: String = "m/44'/60'/0'/0/47"): Pair<ByteArray, String> {
            val mnemonic = MnemonicWords(this)
            val seed = mnemonic.toSeed("")
            val key = seed.toKey(derivationPath)
            return Pair(
                key.keyPair.privateKey.key
                    .toByteArray(),
                key.keyPair.publicKey
                    .toAddress()
                    .hex,
            )
        }

        fun ByteArray.privateToAddress() =
            PrivateKey(this)
                .toECKeyPair()
                .publicKey
                .toAddress()
                .hex
    }
}
