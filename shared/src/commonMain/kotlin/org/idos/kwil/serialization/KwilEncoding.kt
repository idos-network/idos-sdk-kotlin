@file:OptIn(ExperimentalUuidApi::class)

package org.idos.kwil.serialization

import io.ktor.utils.io.core.toByteArray
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Encodes a KwilValue to bytes according to KWIL protocol.
 *
 * All values are prefixed with a null/not-null byte:
 * - 0x00 for null
 * - 0x01 for non-null (followed by the actual value bytes)
 */
fun KwilValue.encode(): ByteArray =
    when (this) {
        is KwilValue.Null -> byteArrayOf(0x00)
        is KwilValue.Uuid -> encodeNotNull(Uuid.parse(value).toByteArray())
        is KwilValue.Text -> encodeNotNull(value.toByteArray())
        is KwilValue.Int -> encodeNotNull(value.toByteArray())
        is KwilValue.Numeric -> encodeNotNull(value.toByteArray())
        is KwilValue.Bool -> encodeNotNull(value.toByteArray())
        is KwilValue.Bytea -> encodeNotNull(value)
        is KwilValue.Array -> throw IllegalStateException("Array encoding handled separately")
    }

/**
 * Encodes a non-null value with 0x01 prefix.
 */
private fun encodeNotNull(bytes: ByteArray): ByteArray {
    val result = ByteArray(bytes.size + 1)
    result[0] = 0x01
    bytes.copyInto(result, destinationOffset = 1)
    return result
}

/**
 * Encoded value with type information.
 *
 * Used for KWIL protocol wire format.
 */
data class EncodedValue(
    val type: KwilType,
    val data: List<ByteArray>,
)

/**
 * Encodes a KwilValue with its type information.
 */
fun KwilValue.encodeWithType(typeHint: KwilType? = null): EncodedValue {
    val actualType = typeHint ?: toKwilType()

    return when (this) {
        is KwilValue.Array -> {
            EncodedValue(
                type = actualType,
                data = values.map { it.encode() },
            )
        }
        else -> {
            EncodedValue(
                type = actualType,
                data = listOf(encode()),
            )
        }
    }
}
