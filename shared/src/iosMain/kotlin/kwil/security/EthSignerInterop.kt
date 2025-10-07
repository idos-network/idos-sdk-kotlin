package org.idos.kwil.security

import org.idos.kwil.protocol.AccountId
import org.idos.kwil.security.signer.KeyType
import org.idos.kwil.security.signer.SignatureType
import org.idos.kwil.security.signer.Signer
import org.idos.kwil.types.HexString

abstract class EthSignerInterop : Signer {
    override fun getIdentifier(): HexString = HexString(getIdentifierInterop())

    override fun getSignatureType(): SignatureType = SignatureType.SECP256K1_PERSONAL

    override fun accountId(): AccountId =
        AccountId(
            identifier = getIdentifier(),
            keyType = KeyType.SECP256K1,
        )

    abstract fun getIdentifierInterop(): String
}
