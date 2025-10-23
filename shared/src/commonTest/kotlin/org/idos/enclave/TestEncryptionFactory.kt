package org.idos.enclave

import org.idos.enclave.crypto.Encryption

// Platform-specific factory function for creating Encryption instances in tests
expect fun getTestEncryption(): Encryption
