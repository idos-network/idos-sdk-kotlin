package org.idos.enclave

import org.bouncycastle.crypto.generators.SCrypt
import java.text.Normalizer

class AndroidKeyDerivation : KeyDerivation() {
    override fun normalizeString(input: String): String = Normalizer.normalize(input, Normalizer.Form.NFKC)

    override fun scryptGenerate(
        passwordBytes: ByteArray,
        saltBytes: ByteArray,
        n: Int,
        r: Int,
        p: Int,
        dkLen: Int,
    ): ByteArray =
        try {
            // Use Bouncy Castle's SCrypt implementation
            SCrypt.generate(passwordBytes, saltBytes, n, r, p, dkLen)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to generate key with SCrypt", e)
        }
}

// Platform-specific implementation
actual fun getKeyDerivation(): KeyDerivation = AndroidKeyDerivation()
