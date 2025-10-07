package org.idos.kwil.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString

typealias TransactionBase64 = Transaction<HexString, Base64String, String>
typealias TransactionUint8 = Transaction<ByteArray, ByteArray, Long>

@Serializable
data class TxBody<TPayload, TFee>(
    val desc: String,
    val payload: TPayload? = null,
    val type: PayloadType = PayloadType.EXECUTE_ACTION,
    val fee: TFee? = null,
    val nonce: Int?,
    @SerialName("chain_id")
    val chainId: String,
)

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/tx.ts#L38C13-L38C24
@Serializable
class Transaction<TSender, TPayload, TFee>(
    val signature: Signature,
    val body: TxBody<TPayload, TFee>,
    val sender: TSender?,
    val serialization: SerializationType,
) {
    fun isSigned(): Boolean = signature.sig !== null
}
