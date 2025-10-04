package org.idos.enclave

import io.ktor.utils.io.core.toByteArray
import org.idos.kwil.types.UuidString

// https://github.com/idos-network/idos-sdk-js/blob/main/packages/utils/src/encryption/idOSKeyDerivation.ts
abstract class KeyDerivation {
    companion object {
        private const val LATEST_VERSION = 0.1
        private val allowedVersions = setOf(0.0, 0.1)

        fun deriveKey(
            password: String,
            salt: String,
            version: Double = LATEST_VERSION,
        ): ByteArray {
            val keyDerivation = getKeyDerivation()
            return keyDerivation.deriveKeyImpl(password, salt, version)
        }
    }

    data class KDFConfig(
        val normalizePassword: (String) -> String,
        val validateSalt: (String) -> Boolean,
        val n: Int,
        val r: Int,
        val p: Int,
        val dkLen: Int,
    )

    protected fun kdfConfig(version: Double = LATEST_VERSION): KDFConfig {
        if (!allowedVersions.contains(version)) {
            throw IllegalArgumentException("Wrong KDF version $version")
        }

        return when (version) {
            0.0 ->
                KDFConfig(
                    normalizePassword = { normalizeString(it) },
                    validateSalt = { UuidString.isValidUuid(it) },
                    n = 128,
                    r = 8,
                    p = 1,
                    dkLen = 32,
                )

            0.1 ->
                KDFConfig(
                    normalizePassword = { normalizeString(it) },
                    validateSalt = { UuidString.isValidUuid(it) },
                    n = 16384,
                    r = 8,
                    p = 1,
                    dkLen = 32,
                )

            else -> throw IllegalArgumentException("Unsupported KDF version")
        }
    }

    fun deriveKeyImpl(
        password: String,
        salt: String,
        version: Double = LATEST_VERSION,
    ): ByteArray {
        val cfg = kdfConfig(version)

        if (!cfg.validateSalt(salt)) throw IllegalArgumentException("Invalid salt")

        val normalizedPassword = cfg.normalizePassword(password)
        val passwordBytes = normalizedPassword.toByteArray()
        val saltBytes = salt.toByteArray()

        return scryptGenerate(passwordBytes, saltBytes, cfg.n, cfg.r, cfg.p, cfg.dkLen)
    }

    abstract fun normalizeString(input: String): String

    abstract fun scryptGenerate(
        passwordBytes: ByteArray,
        saltBytes: ByteArray,
        n: Int,
        r: Int,
        p: Int,
        dkLen: Int,
    ): ByteArray
}

// Get platform-specific key derivation implementation
expect fun getKeyDerivation(): KeyDerivation
