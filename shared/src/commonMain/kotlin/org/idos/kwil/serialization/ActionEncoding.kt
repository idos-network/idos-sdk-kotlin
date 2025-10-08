package org.idos.kwil.serialization

import io.ktor.utils.io.core.toByteArray
import org.idos.kwil.domain.ExecuteAction
import org.idos.kwil.domain.PositionalParams
import org.idos.kwil.domain.ViewAction
import org.idos.kwil.protocol.Message
import org.idos.kwil.protocol.TransactionBase64
import org.idos.kwil.protocol.buildTransaction
import org.idos.kwil.protocol.createMessage
import org.idos.kwil.security.signer.Signer
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString

/**
 * Represents an action call or execution payload before encoding.
 *
 * @param T The type of arguments (List<EncodedValue> for call, List<List<EncodedValue>> for execute)
 */
data class ActionPayload<T>(
    val namespace: String,
    val action: String,
    val arguments: T,
)

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
fun <I> ExecuteAction<I>.toTransaction(
    input: List<I>,
    signer: Signer,
    nonce: Int,
    chainId: String,
): TransactionBase64 {
    val positionalParams = toPositionalParams(input)
    val encodedPayload = encodeExecuteAction(namespace, name, positionalParams, positionalTypes)
    return buildTransaction(encodedPayload, description, nonce, signer, chainId)
}

// for test
internal fun <I> ExecuteAction<I>.toTransaction(
    input: I,
    signer: Signer,
    nonce: Int = 0,
    chainId: String = "",
): TransactionBase64 = toTransaction(listOf<I>(input), signer, nonce, chainId)

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
    params: List<PositionalParams>,
    types: List<KwilType>,
): Base64String {
    val encodedValues =
        params.map { row ->
            row.mapIndexed { index, value ->
                val typeHint = types.getOrNull(index)
                value.toKwilValue().encodeWithType(typeHint)
            }
        }

    val payload =
        ActionPayload(
            namespace = namespace,
            action = actionName.lowercase(),
            arguments = encodedValues,
        )

    return encodeActionExecution(payload)
}

/**
 * Encodes an action call payload (view action).
 *
 * Format: version(2) + namespace_len(4) + namespace + action_len(4) + action + num_args(2) + args
 * Format for args: arg_1_size(2) + arg_1_encoded + ... + arg_n_size(2) + arg_n_encoded
 */
fun encodeActionCall(payload: ActionPayload<List<EncodedValue>>): Base64String {
    val version = 0.uint16Le()
    val namespace = payload.namespace.toByteArray().prefixBytesWithLength()
    val action = payload.action.toByteArray().prefixBytesWithLength()

    val numArgs = payload.arguments.size.uint16Le()
    val args =
        payload.arguments.fold(ByteArray(0)) { acc, encodedValue ->
            acc + encodedValue.encodeToBytes().prefixBytesWithLength()
        }

    return Base64String(version + namespace + action + numArgs + args)
}

/**
 * Encodes an action execution payload (mutative action).
 * Payload has a nested lis of args: list(list(EncodedValue))
 *
 * Format: version(2) + namespace_len(4) + namespace + action_len(4) + action + num_args(2) + args
 * Format for outer args: args_1_size(2) + args_1_inner_encoded + ... + args_n_size(2) + args_n_inner_encoded
 * Format for inner args: arg_1_size(2) + arg_1_encoded + ... + arg_n_size(2) + arg_n_encoded
 */
fun encodeActionExecution(payload: ActionPayload<List<List<EncodedValue>>>): Base64String {
    val version = 0.uint16Le()
    val namespace = payload.namespace.toByteArray().prefixBytesWithLength()
    val action = payload.action.toByteArray().prefixBytesWithLength()

    val numArgs = payload.arguments.size.uint16Le()
    val args =
        payload.arguments.fold(ByteArray(0)) { acc, argList ->
            val argBytes =
                argList.fold(ByteArray(0)) { argAcc, encodedValue ->
                    argAcc + encodedValue.encodeToBytes().prefixBytesWithLength()
                }
            val argLen = argList.size.uint16Le()
            acc + argLen + argBytes
        }

    return Base64String(version + namespace + action + numArgs + args)
}

/**
 * Encodes an EncodedValue to bytes.
 *
 * Format: version(2) + type_len(4) + type_bytes + data_len(2) + data
 */
internal fun EncodedValue.encodeToBytes(): ByteArray {
    val version = 0.uint16Le()
    val typeBytes = type.encodeToBytes().prefixBytesWithLength()

    val dataLen = data.size.uint16Le()
    val dataBytes =
        data.fold(ByteArray(0)) { acc, bytes ->
            acc + bytes.prefixBytesWithLength()
        }

    return version + typeBytes + dataLen + dataBytes
}
