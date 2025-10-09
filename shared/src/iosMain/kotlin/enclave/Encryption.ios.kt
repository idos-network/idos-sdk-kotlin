package org.idos.enclave

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import libsodium.crypto_box_MACBYTES
import libsodium.crypto_box_NONCEBYTES
import libsodium.crypto_box_PUBLICKEYBYTES
import libsodium.crypto_box_easy
import libsodium.crypto_box_open_easy
import libsodium.crypto_scalarmult_base
import libsodium.randombytes_buf

@OptIn(ExperimentalForeignApi::class)
class IosEncryption(
    storage: SecureStorage,
) : Encryption(storage) {

    override suspend fun publicKey(secret: ByteArray): ByteArray =
        runCatchingErrorAsync {
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
        }

    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> =
        runCatchingErrorAsync {
            withContext(Dispatchers.Default) {
                if (receiverPublicKey.size != PUBLIC_KEY_BYTES) {
                    throw EnclaveError.InvalidPublicKey(
                        "Receiver public key must be $PUBLIC_KEY_BYTES bytes, got ${receiverPublicKey.size}",
                    )
                }

                val nonce =
                    ByteArray(NONCE_BYTES).apply {
                        usePinned { pinned ->
                            randombytes_buf(pinned.addressOf(0), NONCE_BYTES.toULong())
                        }
                    }

                val ciphertext = ByteArray(message.size + MAC_BYTES)
                val secretKey = getSecretKey()
                val pubkey = publicKey(secretKey)

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

                secretKey.fill(0)

                if (result != 0) {
                    throw EnclaveError.EncryptionFailed("Libsodium crypto_box_easy failed with code: $result")
                }

                val fullMessage = nonce + ciphertext
                fullMessage to pubkey
            }
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray =
        runCatchingErrorAsync {
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

                val secretKey = getSecretKey()
                val nonce = fullMessage.copyOfRange(0, NONCE_BYTES)
                val ciphertext = fullMessage.copyOfRange(NONCE_BYTES, fullMessage.size)
                val decrypted = ByteArray(ciphertext.size - MAC_BYTES)

                val result =
                    ciphertext.usePinned { encPinned ->
                        nonce.usePinned { noncePinned ->
                            senderPublicKey.usePinned { pkPinned ->
                                secretKey.usePinned { skPinned ->
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

                secretKey.fill(0)

                if (result != 0) {
                    throw EnclaveError.DecryptionFailed(
                        reason = DecryptFailure.WrongPassword,
                        details = "Libsodium crypto_box_open_easy failed with code: $result",
                    )
                }

                decrypted
            }
        }

    companion object {
        private val NONCE_BYTES = crypto_box_NONCEBYTES.toInt()
        private val MAC_BYTES = crypto_box_MACBYTES.toInt()
        private val PUBLIC_KEY_BYTES = crypto_box_PUBLICKEYBYTES.toInt()
    }
}
