package org.idos.enclave.crypto

import org.idos.toByteArray
import org.idos.toUint8Array
import org.khronos.webgl.Uint8Array

/**
 * External declaration for scrypt-js library.
 * https://github.com/ricmoo/scrypt-js
 */
@JsModule("scrypt-js")
@JsNonModule
external object ScryptJs {
    /**
     * Synchronous scrypt key derivation.
     * @param password Password as Uint8Array
     * @param salt Salt as Uint8Array
     * @param N CPU/memory cost parameter (must be power of 2)
     * @param r Block size parameter
     * @param p Parallelization parameter
     * @param dkLen Derived key length in bytes
     * @return Derived key as Uint8Array
     */
    fun syncScrypt(
        password: Uint8Array,
        salt: Uint8Array,
        N: Int,
        r: Int,
        p: Int,
        dkLen: Int,
    ): Uint8Array
}

/**
 * JavaScript/Browser implementation of KeyDerivation using scrypt-js.
 * Uses NFKC normalization and scrypt key derivation matching the JS SDK.
 */
class JsKeyDerivation : KeyDerivation() {
    override fun normalizeString(input: String): String {
        // Use JavaScript's native String.normalize() for NFKC normalization
        return input.asDynamic().normalize("NFKC") as String
    }

    override fun scryptGenerate(
        passwordBytes: ByteArray,
        saltBytes: ByteArray,
        n: Int,
        r: Int,
        p: Int,
        dkLen: Int,
    ): ByteArray {
        val result = ScryptJs.syncScrypt(
            passwordBytes.toUint8Array(),
            saltBytes.toUint8Array(),
            n,
            r,
            p,
            dkLen,
        )
        return result.toByteArray()
    }
}

// Platform-specific implementation
actual fun getKeyDerivation(): KeyDerivation = JsKeyDerivation()
