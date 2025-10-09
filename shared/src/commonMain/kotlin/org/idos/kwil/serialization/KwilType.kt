package org.idos.kwil.serialization

import io.ktor.utils.io.core.toByteArray

/**
 * Type-safe representation of KWIL data types.
 *
 * Replaces fragile VarType enum with exhaustive sealed class.
 */
sealed class KwilType {
    abstract val name: String
    abstract val isArray: Boolean

    /** UUID type */
    data class Uuid(
        override val isArray: Boolean = false,
    ) : KwilType() {
        override val name: String = "uuid"
    }

    /** Text/string type */
    data class Text(
        override val isArray: Boolean = false,
    ) : KwilType() {
        override val name: String = "text"
    }

    /** Integer type (int8 in KWIL) */
    data class Int(
        override val isArray: Boolean = false,
    ) : KwilType() {
        override val name: String = "int8"
    }

    /** Numeric/decimal type with precision and scale */
    data class Numeric(
        val precision: kotlin.Int,
        val scale: kotlin.Int,
        override val isArray: Boolean = false,
    ) : KwilType() {
        override val name: String = "numeric"
    }

    /** Boolean type */
    data class Bool(
        override val isArray: Boolean = false,
    ) : KwilType() {
        override val name: String = "bool"
    }

    /** Binary data type */
    data class Bytea(
        override val isArray: Boolean = false,
    ) : KwilType() {
        override val name: String = "bytea"
    }

    /** Null type */
    data class Null(
        override val isArray: Boolean = false,
    ) : KwilType() {
        override val name: String = "null"
    }

    private val metadata: List<kotlin.Int>
        get() =
            when (this) {
                is Numeric -> listOf(precision, scale)
                else -> listOf(0, 0)
            }
}

/**
 * Infers KWIL type from a KwilValue.
 */
fun KwilValue.toKwilType(): KwilType =
    when (this) {
        is KwilValue.Uuid -> KwilType.Uuid()
        is KwilValue.Text -> KwilType.Text()
        is KwilValue.Int -> KwilType.Int()
        is KwilValue.Numeric -> KwilType.Numeric(precision, scale)
        is KwilValue.Bool -> KwilType.Bool()
        is KwilValue.Bytea -> KwilType.Bytea()
        is KwilValue.Null -> KwilType.Null()
        is KwilValue.Array -> {
            val elementType = values.first().toKwilType()
            when (elementType) {
                is KwilType.Uuid -> KwilType.Uuid(isArray = true)
                is KwilType.Text -> KwilType.Text(isArray = true)
                is KwilType.Int -> KwilType.Int(isArray = true)
                is KwilType.Numeric -> KwilType.Numeric(elementType.precision, elementType.scale, isArray = true)
                is KwilType.Bool -> KwilType.Bool(isArray = true)
                is KwilType.Bytea -> KwilType.Bytea(isArray = true)
                is KwilType.Null -> KwilType.Null(isArray = true)
            }
        }
    }

/**
 * Encodes KwilType to bytes for KWIL wire protocol.
 *
 * Format: version(2) + name_len(4) + name + isArray(1) + precision(2) + scale(2)
 */
fun KwilType.encodeToBytes(): ByteArray {
    val version = 0.uint16()
    val nameBytes = name.toByteArray()
    val nameLen = nameBytes.size.toLong().uint32()
    val isArrayByte = if (isArray) byteArrayOf(1) else byteArrayOf(0)

    val (precision, scale) =
        when (this) {
            is KwilType.Numeric -> this.precision to this.scale
            else -> 0 to 0
        }
    val precisionBytes = precision.uint16()
    val scaleBytes = scale.uint16()

    return version + nameLen + nameBytes + isArrayByte + precisionBytes + scaleBytes
}
