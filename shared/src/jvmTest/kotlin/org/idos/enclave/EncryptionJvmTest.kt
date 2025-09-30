package org.idos.enclave

actual fun getTestEncryption(): Encryption = JvmEncryption(MockSecureStorage())
