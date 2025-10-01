package org.idos.enclave

actual fun getTestEncryption(): Encryption = IosEncryption(MockSecureStorage())
