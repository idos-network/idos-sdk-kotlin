package org.idos

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

/**
 * Convert ByteArray to JavaScript Uint8Array.
 */
fun ByteArray.toUint8Array(): Uint8Array {
    val uint8Array = Uint8Array(size)
    forEachIndexed { index, byte ->
        uint8Array.asDynamic()[index] = byte
    }
    return uint8Array
}

/**
 * Convert JavaScript Uint8Array to ByteArray.
 */
fun Uint8Array.toByteArray(): ByteArray = ByteArray(length) { i -> this[i] }
