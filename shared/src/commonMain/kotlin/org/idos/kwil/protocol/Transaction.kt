package org.idos.kwil.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.security.signer.Signer
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString

typealias TransactionBase64 = Transaction<HexString, Base64String, String>
typealias TransactionUint8 = Transaction<ByteArray, ByteArray, Long>

@Serializable
data class TxBody<TPayload, TFee>(
    val desc: String,
    val payload: TPayload,
    val type: PayloadType = PayloadType.EXECUTE_ACTION,
    val fee: TFee,
    val nonce: Int,
    @SerialName("chain_id")
    val chainId: String,
)

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/tx.ts#L38C13-L38C24
@Serializable
class Transaction<TSender, TPayload, TFee>(
    val signature: Signature,
    val body: TxBody<TPayload, TFee>,
    val sender: TSender,
    val serialization: SerializationType,
) {
    fun isSigned(): Boolean = signature.sig !== null
}

/**
 * Builds an unsigned transaction.
 *
 * @param payload The action payload (Base64 encoded)
 * @param description Transaction description
 * @param nonce Account nonce
 * @param signer The signer (for identifier and signature type)
 * @return Unsigned transaction
 */
internal fun buildTransaction(
    payload: Base64String,
    description: String,
    nonce: Int,
    signer: Signer,
    chainId: String,
): TransactionBase64 =
    Transaction(
        signature = Signature(sig = null, type = signer.getSignatureType()),
        body =
            TxBody(
                desc = description,
                payload = payload,
                type = PayloadType.EXECUTE_ACTION,
                fee = "0",
                nonce = nonce,
                chainId = chainId,
            ),
        sender = signer.getIdentifier(),
        serialization = SerializationType.SIGNED_MSG_CONCAT,
    )
