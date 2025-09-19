package org.idos.kwil.utils

import io.ktor.utils.io.core.toByteArray
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeIntLe
import kotlinx.io.writeShortLe

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/bytes.ts

fun numberToUint16BigEndian(number: Int): ByteArray {
    require(number in 0..0xFFFF) { "Number is out of range for uint16" }

    return Buffer().apply { writeShort(number.toShort()) }.readByteArray()
}

fun numberToUint16LittleEndian(number: Int): ByteArray {
    require(number in 0..0xFFFF) { "The number must be an integer between 0 and 65535." }

    return Buffer().apply { writeShortLe(number.toShort()) }.readByteArray()
}

fun numberToUint32LittleEndian(number: Long): ByteArray {
    require(number in 0..0xFFFFFFFFL) { "The number must be an integer between 0 and 4294967295." }

    return Buffer().apply { writeIntLe(number.toInt()) }.readByteArray()
}

fun numberToUint32BigEndian(number: Long): ByteArray {
    require(number in 0..0xFFFFFFFFL) { "The number must be an integer between 0 and 4294967295." }

    return Buffer().apply { writeInt(number.toInt()) }.readByteArray()
}

fun prefixBytesLength(bytes: ByteArray): ByteArray {
    val length = numberToUint32LittleEndian(bytes.size.toLong())
    return length + bytes
}

// uses UTF-8
fun stringToBytes(s: String): ByteArray = s.toByteArray()

// uses UTF-8
fun bytesToString(bytes: ByteArray): String = bytes.decodeToString()

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
        ((parseLong(uuid.substring(24, 36), 16).also { v = it }) / 0x10000000000L and 0xff)
            .toByte(),
        ((v / 0x100000000L) and 0xff).toByte(),
        ((v ushr 24) and 0xff).toByte(),
        ((v ushr 16) and 0xff).toByte(),
        ((v ushr 8) and 0xff).toByte(),
        (v and 0xff).toByte(),
    )
}

private fun parseLong(
    value: String,
    radix: Int,
): Long = value.toLong(radix)
