package org.idos.kwil.rpc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.signer.SignatureType
import org.idos.kwil.utils.SerializationType

//
// https://github.com/trufnetwork/kwil-js/blob/main/src/core/tx.ts#L70
//

// "defaults"
// BASE64_ENCODED = Transaction<HexString, Base64String, String>
// UINT8_ENCODED = Transaction<ByteArray, ByteArray, Long>

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
    override var chainId: String
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
    override var serialization: SerializationType
) : TxnData<TSender, TPayload, TFee> {
    fun isSigned(): Boolean {
        return signature.sig !== null;
    }

    fun data(): TxnData<TSender, TPayload, TFee> {
        return this;
    }

    companion object {
        // https://github.com/trufnetwork/kwil-js/blob/main/src/core/tx.ts#L104
        fun <TSender, TPayload, TFee> create(): Transaction<TSender, TPayload, TFee> {
            // Create a new empty object
            return Transaction(
                body = TxBody(
                    desc = "",
                    payload = null,
                    type = PayloadType.INVALID_PAYLOAD_TYPE,
                    fee = null,
                    nonce = null,
                    chainId = ""
                ),
                signature = Signature(
                    sig = null,
                    type = SignatureType.SIGNATURE_TYPE_INVALID
                ),
                sender = null,
                serialization = SerializationType.SIGNED_MSG_CONCAT
            )
        }

        fun createBase64(): TransactionBase64 {
            return create();
        }

        fun createUint8(): TransactionUint8 {
            return create();
        }
    }
}

fun <TSender, TPayload, TFee, TOtherSender, TOtherPayload, TOtherFee> Transaction<TSender, TPayload, TFee>.copyAs(
    senderMapper: (TSender?) -> TOtherSender?,
    payloadMapper: (TPayload?) -> TOtherPayload?,
    feeMapper: (TFee?) -> TOtherFee?
): Transaction<TOtherSender, TOtherPayload, TOtherFee> {
    return Transaction(
        signature = Signature(
            sig = this.signature.sig,
            type = this.signature.type
        ),
        body = TxBody(
            desc = this.body.desc,
            payload = payloadMapper(this.body.payload),
            type = this.body.type,
            fee = feeMapper(this.body.fee),
            nonce = this.body.nonce,
            chainId = this.body.chainId
        ),
        sender = senderMapper(this.sender),
        serialization = this.serialization
    )
}

