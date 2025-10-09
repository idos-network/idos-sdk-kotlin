package org.idos.kwil.security.signer

import org.idos.kwil.types.HexString
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign
import org.kethereum.model.ECKeyPair

class JvmEthSigner(
    private val keyPair: ECKeyPair,
) : EthSigner() {
    val address get() = keyPair.toAddress()

    override fun getIdentifier(): HexString = keyPair.toAddress().cleanHex

    override suspend fun sign(msg: ByteArray): ByteArray =
        keyPair
            .signWithEIP191PersonalSign(msg)
            .toHex()
            .removePrefix("0x")
            .hexToByteArray()
}
