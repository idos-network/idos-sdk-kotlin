package org.idos.kwil.protocol

import org.idos.kwil.security.signer.Signer
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString

/**
 * Creates a message with action payload.
 *
 * @param payload Base64-encoded action call or execution
 * @param challenge Optional authentication challenge
 * @param signer Optional signer for authenticated messages
 * @param signature Optional pre-computed signature
 * @return Protocol message ready for transmission
 */
fun createMessage(
    payload: Base64String,
    challenge: HexString = "",
    signer: Signer? = null,
    signature: Base64String? = null,
): Message =
    if (signer != null) {
        Message(
            body = MsgBody(payload = payload, challenge = challenge),
            authType = signer.getSignatureType(),
            sender = signer.getIdentifier(),
            signature = signature,
        )
    } else {
        Message(
            body = MsgBody(payload = payload, challenge = challenge),
        )
    }

/**
 * Adds authentication to an existing message.
 *
 * @param signer Cryptographic signer
 * @param signature Optional pre-computed signature
 * @return New message with authentication fields
 */
fun Message.withAuth(
    signer: Signer,
    signature: Base64String? = null,
): Message =
    copy(
        authType = signer.getSignatureType(),
        sender = signer.getIdentifier(),
        signature = signature,
    )

/**
 * Adds signature to an existing message.
 *
 * @param signature Cryptographic signature
 * @return New message with signature
 */
fun Message.withSignature(signature: Base64String): Message = copy(signature = signature)

/**
 * Adds challenge to an existing message.
 *
 * @param challenge Authentication challenge
 * @return New message with challenge
 */
fun Message.withChallenge(challenge: HexString): Message = copy(body = body.copy(challenge = challenge))
