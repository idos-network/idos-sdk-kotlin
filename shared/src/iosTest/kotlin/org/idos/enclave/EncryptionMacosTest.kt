package org.idos.enclave

import org.idos.enclave.crypto.Encryption
import org.idos.enclave.crypto.IosEncryption

actual fun getTestEncryption(): Encryption = IosEncryption(MockSecureStorage())
