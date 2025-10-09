package org.idos.kwil.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.security.signer.SignatureType
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString

/**
 * Message body containing action payload and authentication challenge.
 *
 * @property payload Base64-encoded action call or execution
 * @property challenge Authentication challenge from server
 */
@Serializable
data class MsgBody(
    val payload: Base64String? = null,
    val challenge: HexString = "",
)

/**
 * KWIL protocol message for action calls and transactions.
 *
 * Simple, immutable data structure for protocol communication.
 *
 * @property body Message body with payload and challenge
 * @property authType Signature algorithm type
 * @property sender Sender's address (hex-encoded)
 * @property signature Cryptographic signature (base64-encoded)
 *
 * @see <a href="https://github.com/trufnetwork/kwil-js/blob/main/src/core/message.ts#L60">kwil-js Message</a>
 */
@Serializable
data class Message(
    val body: MsgBody = MsgBody(),
    @SerialName("auth_type")
    val authType: SignatureType = SignatureType.SIGNATURE_TYPE_INVALID,
    val sender: HexString? = null,
    val signature: Base64String? = null,
)
