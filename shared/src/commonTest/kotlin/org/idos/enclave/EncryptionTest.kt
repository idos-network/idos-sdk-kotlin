package org.idos.enclave

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptionTest :
    StringSpec({
        "should encrypt and decrypt a message" {
            val encryption = getTestEncryption()

            // Generate keys for both parties
            val aliceId = "550e8400-e29b-41d4-a716-446655440001"
            val bobId = "550e8400-e29b-41d4-a716-446655440002"
            val password = "test-password-123"

            withContext(Dispatchers.Default) {
                // Alice generates her key
                val alicePublicKey = encryption.generateKey(aliceId, password, EnclaveKeyType.USER)

                // Create a second encryption instance for Bob
                val bobEncryption = getTestEncryption()
                val bobPublicKey = bobEncryption.generateKey(bobId, password, EnclaveKeyType.USER)

                // Alice encrypts a message for Bob
                val message = "Hello, Bob!".encodeToByteArray()
                val (encryptedMessage, alice) = encryption.encrypt(message, bobPublicKey, EnclaveKeyType.USER)

                alice shouldBe alicePublicKey
                // Verify encryption changed the message
                encryptedMessage shouldNotBe message

                // Bob decrypts the message from Alice
                val decryptedMessage = bobEncryption.decrypt(encryptedMessage, alicePublicKey, EnclaveKeyType.USER)

                // Verify decryption worked
                decryptedMessage.decodeToString() shouldBe "Hello, Bob!"

                // Clean up
                encryption.deleteKey(EnclaveKeyType.USER)
                bobEncryption.deleteKey(EnclaveKeyType.USER)
            }
        }

        "should generate different ciphertexts for same message" {
            val encryption = getTestEncryption()
            val userId = "550e8400-e29b-41d4-a716-446655440003"
            val password = "test-password-456"
            val dummyPubKey = "40305d02602310f54c88dd57f2e15abc4c392b2cd8cded52867e3bfc5709a819".hexToByteArray()

            withContext(Dispatchers.Default) {
                encryption.generateKey(userId, password, EnclaveKeyType.USER)

                val message = "Test message".encodeToByteArray()

                val (encrypted1, _) = encryption.encrypt(message, dummyPubKey, EnclaveKeyType.USER)
                val (encrypted2, _) = encryption.encrypt(message, dummyPubKey, EnclaveKeyType.USER)

                // Due to random nonces, same message should produce different ciphertexts
                encrypted1 shouldNotBe encrypted2

                encryption.deleteKey(EnclaveKeyType.USER)
            }
        }

        "should derive consistent public key from secret" {
            val encryption = getTestEncryption()
            val userId = "550e8400-e29b-41d4-a716-446655440004"
            val password = "test-password-789"
            val dummyPubKey = "40305d02602310f54c88dd57f2e15abc4c392b2cd8cded52867e3bfc5709a819".hexToByteArray()

            withContext(Dispatchers.Default) {
                encryption.generateKey(userId, password, EnclaveKeyType.USER)

                // Get public key twice by encrypting dummy messages
                val (_, pubKey1) = encryption.encrypt("test".encodeToByteArray(), dummyPubKey, EnclaveKeyType.USER)
                val (_, pubKey2) = encryption.encrypt("test".encodeToByteArray(), dummyPubKey, EnclaveKeyType.USER)

                // Public key should be consistent, not empty bytes
                pubKey1 shouldNotBe ByteArray(32)
                pubKey1 shouldBe pubKey2

                encryption.deleteKey(EnclaveKeyType.USER)
            }
        }
    })
