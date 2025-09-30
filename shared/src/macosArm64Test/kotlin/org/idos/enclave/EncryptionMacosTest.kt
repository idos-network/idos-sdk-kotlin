package org.idos.enclave

// macOS tests use DarwinEncryption with mock storage
// macOS libsodium from Homebrew works perfectly for testing
actual fun getTestEncryption(): Encryption = DarwinEncryption(MockSecureStorage())
