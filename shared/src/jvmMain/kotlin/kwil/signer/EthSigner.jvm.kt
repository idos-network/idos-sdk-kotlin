package org.idos.kwil.signer

import org.idos.kwil.rpc.HexString
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toECKeyPair
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import java.math.BigInteger

class JvmEthSigner(
    override val privateKeyHex: String,
) : EthSigner() {
    private val privateKey = PrivateKey(BigInteger(privateKeyHex.removePrefix("0x"), 16))
    private val keyPair: ECKeyPair = privateKey.toECKeyPair()

    override fun getIdentifier(): HexString = HexString(keyPair.toAddress().toString().removePrefix("0x"))

    override fun sign(msg: ByteArray): ByteArray =
        keyPair
            .signWithEIP191PersonalSign(msg)
            .toHex()
            .removePrefix("0x")
            .hexToByteArray()
}

// Platform-specific implementation
actual fun createEthSigner(privateKeyHex: String): EthSigner = JvmEthSigner(privateKeyHex)
