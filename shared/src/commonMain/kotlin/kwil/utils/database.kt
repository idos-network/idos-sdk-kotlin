package org.idos.kwil.utils

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/database.ts#L32
@kotlinx.serialization.Serializable
data class DataInfo(
    val name: VarType,
    @kotlinx.serialization.SerialName("is_array")
    val isArray: Boolean,
    val metadata: List<Int>?,
)

data class SchemaField(
    val name: String,
    val type: DataInfo,
    val nullable: Boolean = false,
)

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/database.ts#L32C1-L112C1
object DataType {
    val Uuid = DataInfo(VarType.UUID, isArray = false, metadata = listOf(0, 0))
    val UuidArray = DataInfo(VarType.UUID, isArray = true, metadata = listOf(0, 0))

    val Text = DataInfo(VarType.TEXT, isArray = false, metadata = listOf(0, 0))
    val TextArray = DataInfo(VarType.TEXT, isArray = true, metadata = listOf(0, 0))

    val Int = DataInfo(VarType.INT8, isArray = false, metadata = listOf(0, 0))
    val IntArray = DataInfo(VarType.INT8, isArray = true, metadata = listOf(0, 0))

    val Boolean = DataInfo(VarType.BOOL, isArray = false, metadata = listOf(0, 0))
    val BooleanArray = DataInfo(VarType.BOOL, isArray = true, metadata = listOf(0, 0))

    fun Numeric(
        precision: Int,
        scale: Int,
    ): DataInfo = DataInfo(VarType.NUMERIC, isArray = false, metadata = listOf(precision, scale))

    fun NumericArray(
        precision: Int,
        scale: Int,
    ): DataInfo = DataInfo(VarType.NUMERIC, isArray = true, metadata = listOf(precision, scale))

    val Null = DataInfo(VarType.NULL, isArray = false, metadata = listOf(0, 0))
    val NullArray = DataInfo(VarType.NULL, isArray = true, metadata = listOf(0, 0))

    val Bytea = DataInfo(VarType.BYTEA, isArray = false, metadata = listOf(0, 0))
    val ByteaArray = DataInfo(VarType.BYTEA, isArray = true, metadata = listOf(0, 0))
}
