@file:OptIn(ExperimentalUuidApi::class)

package org.idos.kwil.utils

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/kwilEncoding.ts

import io.ktor.utils.io.core.toByteArray
import org.idos.kwil.rpc.Base64String
import org.idos.kwil.serialization.prefixBytesWithLength
import org.idos.kwil.serialization.toByteArray
import org.idos.kwil.serialization.uint16
import org.idos.kwil.serialization.uint16Le
import org.idos.kwil.serialization.uint32
import org.idos.kwil.transaction.EncodedValue
import org.idos.kwil.transaction.UnencodedActionPayload
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun concatBytes(vararg arrays: ByteArray): ByteArray {
    val size = arrays.sumOf { it.size }
    val result = ByteArray(size)
    var pos = 0
    for (arr in arrays) {
        arr.copyInto(result, pos)
        pos += arr.size
    }
    return result
}

fun isDecimal(n: Number): Boolean = n.toString().contains('.')

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/kwilEncoding.ts#L57
fun encodeActionExecution(action: UnencodedActionPayload<List<List<EncodedValue>>>): Base64String {
    // The version of the action execution encoding used by the Kwil DB Engine
    val actionExecutionVersion = 0

    val encodedVersion = actionExecutionVersion.uint16Le()
    val encodedDbId = action.dbid.toByteArray().prefixBytesWithLength()
    val encodedAction = action.action.toByteArray().prefixBytesWithLength()

    val numArgs = action.arguments?.size ?: 0
    val encodedNumArgs = numArgs.uint16Le()

    var actionArguments = ByteArray(0)
    action.arguments?.forEach { encodedValues ->
        val argLength = encodedValues.size.uint16Le()
        var argBytes = ByteArray(0)

        encodedValues.forEach { value ->
            val evBytes = encodeEncodedValue(value)
            val prefixEvBytes = (evBytes)

            argBytes = concatBytes(argBytes, prefixEvBytes)
        }

        actionArguments = concatBytes(actionArguments, argLength, argBytes)
    }

    val encodedActionArguments = concatBytes(encodedNumArgs, actionArguments)

    return Base64String(
        concatBytes(encodedVersion, encodedDbId, encodedAction, encodedActionArguments),
    )
}

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/kwilEncoding.ts#L198
fun encodeValue(
    value: Any?,
    o: VarType?,
): ByteArray {
    if (o != null) {
        return overrideValue(value, o)
    }

    // uuid case
    if (value is String && isUuid(value)) {
        return encodeNotNull(Uuid.parse(value).toByteArray())
    }

    // null case
    if (value == null) {
        return encodeNull()
    }

    // bytearray case
    if (value is ByteArray) {
        return encodeNotNull(value)
    }

    // decimal case
    if (value is Number && isDecimal(value)) {
        return encodeNotNull(value.toString().toByteArray())
    }

    return when (value) {
        is String -> encodeNotNull(value.toByteArray())
        is Boolean -> encodeNotNull(value.toByteArray())
        is Number -> encodeNotNull(value.toByteArray())
        else -> throw IllegalArgumentException("Unsupported type ${value::class}")
    }
}

fun overrideValue(
    v: Any?,
    o: VarType,
): ByteArray {
    if (v === null) {
        return encodeNull()
    }

    return when (o) {
        VarType.NULL -> encodeNull()
        VarType.TEXT -> encodeNotNull((v as String).toByteArray())
        VarType.INT8 -> encodeNotNull((v as Number).toByteArray())
        VarType.BOOL -> encodeNotNull((v as Boolean).toByteArray())
        VarType.NUMERIC -> encodeNotNull(v.toString().toByteArray())
        VarType.UUID -> encodeNotNull(Uuid.parse(v as String).toByteArray())
        VarType.BYTEA -> encodeNotNull(v as ByteArray)
        else -> throw IllegalArgumentException("invalid scalar value")
    }
}

fun encodeNull(): ByteArray = byteArrayOf(0)

fun encodeNotNull(v: ByteArray): ByteArray {
    val bytes = ByteArray(v.size + 1)
    bytes[0] = 1
    v.copyInto(bytes, 1)
    return bytes
}

//
// https://github.com/trufnetwork/kwil-js/blob/4ffabc8ef583f9b0b8e71abaa7e7527c5e4f5b85/src/utils/kwilEncoding.ts#L28
//
fun encodeActionCall(actionCall: UnencodedActionPayload<MutableList<EncodedValue>>): Base64String {
    val actionCallVersion = 0
    val encodedVersion = actionCallVersion.uint16Le()
    val encodedDbId = actionCall.dbid.toByteArray().prefixBytesWithLength()
    val encodedAction = actionCall.action.toByteArray().prefixBytesWithLength()

    val numArgs = actionCall.arguments?.size ?: 0
    val encodedNumArgs = numArgs.uint16Le()

    var actionArguments = ByteArray(0)
    actionCall.arguments?.forEach { a ->
        val aBytes = encodeEncodedValue(a)
        val prefixedABytes = aBytes.prefixBytesWithLength()
        actionArguments = concatBytes(actionArguments, prefixedABytes)
    }

    val encodedActionArguments = concatBytes(encodedNumArgs, actionArguments)
    val finalBytes = concatBytes(encodedVersion, encodedDbId, encodedAction, encodedActionArguments)

    return Base64String(finalBytes)
}

//
// https://github.com/trufnetwork/kwil-js/blob/4ffabc8ef583f9b0b8e71abaa7e7527c5e4f5b85/src/utils/kwilEncoding.ts#L182
//
fun encodeDataType(dt: DataInfo): ByteArray {
    val dtVersion = 0
    val versionBytes = dtVersion.uint16()

    val nameBytes = dt.name.value.toByteArray()
    val nameLength = nameBytes.size.toLong().uint32()
    val isArray = dt.isArray.toByteArray()
    val metadataLength = (dt.metadata?.getOrNull(0)?.toInt() ?: 0).uint16()
    val precisionLength = (dt.metadata?.getOrNull(1)?.toInt() ?: 0).uint16()

    return concatBytes(versionBytes, nameLength, nameBytes, isArray, metadataLength, precisionLength)
}

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/kwilEncoding.ts#L157
fun encodeEncodedValue(ev: EncodedValue): ByteArray {
    // To encode an `EncodedValue` we need to concat a bytes array with all of the necessary elements
    // The order is important.

    val evVersion = 0
    // convert evVersion to Uint16
    val encodedVersion = evVersion.uint16Le()

    // EncodedValue.type - the `encodeDataType` function to get the bytes
    val encodedType = (encodeDataType(ev.type)).prefixBytesWithLength()

    // EncodedValue.data - first, prepend 4 bytes (uint32) for the length of bytes
    val encodedDataLength = ev.data.size.uint16Le()
    var encodedData = concatBytes(encodedDataLength)
    ev.data.forEach {
        encodedData = concatBytes(encodedData, it.prefixBytesWithLength())
    }

    // Contact bytes together in correct order
    return concatBytes(encodedVersion, encodedType, encodedData)
}
