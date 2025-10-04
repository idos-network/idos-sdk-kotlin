package org.idos.kwil.serialization

import org.idos.kwil.domain.generated.ExecuteAction
import org.idos.kwil.domain.generated.ViewAction
import org.idos.kwil.protocol.Message
import org.idos.kwil.protocol.createMessage
import org.idos.kwil.security.signer.Signer
import org.idos.kwil.domain.PositionalParams
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString

/**
 * Creates a protocol message for a view action call.
 *
 * View actions are read-only queries that don't modify database state.
 *
 * @param action The view action descriptor
 * @param input Action input parameters
 * @param signer Optional signer for authenticated calls
 * @param challenge Optional authentication challenge
 * @return Message with encoded action call payload
 */
fun <I, O> ViewAction<I, O>.toMessage(
    input: I,
    signer: Signer? = null,
    challenge: HexString = HexString(""),
): Message {
    val positionalParams = toPositionalParams(input)
    val encodedPayload = encodeViewAction(namespace, name, positionalParams, positionalTypes)
    return createMessage(encodedPayload, challenge, signer)
}

/**
 * Creates a protocol message for an execute action (transaction).
 *
 * Execute actions modify database state and must be mined on the blockchain.
 *
 * @param action The execute action descriptor
 * @param input Action input parameters
 * @param signer Signer for transaction authentication
 * @return Message with encoded action execution payload
 */
fun <I> ExecuteAction<I>.toMessage(
    input: I,
    signer: Signer,
): Message {
    val positionalParams = toPositionalParams(input)
    val encodedPayload = encodeExecuteAction(namespace, name, positionalParams, positionalTypes)
    return createMessage(encodedPayload, signer = signer)
}

/**
 * Encodes a view action call to Base64 payload.
 *
 * @param namespace Action namespace (database ID)
 * @param actionName Action name
 * @param params Positional parameters
 * @param types Parameter type information
 * @return Base64-encoded action call
 */
internal fun encodeViewAction(
    namespace: String,
    actionName: String,
    params: PositionalParams,
    types: List<KwilType>,
): Base64String {
    val encodedValues =
        params.mapIndexed { index, value ->
            val typeHint = types.getOrNull(index)
            value.toKwilValue().encodeWithType(typeHint)
        }

    val payload =
        ActionPayload(
            namespace = namespace,
            action = actionName.lowercase(),
            arguments = encodedValues,
        )

    return encodeActionCall(payload)
}

/**
 * Encodes an execute action to Base64 payload.
 *
 * @param namespace Action namespace (database ID)
 * @param actionName Action name
 * @param params Positional parameters
 * @param types Parameter type information
 * @return Base64-encoded action execution
 */
internal fun encodeExecuteAction(
    namespace: String,
    actionName: String,
    params: PositionalParams,
    types: List<KwilType>,
): Base64String {
    val encodedValues =
        params.mapIndexed { index, value ->
            val typeHint = types.getOrNull(index)
            listOf(value.toKwilValue().encodeWithType(typeHint))
        }

    val payload =
        ActionPayload(
            namespace = namespace,
            action = actionName.lowercase(),
            arguments = encodedValues,
        )

    return encodeActionExecution(payload)
}
