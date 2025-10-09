package org.idos.kwil.serialization

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeIntLe
import kotlinx.io.writeShortLe
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString
import kotlin.io.encoding.Base64

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/bytes.ts

fun Int.uint16(): ByteArray {
    require(this in 0..0xFFFF) { "Number is out of range for uint16" }
    return Buffer().apply { writeShort(this@uint16.toShort()) }.readByteArray()
}

fun Int.uint16Le(): ByteArray {
    require(this in 0..0xFFFF) { "Number is out of range for uint16" }
    return Buffer().apply { writeShortLe(this@uint16Le.toShort()) }.readByteArray()
}

fun Long.uint32(): ByteArray {
    require(this in 0..0xFFFFFFFFL) { "The number must be an integer between 0 and 4294967295." }
    return Buffer().apply { writeInt(this@uint32.toInt()) }.readByteArray()
}

fun Long.uint32Le(): ByteArray {
    require(this in 0..0xFFFFFFFFL) { "The number must be an integer between 0 and 4294967295." }
    return Buffer().apply { writeIntLe(this@uint32Le.toInt()) }.readByteArray()
}

fun Number.toByteArray(): ByteArray {
    require(this is Int || this is Long) { "Unsupported type for conversion to bytes: ${this::class}" }

    val value = this.toLong()
    // "safe integer" (max 2^53 - 1)
    require(value in 0..9007199254740991L) { "Number out of bounds for safe integer representation" }

    val high = (value ushr 32).toInt()
    val low = (value and 0xFFFFFFFFL).toInt()

    return Buffer()
        .apply {
            writeInt(high)
            writeInt(low)
        }.readByteArray()
}

fun ByteArray.prefixBytesWithLength(): ByteArray {
    val length = this.size.toLong().uint32Le()
    return length + this
}

fun Boolean.toByteArray() = byteArrayOf(if (this) 1 else 0)

fun Base64String.toByteArray() = Base64.decode(this.value)
