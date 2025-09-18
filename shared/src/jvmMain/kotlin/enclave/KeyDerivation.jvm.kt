package org.idos.enclave

import org.bouncycastle.crypto.generators.SCrypt
import java.text.Normalizer

class JvmKeyDerivation : KeyDerivation() {
    
    override fun normalizeString(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFKC)
    }
    
    override fun scryptGenerate(
        passwordBytes: ByteArray,
        saltBytes: ByteArray,
        n: Int,
        r: Int,
        p: Int,
        dkLen: Int
    ): ByteArray {
        return SCrypt.generate(passwordBytes, saltBytes, n, r, p, dkLen)
    }
}

// Platform-specific implementation
actual fun getKeyDerivation(): KeyDerivation = JvmKeyDerivation()
