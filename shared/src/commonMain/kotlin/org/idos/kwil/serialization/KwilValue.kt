package org.idos.kwil.serialization

import org.idos.kwil.types.isValidUuid

/**
 * Type-safe representation of values supported by KWIL database.
 *
 * Replaces fragile Any? checks with exhaustive sealed class pattern.
 */
sealed class KwilValue {
    /** Text/string value */
    data class Text(
        val value: String,
    ) : KwilValue()

    /** UUID value (validated string) */
    data class Uuid(
        val value: String,
    ) : KwilValue() {
        init {
            require(value.isValidUuid()) { "Invalid UUID format: $value" }
        }
    }

    /** Integer value */
    data class Int(
        val value: Long,
    ) : KwilValue()

    /** Numeric/decimal value with precision and scale */
    data class Numeric(
        val value: String,
        val precision: kotlin.Int,
        val scale: kotlin.Int,
    ) : KwilValue()

    /** Boolean value */
    data class Bool(
        val value: Boolean,
    ) : KwilValue()

    /** Binary data */
    data class Bytea(
        val value: ByteArray,
    ) : KwilValue() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Bytea) return false
            return value.contentEquals(other.value)
        }

        override fun hashCode(): kotlin.Int = value.contentHashCode()
    }

    /** Null value */
    data object Null : KwilValue()

    /** Array of values (all same type) */
    data class Array(
        val values: List<KwilValue>,
    ) : KwilValue() {
        init {
            require(values.isNotEmpty()) { "Array must contain at least one value" }
            val firstType = values.first()::class
            require(values.all { it::class == firstType }) {
                "All array values must be of the same type"
            }
        }
    }
}

/**
 * Converts Kotlin value to KwilValue with automatic type detection.
 */
fun Any?.toKwilValue(): KwilValue =
    when (this) {
        null -> KwilValue.Null
        is KwilValue -> this
        is String ->
            if (this.isValidUuid()) {
                KwilValue.Uuid(this)
            } else {
                KwilValue.Text(this)
            }

        is Int -> KwilValue.Int(this.toLong())
        is Long -> KwilValue.Int(this)
        is Double -> analyzeNumeric(this.toString())
        is Float -> analyzeNumeric(this.toString())
        is Boolean -> KwilValue.Bool(this)
        is ByteArray -> KwilValue.Bytea(this)
        is List<*> -> KwilValue.Array(this.map { it.toKwilValue() })
        is Array<*> -> KwilValue.Array(this.map { it.toKwilValue() })
        else ->
            throw IllegalArgumentException(
                "Unsupported type: ${this::class.simpleName}. " +
                    "If using a uuid, blob, or uint256, please convert to a String.",
            )
    }

/**
 * Analyzes numeric string to create appropriate KwilValue.
 */
private fun analyzeNumeric(value: String): KwilValue {
    val parts = value.split('.')
    return if (parts.size == 2) {
        val precision = value.replace(".", "").length
        val scale = parts[1].length
        KwilValue.Numeric(value, precision, scale)
    } else {
        // No decimal point, treat as integer
        KwilValue.Int(value.toLong())
    }
}
