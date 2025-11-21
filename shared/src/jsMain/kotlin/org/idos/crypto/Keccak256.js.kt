package org.idos.crypto

import org.idos.toUint8Array
import org.khronos.webgl.Uint8Array

/**
 * External declarations for js-sha3 library.
 * https://github.com/nicolo-ribaudo/js-sha3
 */
@JsModule("js-sha3")
@JsNonModule
external object JsSha3 {
    val keccak256: dynamic
}

/**
 * Browser/JavaScript implementation of Keccak256 using js-sha3 library.
 */
class JsKeccak256 : Keccak256Hasher {
    override fun digest(data: ByteArray): ByteArray {
        // Compute hash and get hex string
        val hashHex = JsSha3.keccak256(data.toUint8Array()) as String
        // Convert hex string back to ByteArray
        return hashHex.hexToByteArray()
    }
}
