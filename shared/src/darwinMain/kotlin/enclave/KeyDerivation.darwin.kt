package org.idos.enclave

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import libsodium.crypto_pwhash_scryptsalsa208sha256_ll
import libsodium.sodium_init
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.precomposedStringWithCompatibilityMapping

@OptIn(ExperimentalForeignApi::class, ExperimentalUnsignedTypes::class, BetaInteropApi::class)
class DarwinKeyDerivation : KeyDerivation() {
    init {
        if (sodium_init() < 0) {
            error("Failed to initialize libsodium")
        }
    }

    override fun normalizeString(input: String): String {
        val nsString = NSString.create(string = input)
        return nsString.precomposedStringWithCompatibilityMapping
    }

    override fun scryptGenerate(
        passwordBytes: ByteArray,
        saltBytes: ByteArray,
        n: Int,
        r: Int,
        p: Int,
        dkLen: Int,
    ): ByteArray {
        val key = ByteArray(dkLen)

        val result =
            passwordBytes.usePinned { passPinned ->
                saltBytes.usePinned { saltPinned ->
                    key.usePinned { keyPinned ->
                        crypto_pwhash_scryptsalsa208sha256_ll(
                            passPinned.addressOf(0).reinterpret<UByteVar>(),
                            passPinned.get().size.toULong(),
                            saltPinned.addressOf(0).reinterpret<UByteVar>(),
                            saltBytes.size.toULong(),
                            n.toULong(),
                            r.toUInt(),
                            p.toUInt(),
                            keyPinned.addressOf(0).reinterpret<UByteVar>(),
                            dkLen.toULong(),
                        )
                    }
                }
            }

        if (result != 0) {
            error("Key derivation failed with error code: $result")
        }

        return key
    }
}

actual fun getKeyDerivation(): KeyDerivation = DarwinKeyDerivation()
