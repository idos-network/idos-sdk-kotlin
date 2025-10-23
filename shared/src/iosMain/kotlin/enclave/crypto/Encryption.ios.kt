package org.idos.enclave.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import libsodium.crypto_box_MACBYTES
import libsodium.crypto_box_NONCEBYTES
import libsodium.crypto_box_PUBLICKEYBYTES
import libsodium.crypto_box_SECRETKEYBYTES
import libsodium.crypto_box_easy
import libsodium.crypto_box_keypair
import libsodium.crypto_box_open_easy
import libsodium.crypto_scalarmult_base
import libsodium.randombytes_buf
import org.idos.enclave.DecryptFailure
import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.SecureStorage

@OptIn(ExperimentalForeignApi::class)
class IosEncryption(
    storage: SecureStorage,
) : Encryption(storage) {
    override fun generateEphemeralKeyPair(): KeyPair {
        val publicKey = ByteArray(PUBLIC_KEY_BYTES)
        val secretKey = ByteArray(SECRET_KEY_BYTES)

        val result =
            publicKey.usePinned { pkPinned ->
                secretKey.usePinned { skPinned ->
                    crypto_box_keypair(
                        pkPinned.addressOf(0).reinterpret(),
                        skPinned.addressOf(0).reinterpret(),
                    )
                }
            }

        if (result != 0) {
            throw EnclaveError.KeyGenerationFailed("Failed to generate keypair with error code: $result")
        }

        return KeyPair(publicKey, secretKey)
    }

    override suspend fun publicKey(secret: ByteArray): ByteArray =
        withContext(Dispatchers.Default) {
            val publicKey = ByteArray(PUBLIC_KEY_BYTES)
            val result =
                secret.usePinned { skPinned ->
                    publicKey.usePinned { pkPinned ->
                        crypto_scalarmult_base(
                            pkPinned.addressOf(0).reinterpret(),
                            skPinned.addressOf(0).reinterpret(),
                        )
                    }
                }

            if (result != 0) {
                error("Failed to get public key with error code: $result")
            }

            publicKey
        }

    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): Pair<ByteArray, ByteArray> =
        withContext(Dispatchers.Default) {
            // Validate receiver public key
            if (receiverPublicKey.size != PUBLIC_KEY_BYTES) {
                throw EnclaveError.InvalidPublicKey(
                    "Receiver public key must be $PUBLIC_KEY_BYTES bytes, got ${receiverPublicKey.size}",
                )
            }

            // Validate message is not empty
            if (message.isEmpty()) {
                throw EnclaveError.EncryptionFailed("Cannot encrypt empty message")
            }

            val nonce =
                ByteArray(NONCE_BYTES).apply {
                    usePinned { pinned ->
                        randombytes_buf(pinned.addressOf(0), NONCE_BYTES.toULong())
                    }
                }

            val ciphertext = ByteArray(message.size + MAC_BYTES)
            val secretKey = getSecretKey(enclaveKeyType)
            val pubkey = publicKey(secretKey)

            try {
                val result =
                    message.usePinned { msgPinned ->
                        nonce.usePinned { noncePinned ->
                            receiverPublicKey.usePinned { pkPinned ->
                                secretKey.usePinned { skPinned ->
                                    ciphertext.usePinned { ctPinned ->
                                        crypto_box_easy(
                                            ctPinned.addressOf(0).reinterpret(),
                                            msgPinned.addressOf(0).reinterpret(),
                                            message.size.toULong(),
                                            noncePinned.addressOf(0).reinterpret(),
                                            pkPinned.addressOf(0).reinterpret(),
                                            skPinned.addressOf(0).reinterpret(),
                                        )
                                    }
                                }
                            }
                        }
                    }

                if (result != 0) {
                    throw EnclaveError.EncryptionFailed("Libsodium crypto_box_easy failed with code: $result")
                }

                val fullMessage = nonce + ciphertext
                fullMessage to pubkey
            } finally {
                secretKey.fill(0)
            }
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): ByteArray =
        withContext(Dispatchers.Default) {
            val secretKey = getSecretKey(enclaveKeyType)
            try {
                decrypt(fullMessage, secretKey, senderPublicKey)
            } finally {
                secretKey.fill(0)
            }
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        secret: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.Default) {
            if (fullMessage.size <= NONCE_BYTES + MAC_BYTES) {
                throw EnclaveError.DecryptionFailed(
                    reason = DecryptFailure.InvalidCiphertext,
                    details = "Message too short: ${fullMessage.size} bytes",
                )
            }
            if (senderPublicKey.size != PUBLIC_KEY_BYTES) {
                throw EnclaveError.InvalidPublicKey(
                    details = "Public key must be $PUBLIC_KEY_BYTES bytes, got ${senderPublicKey.size}",
                )
            }

            val nonce = fullMessage.copyOfRange(0, NONCE_BYTES)
            val ciphertext = fullMessage.copyOfRange(NONCE_BYTES, fullMessage.size)
            val decrypted = ByteArray(ciphertext.size - MAC_BYTES)

            val result =
                ciphertext.usePinned { encPinned ->
                    nonce.usePinned { noncePinned ->
                        senderPublicKey.usePinned { pkPinned ->
                            secret.usePinned { skPinned ->
                                decrypted.usePinned { decPinned ->
                                    crypto_box_open_easy(
                                        decPinned.addressOf(0).reinterpret(),
                                        encPinned.addressOf(0).reinterpret(),
                                        ciphertext.size.toULong(),
                                        noncePinned.addressOf(0).reinterpret(),
                                        pkPinned.addressOf(0).reinterpret(),
                                        skPinned.addressOf(0).reinterpret(),
                                    )
                                }
                            }
                        }
                    }
                }

            if (result != 0) {
                throw EnclaveError.DecryptionFailed(
                    reason = DecryptFailure.WrongPassword,
                    details = "Libsodium crypto_box_open_easy failed with code: $result",
                )
            }

            decrypted
        }

    companion object {
        private val NONCE_BYTES = crypto_box_NONCEBYTES.toInt()
        private val MAC_BYTES = crypto_box_MACBYTES.toInt()
        private val PUBLIC_KEY_BYTES = crypto_box_PUBLICKEYBYTES.toInt()
        private val SECRET_KEY_BYTES = crypto_box_SECRETKEYBYTES.toInt()
    }
}
