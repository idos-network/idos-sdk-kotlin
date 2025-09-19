package org.idos.kwil.serialization

import org.kotlincrypto.hash.sha2.SHA256

fun ByteArray.sha256(): ByteArray = SHA256().digest(this)
