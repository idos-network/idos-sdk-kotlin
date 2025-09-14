package org.idos.kwil.signer

import org.idos.kwil.rpc.AccountId
import org.idos.kwil.rpc.HexString
import org.idos.kwil.utils.hexToBytes
import org.kethereum.crypto.toAddress
import org.kethereum.model.ECKeyPair
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign

class EthSigner(val keyPair: ECKeyPair): BaseSigner {
    override fun getIdentifier(): HexString {
        return keyPair.toAddress().toString().substring(2) // without 0x
    }

    override fun getSignatureType(): SignatureType {
        return SignatureType.SECP256K1_PERSONAL;
    }

    override fun sign(msg: ByteArray): ByteArray {
        return hexToBytes(keyPair.signWithEIP191PersonalSign(msg).toHex())
    }

    override fun accountId(): AccountId {
        return AccountId(
            identifier = getIdentifier(),
            keyType = KeyType.SECP256K1,
        )
    }
}