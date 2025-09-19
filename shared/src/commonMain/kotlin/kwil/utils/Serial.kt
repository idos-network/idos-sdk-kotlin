package org.idos.kwil.utils

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import org.idos.kwil.rpc.Base64String
import kotlin.io.encoding.Base64

//
// https://github.com/trufnetwork/kwil-js/blob/4ffabc8ef583f9b0b8e71abaa7e7527c5e4f5b85/src/utils/serial.ts#L154
//
fun booleanToBytes(value: Boolean): ByteArray = byteArrayOf(if (value) 1 else 0)

fun base64ToHex(value: String): String = bytesToHex(base64ToBytes(value))

fun base64ToBytes(value: Base64String): ByteArray = Base64.decode(value)

fun bytesToBase64(bytes: ByteArray): String = Base64.encode(bytes)

fun numberToBytes(num: Number): ByteArray {
    val buffer = Buffer()

    when (num) {
        is Int, is Long -> {
            val value = num.toLong()

            // "safe integer" (max 2^53 - 1)
            if (value !in 0..9007199254740991L) {
                throw IllegalArgumentException("Number out of bounds for safe integer representation")
            }

            val high = (value ushr 32).toInt()
            val low = (value and 0xFFFFFFFFL).toInt()

            buffer.writeInt(high)
            buffer.writeInt(low)
        }

        else -> throw IllegalArgumentException("Unsupported type for conversion to bytes: ${num::class}")
    }

    return buffer.readByteArray()
}
