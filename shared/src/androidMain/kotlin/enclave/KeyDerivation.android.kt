package org.idos.enclave

// TODO: Implement Android-specific key derivation using appropriate crypto libraries
class AndroidKeyDerivation : KeyDerivation() {
    
    override fun normalizeString(input: String): String {
        TODO("Android string normalization implementation not yet available")
    }
    
    override fun scryptGenerate(
        passwordBytes: ByteArray,
        saltBytes: ByteArray,
        n: Int,
        r: Int,
        p: Int,
        dkLen: Int
    ): ByteArray {
        TODO("Android scrypt implementation not yet available")
    }
}

// Platform-specific implementation
actual fun getKeyDerivation(): KeyDerivation = AndroidKeyDerivation()
