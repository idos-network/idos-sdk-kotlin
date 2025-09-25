package org.idos.kwil.signer

import org.idos.kwil.rpc.AccountId
import org.idos.kwil.rpc.HexString

interface BaseSigner {
    fun getIdentifier(): HexString

    fun getSignatureType(): SignatureType

    suspend fun sign(msg: ByteArray): ByteArray

    fun accountId(): AccountId
}
