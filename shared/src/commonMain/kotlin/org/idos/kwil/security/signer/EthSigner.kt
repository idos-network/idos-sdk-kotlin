package org.idos.kwil.security.signer

import org.idos.kwil.protocol.AccountId

/**
 * Abstract Ethereum signer using Secp256k1 personal sign.
 *
 * Implementations should provide the actual signing logic for Ethereum-compatible
 * signatures (using libraries like Web3j, Kethereum, etc.).
 */
abstract class EthSigner : Signer {
    override fun getSignatureType(): SignatureType = SignatureType.SECP256K1_PERSONAL

    override fun accountId(): AccountId =
        AccountId(
            identifier = getIdentifier(),
            keyType = KeyType.SECP256K1,
        )
}
