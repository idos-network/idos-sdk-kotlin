package org.idos.kwil.rpc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.signer.SignatureType

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/message.ts#L18
@Serializable
sealed interface MsgData {
    var body: MsgBody
    var authType: SignatureType
    var sender: HexString?
    var signature: Base64String?
}

@Serializable
data class MsgBody(
    override var payload: Base64String? = null,
    override var challenge: HexString = "",
) : IMsgBody

interface IMsgBody {
    var payload: Base64String?
    var challenge: HexString
}

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/message.ts#L60
@Serializable
data class Message(
    override var body: MsgBody = MsgBody(),
    @SerialName("auth_type")
    override var authType: SignatureType = SignatureType.SIGNATURE_TYPE_INVALID,
    override var sender: HexString? = null,
    override var signature: Base64String? = null,
) : MsgData {
    companion object {
        // https://github.com/trufnetwork/kwil-js/blob/main/src/core/message.ts#L102
        fun create(): Message {
            // Create a new empty object
            return Message(
                body =
                    MsgBody(
                        payload = null,
                        challenge = "",
                    ),
                authType = SignatureType.SECP256K1_PERSONAL,
                sender = null,
                signature = null,
            )
        }

        fun copy(msg: Message): Message =
            Message(
                body =
                    MsgBody(
                        // TODO: I am not sure about this, it's a deep copy?...
                        payload = msg.body.payload,
                        challenge = msg.body.challenge,
                    ),
                authType = msg.authType,
                sender = msg.sender,
                signature = msg.signature,
            )
    }
}
