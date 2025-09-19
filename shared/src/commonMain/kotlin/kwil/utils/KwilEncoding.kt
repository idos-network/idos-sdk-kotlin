package org.idos.kwil.utils

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/kwilEncoding.ts

import org.idos.kwil.transaction.EncodedValue
import org.idos.kwil.transaction.UnencodedActionPayload
import kotlin.io.encoding.Base64

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
fun encodeActionExecution(action: UnencodedActionPayload<List<List<EncodedValue>>>): String {
    // The version of the action execution encoding used by the Kwil DB Engine
    val actionExecutionVersion = 0

    val encodedVersion = numberToUint16LittleEndian(actionExecutionVersion)
    val encodedDbId = prefixBytesLength(stringToBytes(action.dbid))
    val encodedAction = prefixBytesLength(stringToBytes(action.action))

    val numArgs = action.arguments?.size ?: 0
    val encodedNumArgs = numberToUint16LittleEndian(numArgs)

    var actionArguments = ByteArray(0)
    action.arguments?.forEach { encodedValues ->
        val argLength = numberToUint16LittleEndian(encodedValues.size)
        var argBytes = ByteArray(0)

        encodedValues.forEach { value ->
            val evBytes = encodeEncodedValue(value)
            val prefixEvBytes = prefixBytesLength(evBytes)

            argBytes = concatBytes(argBytes, prefixEvBytes)
        }

        actionArguments = concatBytes(actionArguments, argLength, argBytes)
    }

    val encodedActionArguments = concatBytes(encodedNumArgs, actionArguments)

    return Base64.encode(
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
        return encodeNotNull(convertUuidToBytes(value))
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
        return encodeNotNull(stringToBytes(value.toString()))
    }

    return when (value) {
        is String -> encodeNotNull(stringToBytes(value))
        is Boolean -> encodeNotNull(booleanToBytes(value))
        is Number -> encodeNotNull(numberToBytes(value))
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
        VarType.TEXT -> encodeNotNull(stringToBytes(v as String))
        VarType.INT8 -> encodeNotNull(numberToBytes(v as Number))
        VarType.BOOL -> encodeNotNull(booleanToBytes(v as Boolean))
        VarType.NUMERIC -> encodeNotNull(stringToBytes(v.toString()))
        VarType.UUID -> encodeNotNull(convertUuidToBytes(v as String))
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
fun encodeActionCall(actionCall: UnencodedActionPayload<MutableList<EncodedValue>>): String {
    val actionCallVersion = 0
    val encodedVersion = numberToUint16LittleEndian(actionCallVersion)
    val encodedDbId = prefixBytesLength(stringToBytes(actionCall.dbid))
    val encodedAction = prefixBytesLength(stringToBytes(actionCall.action))

    val numArgs = actionCall.arguments?.size ?: 0
    val encodedNumArgs = numberToUint16LittleEndian(numArgs)

    var actionArguments = ByteArray(0)
    actionCall.arguments?.forEach { a ->
        val aBytes = encodeEncodedValue(a)
        val prefixedABytes = prefixBytesLength(aBytes)
        actionArguments = concatBytes(actionArguments, prefixedABytes)
    }

    val encodedActionArguments = concatBytes(encodedNumArgs, actionArguments)
    val finalBytes = concatBytes(encodedVersion, encodedDbId, encodedAction, encodedActionArguments)

    return Base64.encode(finalBytes)
}

//
// https://github.com/trufnetwork/kwil-js/blob/4ffabc8ef583f9b0b8e71abaa7e7527c5e4f5b85/src/utils/kwilEncoding.ts#L182
//
fun encodeDataType(dt: DataInfo): ByteArray {
    val dtVersion = 0
    val versionBytes = numberToUint16BigEndian(dtVersion)

    val nameBytes = stringToBytes(dt.name.value)
    val nameLength = numberToUint32BigEndian(nameBytes.size.toLong())
    val isArray = booleanToBytes(dt.isArray)
    val metadataLength = numberToUint16BigEndian(dt.metadata?.getOrNull(0)?.toInt() ?: 0)
    val precisionLength = numberToUint16BigEndian(dt.metadata?.getOrNull(1)?.toInt() ?: 0)

    return concatBytes(versionBytes, nameLength, nameBytes, isArray, metadataLength, precisionLength)
}

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/kwilEncoding.ts#L157
fun encodeEncodedValue(ev: EncodedValue): ByteArray {
    // To encode an `EncodedValue` we need to concat a bytes array with all of the necessary elements
    // The order is important.

    val evVersion = 0
    // convert evVersion to Uint16
    val encodedVersion = numberToUint16LittleEndian(evVersion)

    // EncodedValue.type - the `encodeDataType` function to get the bytes
    val encodedType = prefixBytesLength(encodeDataType(ev.type))

    // EncodedValue.data - first, prepend 4 bytes (uint32) for the length of bytes
    val encodedDataLength = numberToUint16LittleEndian(ev.data.size)
    var encodedData = concatBytes(encodedDataLength)
    ev.data.forEach { it ->
        encodedData = concatBytes(encodedData, prefixBytesLength(it))
    }

    // Contact bytes together in correct order
    return concatBytes(encodedVersion, encodedType, encodedData)
}
