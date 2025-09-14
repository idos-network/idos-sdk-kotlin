package org.idos.enclave

import org.bouncycastle.crypto.generators.SCrypt
import org.idos.kwil.utils.isUuid
import java.text.Normalizer

// https://github.com/idos-network/idos-sdk-js/blob/main/packages/utils/src/encryption/idOSKeyDerivation.ts
object idOSKeyDerivation {
    private const val latestVersion = 0.1
    private val allowedVersions = setOf(0.0, 0.1)

    data class KDFConfig(
        val normalizePassword: (String) -> String,
        val validateSalt: (String) -> Boolean,
        val n: Int,
        val r: Int,
        val p: Int,
        val dkLen: Int,
    )

    private fun kdfConfig(version: Double = latestVersion): KDFConfig {
        if (!allowedVersions.contains(version)) {
            throw IllegalArgumentException("Wrong KDF version $version")
        }

        return when (version) {
            0.0 ->
                KDFConfig(
                    normalizePassword = { Normalizer.normalize(it, Normalizer.Form.NFKC) },
                    validateSalt = { isUuid(it) },
                    n = 128,
                    r = 8,
                    p = 1,
                    dkLen = 32,
                )
            0.1 ->
                KDFConfig(
                    normalizePassword = { Normalizer.normalize(it, Normalizer.Form.NFKC) },
                    validateSalt = { isUuid(it) },
                    n = 16384,
                    r = 8,
                    p = 1,
                    dkLen = 32,
                )
            else -> throw IllegalArgumentException("Unsupported KDF version")
        }
    }

    fun deriveKey(
        password: String,
        salt: String,
        version: Double = latestVersion,
    ): ByteArray {
        val cfg = kdfConfig(version)

        if (!cfg.validateSalt(salt)) throw IllegalArgumentException("Invalid salt")

        val normalizedPassword = cfg.normalizePassword(password)
        val passwordBytes = normalizedPassword.toByteArray(Charsets.UTF_8)
        val saltBytes = salt.toByteArray(Charsets.UTF_8)

        return SCrypt.generate(passwordBytes, saltBytes, cfg.n, cfg.r, cfg.p, cfg.dkLen)
    }
}
