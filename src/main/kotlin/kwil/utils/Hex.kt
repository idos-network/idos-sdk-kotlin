package org.idos.kwil.utils

import org.idos.kwil.rpc.HexString

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/serial.ts#L82C17-L82C27
fun bytesToHex(bytes: ByteArray): HexString {
    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
        sb.append(String.format("%02x", b))
    }
    return sb.toString()
}

fun hexToBytes(hex: String): ByteArray {
    val cleanHex = hex.removePrefix("0x")
    require(cleanHex.length % 2 == 0) { "Hex string must have even length" }

    return ByteArray(cleanHex.length / 2) { i ->
        cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}