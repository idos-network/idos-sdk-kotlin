package org.idos.kwil.security.signer

import org.idos.kwil.types.HexString
import org.idos.kwil.types.HexString.Companion.toHexString
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toECKeyPair
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import java.math.BigInteger

class JvmEthSigner(
    private val keyPair: ECKeyPair,
) : EthSigner() {
    override fun getIdentifier(): HexString = keyPair.toAddress().toString().toHexString()

    override suspend fun sign(msg: ByteArray): ByteArray =
        keyPair
            .signWithEIP191PersonalSign(msg)
            .toHex()
            .removePrefix("0x")
            .hexToByteArray()
}
