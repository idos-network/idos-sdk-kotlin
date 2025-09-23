package org.idos.kwil.utils

import org.idos.kwil.rpc.Base64String
import org.idos.kwil.transaction.EncodedValue
import kotlin.io.encoding.Base64

@kotlinx.serialization.Serializable
data class EncodedParameterValue(
    val type: EncodedParameterType,
    val data: List<Base64String>,
)

@kotlinx.serialization.Serializable
data class EncodedParameterType(
    val name: String,
    @kotlinx.serialization.SerialName("is_array")
    val isArray: Boolean,
    val metadata: List<Int>
)

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/parameterEncoding.ts#L81C17-L81C32
fun encodeValueType(values: List<ParamsTypes>): List<EncodedValue> {
    return values.map { formatEncodedValue(it.v, it.o) }
}

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/parameterEncoding.ts#L55
// TODO: Any? to sealed class
fun formatEncodedValue(v: Any?, o: DataInfo?): EncodedValue {
   val base = formatDataType(v, o);

    if (v is Array<*>) {
        val data = mutableListOf<ByteArray>();

        for (item in v) {
            data.add(encodeValue(item, null));
        }

        return EncodedValue(
            type = base.type,
            data = data,
        )
    }

    return EncodedValue(
        type = base.type,
        data = listOf(encodeValue(v, null)),
    )
}

fun formatDataType(value: Any?, o: DataInfo?): FullParamsType {
    if (o != null) {
        return FullParamsType(
            data = value,
            type = o
        )
    }

    val detectedType = resolveValueType(value);

    return FullParamsType(
        type = detectedType,
        data = value,
    )
}

fun isUuid(value: String): Boolean {
    return Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matches(value)
}

data class NumberAnalysis(
    val precision: Int,
    val scale: Int,
    val hasDecimal: Boolean
)

fun analyzeNumber(value: Number): NumberAnalysis {
    val stringValue = value.toString()
    val scale = stringValue.substringAfter('.', "").length
    val hasDecimal = scale > 0
    val precision = stringValue.replace(".", "").length
    return NumberAnalysis(precision, scale, hasDecimal)
}

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/parameterEncoding.ts#L131
fun resolveValueType(value: Any?): DataInfo {
    if (value is Array<*>) {
        // In Kwil, if there is an array of values, each value in the array must be of the same type.
        return resolveValueType(value[0]);
    }

    // Default is text on first
    var metadata = listOf(0, 0)
    var varType = VarType.TEXT

    when (value) {
        is String -> {
            if (isUuid(value)) {
                varType = VarType.UUID
            }
        }
        is Number -> {
            val numAnalysis = analyzeNumber(value)
            metadata = listOf(numAnalysis.precision, numAnalysis.scale)
            varType = if (numAnalysis.hasDecimal) VarType.NUMERIC else VarType.INT8
        }
        is Boolean -> varType = VarType.BOOL
        is ByteArray -> varType = VarType.BYTEA
        null -> varType = VarType.NULL
        else -> throw IllegalArgumentException(
            "Unsupported type: ${value::class.simpleName}. If using a uuid, blob, or uint256, please convert to a String."
        )
    }

    return DataInfo(
        metadata = metadata,
        name = varType,
        isArray = false,
    )
}


fun encodeParams(params: Map<String, Any?>): Map<String, EncodedParameterValue> {
    return params.mapValues { (_, value) ->
        formatEncodedValueBase64(value)
    }
}

private fun formatEncodedValueBase64(value: Any?): EncodedParameterValue {
    return when (value) {
        is List<*> -> {
            val encodedValues = value.map { Base64.encode(encodeValue(it, null)) }
            EncodedParameterValue(
                data = encodedValues,
                type = EncodedParameterType(
                    name = getDataType(value.firstOrNull()),
                    isArray = true,
                    metadata = listOf(0, 0)
                )
            )
        }
        else -> {
            EncodedParameterValue(
                data = listOf(Base64.encode(encodeValue(value, null))),
                type = EncodedParameterType(
                    name = getDataType(value),
                    isArray = false,
                    metadata = listOf(0, 0)
                )
            )
        }
    }
}

private fun getDataType(value: Any?): String {
    return when (value) {
        null -> "null"
        is String -> "text"
        is Int -> "int"
        is Long -> "int" 
        is Double -> "float"
        is Boolean -> "bool"
        else -> "text"
    }
}