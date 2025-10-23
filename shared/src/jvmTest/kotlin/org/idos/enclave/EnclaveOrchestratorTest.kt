package org.idos.enclave

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.idos.enclave.local.LocalEnclave

/**
 * Tests for simplified EnclaveOrchestrator.
 * Tests state management and withEnclave access control.
 */
class EnclaveOrchestratorTest :
    FunSpec({
        val userId = "550e8400-e29b-41d4-a716-446655440000"
        val password = "test-password"
        val expiration = EnclaveSessionConfig(ExpirationType.TIMED, 3600000L) // 1 hour

        context("State Management") {
            test("Initial state is Locked") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.NoKey)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Locked>()
            }

            test("checkStatus with no key → Locked") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.NoKey)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.checkStatus()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Locked>()
            }

            test("checkStatus with valid key → Unlocked") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                // Generate key first
                orchestrator.unlock(userId, password, expiration)

                // Then check status
                orchestrator.checkStatus()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Unlocked>()
            }

            test("checkStatus with expired key → Locked") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.KeyExpired)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.checkStatus()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Locked>()
            }
        }

        context("unlock() - Key Generation") {
            test("unlock() transitions Locked → Unlocking → Unlocked") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Locked>()

                runCatching {
                    orchestrator.unlock(userId, password, expiration)
                }.isSuccess shouldBe true
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Unlocked>()
            }

            test("unlock() failure → Locked") {
                val error = EnclaveError.KeyGenerationFailed("Test failure")
                val enclave = MockEnclave(MockEnclave.MockBehavior.KeyGenerationFails(error))
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.unlock(userId, password, expiration)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Locked>()
            }

            test("unlock() from Unlocked state re-generates key") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.unlock(userId, password, expiration)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Unlocked>()

                // Unlock again (re-generate)
                runCatching {
                    orchestrator.unlock(userId, "new-password", expiration)
                }.isSuccess shouldBe true
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Unlocked>()
            }
        }

        context("lock() - Key Deletion") {
            test("lock() transitions to Locked") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.unlock(userId, password, expiration)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Unlocked>()

                orchestrator.lock()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Locked>()
            }

            test("lock() when already Locked → still Locked") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.NoKey)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Locked>()

                orchestrator.lock()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Locked>()
            }

            test("lock() failure still transitions to Locked") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.DeleteKeyFails)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.unlock(userId, password, expiration)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Unlocked>()

                runCatching {
                    orchestrator.lock()
                }.isSuccess shouldBe true
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Locked>()
            }
        }

        context("withEnclave() - Access Control") {
            test("withEnclave() when Locked → NoKey error") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.NoKey)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                val exception =
                    runCatching {
                        orchestrator.withEnclave { (it as LocalEnclave).hasValidKey() }
                    }.exceptionOrNull()
                exception.shouldBeInstanceOf<EnclaveError.NoKey>()
            }

            test("withEnclave() when Unlocked → executes action") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.unlock(userId, password, expiration)

                runCatching {
                    orchestrator.withEnclave { (it as LocalEnclave).hasValidKey() }
                }.isSuccess shouldBe true
            }

            test("withEnclave() action returns Result") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.unlock(userId, password, expiration)

                runCatching {
                    orchestrator.withEnclave { enclave ->
                        enclave.encrypt("test".encodeToByteArray(), ByteArray(32))
                    }
                }.isSuccess shouldBe true
            }

            test("withEnclave() propagates action failure") {
                val error = EnclaveError.EncryptionFailed("Test failure")
                val enclave = MockEnclave(MockEnclave.MockBehavior.EncryptFails(error))
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.unlock(userId, password, expiration)

                val exception =
                    runCatching {
                        orchestrator.withEnclave { enclave ->
                            enclave.encrypt("test".encodeToByteArray(), ByteArray(32))
                        }
                    }.exceptionOrNull()
                exception.shouldBeInstanceOf<EnclaveError.EncryptionFailed>()
            }
        }

        context("Enclave Operations via withEnclave") {
            test("Encrypt via withEnclave succeeds") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.unlock(userId, password, expiration)

                val message = "Hello, World!".encodeToByteArray()
                val receiverPubKey = ByteArray(32) { 1 }

                val (ciphertext, nonce) = orchestrator.withEnclave { it.encrypt(message, receiverPubKey) }
                ciphertext.isNotEmpty() shouldBe true
                nonce.isNotEmpty() shouldBe true
            }

            test("Decrypt via withEnclave succeeds") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.unlock(userId, password, expiration)

                val message = "Hello, World!".encodeToByteArray()
                val senderPubKey = ByteArray(32) { 1 }

                // First encrypt
                val (ciphertext, pubkey) = orchestrator.withEnclave { it.encrypt(message, senderPubKey) }

                // MockEncryption uses simple XOR, so just decrypt the ciphertext
                val decrypted = orchestrator.withEnclave { it.decrypt(ciphertext, senderPubKey) }
                decrypted.decodeToString() shouldBe "Hello, World!"
            }

            test("Decrypt with wrong password error") {
                val error = EnclaveError.DecryptionFailed(DecryptFailure.WrongPassword)
                val enclave = MockEnclave(MockEnclave.MockBehavior.DecryptFails(error))
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                orchestrator.unlock(userId, password, expiration)

                val ciphertext = "encrypted".encodeToByteArray()
                val pubkey = ByteArray(32)

                val exception =
                    runCatching {
                        orchestrator.withEnclave { it.decrypt(ciphertext, pubkey) }
                    }.exceptionOrNull()

                exception.shouldBeInstanceOf<EnclaveError.DecryptionFailed>()
                exception.reason shouldBe DecryptFailure.WrongPassword
            }
        }

        context("State Transitions") {
            test("Full lifecycle: unlock → use → lock → can't use") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                // Unlock
                orchestrator.unlock(userId, password, expiration)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Unlocked>()

                // Use enclave
                orchestrator.withEnclave {
                    it.encrypt("data".encodeToByteArray(), ByteArray(32))
                } // Should not throw

                // Lock
                orchestrator.lock()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Locked>()

                // Can't use after lock
                val exception =
                    runCatching {
                        orchestrator.withEnclave {
                            it.encrypt("data".encodeToByteArray(), ByteArray(32))
                        }
                    }.exceptionOrNull()
                exception.shouldBeInstanceOf<EnclaveError.NoKey>()
            }

            test("unlock → lock → unlock again") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave, null, EnclaveKeyType.USER)

                // First unlock
                orchestrator.unlock(userId, password, expiration)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Unlocked>()

                // Lock
                orchestrator.lock()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Locked>()

                // Unlock again
                orchestrator.unlock(userId, "new-password", expiration)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveState.Unlocked>()
            }
        }
    })
