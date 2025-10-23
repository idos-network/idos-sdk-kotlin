package org.idos.enclave

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import org.idos.enclave.crypto.AndroidEncryption
import org.idos.enclave.crypto.Encryption

// Android unit tests run on host JVM, so we use desktop lazysodium-java with MockSecureStorage
// This tests the actual AndroidEncryption implementation with real libsodium crypto
actual fun getTestEncryption(): Encryption {
    // Configure JNA to find libsodium from Homebrew
    System.setProperty("jna.library.path", "/opt/homebrew/Cellar/libsodium/1.0.20/lib")

    // Verify libsodium can be loaded
    try {
        LazySodiumJava(SodiumJava())
    } catch (e: Exception) {
        throw IllegalStateException(
            "Failed to load libsodium. Ensure it's installed: brew install libsodium",
            e,
        )
    }

    return AndroidEncryption(MockSecureStorage())
}
