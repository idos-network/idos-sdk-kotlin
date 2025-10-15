package org.idos.enclave

import org.idos.enclave.crypto.Encryption
import org.idos.enclave.crypto.JvmEncryption

actual fun getTestEncryption(): Encryption = JvmEncryption(MockSecureStorage())
