package org.idos.kwil.transaction

import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.json.Json
import kwil.utils.sha256BytesToBytes
import org.idos.kwil.KwilActionClient
import org.idos.kwil.rpc.Base64String
import org.idos.kwil.rpc.HexString
import org.idos.kwil.rpc.PayloadType
import org.idos.kwil.rpc.Signature
import org.idos.kwil.rpc.Transaction
import org.idos.kwil.rpc.TransactionBase64
import org.idos.kwil.rpc.TransactionUint8
import org.idos.kwil.rpc.copyAs
import org.idos.kwil.serialization.toByteArray
import org.idos.kwil.signer.BaseSigner
import org.idos.kwil.signer.SignatureType
import org.idos.kwil.utils.SerializationType
import org.idos.kwil.utils.encodeActionExecution

data class PayloadTxOptions(
    val payload: UnencodedActionPayload<List<List<EncodedValue>>>,
    val payloadType: PayloadType,
    val signer: BaseSigner,
    val identifier: ByteArray,
    val signatureType: SignatureType?,
    val chainId: String,
    val description: String,
    val nonce: Int?,
)

class PayloadTx(
    val kwilActionClient: KwilActionClient,
    val payload: UnencodedActionPayload<List<List<EncodedValue>>>,
    val payloadType: PayloadType,
    val signer: BaseSigner,
    val identifier: ByteArray,
    val signatureType: SignatureType?,
    val chainId: String,
    val description: String,
    val nonce: Int?,
) {
    // https://github.com/trufnetwork/kwil-js/blob/main/src/transaction/payloadTx.ts#L96C28-L96C39
    suspend fun buildTx(): TransactionBase64 {
        val preEstTxn = Transaction.createBase64()
        preEstTxn.body.payload = encodePayload(payloadType, payload)
        preEstTxn.body.type = payloadType
        preEstTxn.sender = HexString(identifier)

        // estimate the cost of the transaction with the estimateCost symbol from the client
        // https://github.com/trufnetwork/kwil-js/blob/main/src/transaction/payloadTx.ts#L118
        val cost = kwilActionClient.estimateCostClient(preEstTxn)

        // retrieve the account for the nonce, if one is provided
        var nonce = nonce
        // if no nonce is provided, retrieve the nonce from the account
        if (nonce == null) {
            // TODO: This is not thread safe! I guess the whole method is not thread safe
            val account = kwilActionClient.getAccountClient(signer.accountId())
            requireNotNull(account.nonce) { "something went wrong with your account nonce." }
            nonce = account.nonce + 1
        }

        val encodedPayload = preEstTxn.body.payload

        requireNotNull(
            encodedPayload,
            { "encoded payload is null. This is likely an internal error, please create an issue." },
        )

        // add the nonce and fee to the transaction. Set the tx bytes back to uint8 so we can do the signature.
        val postEstTxn: TransactionUint8 =
            preEstTxn.copyAs(
                payloadMapper = { encodedPayload.toByteArray() },
                senderMapper = { identifier },
                feeMapper = { cost.price.toLong() },
            )
        postEstTxn.body.nonce = nonce
        postEstTxn.body.chainId = chainId

        if (signatureType == null || signatureType == SignatureType.SIGNATURE_TYPE_INVALID) {
            throw IllegalStateException("Signature type not valid.")
        }

        return signTx(postEstTxn, signer, this.description)
    }

    fun encodePayload(
        payloadType: PayloadType,
        payload: UnencodedActionPayload<List<List<EncodedValue>>>,
    ): Base64String {
        when (payloadType) {
            PayloadType.EXECUTE_ACTION -> return encodeActionExecution(payload)
            // TODO: https://github.com/trufnetwork/kwil-js/blob/main/src/transaction/payloadTx.ts#L96C28-L96C39
            // we are missing the rest of the statements
            else -> throw IllegalStateException("Payload type not valid.")
        }
    }

    companion object {
        // https://github.com/trufnetwork/kwil-js/blob/main/src/transaction/payloadTx.ts#L87
        fun createTx(
            kwilActionClient: KwilActionClient,
            options: PayloadTxOptions,
        ): PayloadTx {
            requireNotNull(options.payload)
            requireNotNull(options.description)

            // Create a new empty object
            return PayloadTx(
                kwilActionClient = kwilActionClient,
                payload = options.payload,
                payloadType = options.payloadType,
                signer = options.signer,
                identifier = options.identifier,
                signatureType = options.signatureType,
                chainId = options.chainId,
                description = options.description,
                nonce = options.nonce,
            )
        }

        fun signTx(
            tx: TransactionUint8,
            signer: BaseSigner,
            description: String,
        ): TransactionBase64 {
            val payload = requireNotNull(tx.body.payload) { "Payload is required" }

            // create the digest, which is the first bytes of the sha256 hash of the rlp-encoded payload
            val digest = sha256BytesToBytes(payload).copyOfRange(0, 20)

            /**
             * create the signature message
             * the signature message cannot have any preceding or succeeding white space. Must be exact length as server expects it
             */
            var signatureMessage = "${description}\n\n"
            signatureMessage += "PayloadType: ${Json.encodeToString(tx.body.type).replace("\"", "")}\n"
            signatureMessage += "PayloadDigest: ${HexString(digest)}\n"
            signatureMessage += "Fee: ${tx.body.fee}\n"
            signatureMessage += "Nonce: ${tx.body.nonce}\n\n"
            signatureMessage += "Kwil Chain ID: ${tx.body.chainId}\n"

            val signedMessage = signer.sign(signatureMessage.toByteArray())

            val newTx: TransactionBase64 =
                tx.copyAs(
                    senderMapper = { HexString(requireNotNull(it)) },
                    payloadMapper = { Base64String(requireNotNull(it)) },
                    feeMapper = { requireNotNull(it).toString() },
                )
            newTx.signature =
                Signature(
                    sig = Base64String(signedMessage),
                    type = signer.getSignatureType(),
                )
            newTx.serialization = SerializationType.SIGNED_MSG_CONCAT
            newTx.body.desc = description

            return newTx
        }
    }
}
