package org.idos.kwil.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/bytes.ts

fun numberToUint16BigEndian(number: Int): ByteArray {
    require(number in 0..0xFFFF) { "Number is out of range for uint16" }

    val buffer = ByteBuffer.allocate(2)
    buffer.order(ByteOrder.BIG_ENDIAN)
    buffer.putShort(number.toShort())
    return buffer.array()
}

fun numberToUint16LittleEndian(number: Int): ByteArray {
    require(number in 0..0xFFFF) { "The number must be an integer between 0 and 65535." }

    val buffer = ByteBuffer.allocate(2)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer.putShort(number.toShort())
    return buffer.array()
}

fun numberToUint32LittleEndian(number: Long): ByteArray {
    require(number in 0..0xFFFFFFFFL) { "The number must be an integer between 0 and 4294967295." }

    val buffer = ByteBuffer.allocate(4)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(number.toInt())
    return buffer.array()
}

fun numberToUint32BigEndian(number: Long): ByteArray {
    require(number in 0..0xFFFFFFFFL) { "The number must be an integer between 0 and 4294967295." }

    val buffer = ByteBuffer.allocate(4)
    buffer.order(ByteOrder.BIG_ENDIAN)
    buffer.putInt(number.toInt())
    return buffer.array()
}

fun prefixBytesLength(bytes: ByteArray): ByteArray {
    val length = numberToUint32LittleEndian(bytes.size.toLong())
    return length + bytes
}

fun stringToBytes(s: String): ByteArray = s.toByteArray(Charsets.UTF_8)

// https://github.com/uuidjs/uuid/blob/7844bc2cd98d171bf631965047bb267505e25318/src/parse.ts#L4
fun convertUuidToBytes(uuid: String): ByteArray {
    var v: Long

    return byteArrayOf(
        // Parse ########-....-....-....-............
        ((parseLong(uuid.take(8), 16).also { v = it }) ushr 24).toByte(),
        ((v ushr 16) and 0xff).toByte(),
        ((v ushr 8) and 0xff).toByte(),
        (v and 0xff).toByte(),

        // Parse ........-####-....-....-............
        ((parseLong(uuid.substring(9, 13), 16).also { v = it }) ushr 8).toByte(),
        (v and 0xff).toByte(),

        // Parse ........-....-####-....-............
        ((parseLong(uuid.substring(14, 18), 16).also { v = it }) ushr 8).toByte(),
        (v and 0xff).toByte(),

        // Parse ........-....-....-####-............
        ((parseLong(uuid.substring(19, 23), 16).also { v = it }) ushr 8).toByte(),
        (v and 0xff).toByte(),

        // Parse ........-....-....-....-############
        ((parseLong(uuid.substring(24, 36), 16).also { v = it }) / 0x10000000000L and 0xff).toByte(),
        ((v / 0x100000000L) and 0xff).toByte(),
        ((v ushr 24) and 0xff).toByte(),
        ((v ushr 16) and 0xff).toByte(),
        ((v ushr 8) and 0xff).toByte(),
        (v and 0xff).toByte()
    )
}

private fun parseLong(value: String, radix: Int): Long =
    value.toLong(radix)