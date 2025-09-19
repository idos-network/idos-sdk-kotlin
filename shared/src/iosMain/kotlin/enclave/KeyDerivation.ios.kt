package org.idos.enclave

// TODO: Implement iOS-specific key derivation using appropriate crypto libraries
class IosKeyDerivation : KeyDerivation() {
    override fun normalizeString(input: String): String {
        TODO("iOS string normalization implementation not yet available")
    }

    override fun scryptGenerate(
        passwordBytes: ByteArray,
        saltBytes: ByteArray,
        n: Int,
        r: Int,
        p: Int,
        dkLen: Int,
    ): ByteArray {
        TODO("iOS scrypt implementation not yet available")
    }
}

// Platform-specific implementation
actual fun getKeyDerivation(): KeyDerivation = IosKeyDerivation()
