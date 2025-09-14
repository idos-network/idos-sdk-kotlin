package org.idos.kwil.rpc

import org.idos.kwil.signer.BaseSigner
import org.idos.kwil.signer.SignatureType
import org.idos.kwil.transaction.EncodedValue
import org.idos.kwil.transaction.UnencodedActionPayload
import org.idos.kwil.utils.encodeActionCall

data class PayloadMsgOptions(
    val signature: String? = null,
    val challenge: String? = null,
    val signer: BaseSigner? = null,
    val identifier: HexString? = null,
    val signatureType: SignatureType? = null
)

/**
 * `PayloadMsg` class creates a call message payload that can be sent over GRPC.
 */
// https://github.com/trufnetwork/kwil-js/blob/main/src/message/payloadMsg.ts#L22
class PayloadMsg(
    val payload: UnencodedActionPayload<MutableList<EncodedValue>>,
    val challenge: HexString? = null,
    var signatureType: SignatureType? = null,
    var identifier: HexString? = null,
    var signer: BaseSigner? = null,
    val signature: String? = null
) {
    companion object {
        fun createMsg(
            payload: UnencodedActionPayload<MutableList<EncodedValue>>?,
            options: PayloadMsgOptions
        ): PayloadMsg {
            requireNotNull(payload) {
                "Payload is required for Payload Msg Builder. Please pass a valid payload."
            }

            return PayloadMsg(
                payload = payload,
                signature = options.signature,
                challenge = options.challenge,
                signer = options.signer,
                identifier = options.identifier,
                signatureType = options.signatureType
            )
        }

        fun authMessage(
            message: Message,
            identifier: HexString,
            signatureType: SignatureType
        ): Message {
            val copy = Message.copy(message);
            copy.authType = signatureType;
            copy.sender = identifier;
            return copy;
        }
    }

    // https://github.com/trufnetwork/kwil-js/blob/main/src/message/payloadMsg.ts#L76C9-L76C17
    fun buildMsg(): Message {
        val message = Message.create();

        message.body.payload = encodeActionCall(payload)
        message.body.challenge = challenge ?: ""
        message.signature = signature

        if (signer !== null) {
            return PayloadMsg.authMessage(
                message,
                requireNotNull(identifier),
                requireNotNull(signatureType),
            );
        }

        // return the unsigned message, with the payload base64 encoded
        val copiedMessage = Message.copy(message)
        return copiedMessage
    }
}
