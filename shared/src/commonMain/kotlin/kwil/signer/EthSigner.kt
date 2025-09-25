package org.idos.kwil.signer

import org.idos.kwil.rpc.AccountId

// Abstract Ethereum signer interface
abstract class EthSigner : BaseSigner {
    override fun getSignatureType(): SignatureType = SignatureType.SECP256K1_PERSONAL

    override fun accountId(): AccountId = AccountId(
        identifier = getIdentifier(),
        keyType = KeyType.SECP256K1,
    )
}
