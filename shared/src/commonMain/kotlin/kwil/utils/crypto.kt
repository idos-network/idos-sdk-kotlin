package kwil.utils

import org.kotlincrypto.hash.sha2.SHA256

fun sha256BytesToBytes(message: ByteArray): ByteArray {
    val digest = SHA256()
    return digest.digest(message)
}
