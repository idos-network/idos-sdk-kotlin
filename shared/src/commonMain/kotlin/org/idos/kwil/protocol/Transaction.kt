package org.idos.kwil.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString

typealias TransactionBase64 = Transaction<HexString, Base64String, String>
typealias TransactionUint8 = Transaction<ByteArray, ByteArray, Long>

interface TxnData<TSender, TPayload, TFee> {
    var signature: Signature
    var body: TxBody<TPayload, TFee>
    var sender: TSender?
    var serialization: SerializationType
}

@Serializable
data class TxBody<TPayload, TFee>(
    override var desc: String,
    override var payload: TPayload? = null,
    override var type: PayloadType = PayloadType.EXECUTE_ACTION,
    override var fee: TFee? = null,
    override var nonce: Int?,
    @SerialName("chain_id")
    override var chainId: String,
) : ITxBody<TPayload, TFee>

interface ITxBody<TPayload, TFee> {
    var desc: String
    var payload: TPayload?
    var type: PayloadType
    var fee: TFee?
    var nonce: Int?
    var chainId: String
}

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/tx.ts#L38C13-L38C24
@Serializable
class Transaction<TSender, TPayload, TFee>(
    override var signature: Signature,
    override var body: TxBody<TPayload, TFee>,
    override var sender: TSender?,
    override var serialization: SerializationType,
) : TxnData<TSender, TPayload, TFee> {
    fun isSigned(): Boolean = signature.sig !== null
}
