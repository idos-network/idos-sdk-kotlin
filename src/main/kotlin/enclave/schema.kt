package org.idos.enclave

import com.iwebpp.crypto.TweetNaclFast

data class PrivateEncryptionProfile(
    val userId: String,
    val password: String,
    val keyPair: TweetNaclFast.Box.KeyPair,
)
