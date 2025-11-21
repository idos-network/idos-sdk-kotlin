package org.idos.enclave

import org.idos.enclave.crypto.Encryption
import org.idos.enclave.crypto.JsEncryption

actual fun getTestEncryption(): Encryption = JsEncryption(MockSecureStorage())
