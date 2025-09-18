package org.idos.kwil.signer

import org.idos.kwil.rpc.AccountId
import org.idos.kwil.rpc.HexString

// Abstract Ethereum signer interface
abstract class EthSigner: BaseSigner {
    abstract val privateKeyHex: String
    
    override fun getSignatureType(): SignatureType {
        return SignatureType.SECP256K1_PERSONAL
    }

    override fun accountId(): AccountId {
        return AccountId(
            identifier = getIdentifier(),
            keyType = KeyType.SECP256K1,
        )
    }
}

// Factory function to create platform-specific EthSigner
expect fun createEthSigner(privateKeyHex: String): EthSigner
