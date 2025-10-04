package org.idos.kwil.serialization

import io.ktor.utils.io.core.toByteArray
import org.idos.kwil.types.Base64String

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
 * Encodes an action call payload (view action).
 *
 * Format: version(2) + namespace_len(4) + namespace + action_len(4) + action + num_args(2) + args
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
 *
 * Format: version(2) + namespace_len(4) + namespace + action_len(4) + action + num_args(2) + args
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
                    argAcc + encodedValue.encodeToBytes()
                }
            val argLen = argBytes.size.toLong().uint32Le()
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
