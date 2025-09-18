package org.idos.enclave

data class PrivateEncryptionProfile(
    val userId: String,
    val password: String,
    val keyPair: KeyPair,
)
