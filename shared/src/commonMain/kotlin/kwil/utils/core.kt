package org.idos.kwil.utils

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/action.ts#L16
data class ParamsTypes(
    val v: Any?,
    val o: DataInfo?
)

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/parameterEncoding.ts#L86
data class FullParamsType(
    val type: DataInfo,
    // todo: Replace with ValueType sealed class
    val data: Any?,
)

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/enums.ts#L7
enum class VarType(val value: String) {
    UUID("uuid"),
    TEXT("text"),
    INT8("int8"),
    BOOL("bool"),
    NUMERIC("numeric"),
    NULL("null"),
    BYTEA("bytea"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String): VarType? =
            entries.find { it.value == value }
    }
}
