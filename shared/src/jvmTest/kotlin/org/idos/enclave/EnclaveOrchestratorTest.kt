package org.idos.enclave

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.idos.kwil.types.UuidString

/**
 * Tests for EnclaveOrchestrator state machine and flow.
 * Based on the use cases defined in the design phase.
 */
class EnclaveOrchestratorTest :
    FunSpec({
        val userId = UuidString("550e8400-e29b-41d4-a716-446655440000")
        val password = "test-password"
        val expiration = 3600000L // 1 hour

        // Happy Path Tests
        context("UC-1: Initial Key Generation") {
            test("UC-1.1: Fresh start - checkStatus shows RequiresKey") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.NoKey)
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.checkStatus()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.RequiresKey>()
            }

            test("UC-1.2: Generate key successfully transitions to Available") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.checkStatus()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.RequiresKey>()

                orchestrator.generateKey(userId, password, expiration)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.Available>()
            }
        }

        context("UC-2: Normal Operations") {
            test("UC-2.1: Encrypt with valid key succeeds") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.generateKey(userId, password, expiration)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.Available>()

                val message = "Hello, World!".encodeToByteArray()
                val receiverPubKey = ByteArray(32) { 1 }

                val result = orchestrator.encrypt(message, receiverPubKey)
                result.isSuccess shouldBe true
            }

            test("UC-2.2: Decrypt with valid key succeeds") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.generateKey(userId, password, expiration)

                val message = "Hello, World!".encodeToByteArray()
                val senderPubKey = ByteArray(32) { 1 }

                // First encrypt to get ciphertext
                val (ciphertext, _) = orchestrator.encrypt(message, senderPubKey).getOrThrow()

                // Then decrypt
                val result = orchestrator.decrypt(ciphertext, senderPubKey)
                result.isSuccess shouldBe true
            }
        }

        context("UC-3: Pending Actions") {
            test("UC-3.1: Action queued when enclave not ready") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.NoKey)
                val orchestrator = EnclaveOrchestrator(enclave)

                var actionExecuted = false
                val result =
                    orchestrator.requireEnclave {
                        actionExecuted = true
                    }

                result.isFailure shouldBe true
                actionExecuted shouldBe false
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.RequiresKey>()
            }

            test("UC-3.2: Pending actions execute after key generation") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                var actionExecuted = false

                // Queue action before key exists
                orchestrator.requireEnclave {
                    actionExecuted = true
                }

                actionExecuted shouldBe false

                // Generate key - should execute pending actions
                orchestrator.generateKey(userId, password, expiration)

                actionExecuted shouldBe true
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.Available>()
            }

            test("UC-3.3: Multiple pending actions execute in order") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                val executionOrder = mutableListOf<Int>()

                orchestrator.requireEnclave { executionOrder.add(1) }
                orchestrator.requireEnclave { executionOrder.add(2) }
                orchestrator.requireEnclave { executionOrder.add(3) }

                executionOrder.size shouldBe 0

                orchestrator.generateKey(userId, password, expiration)

                executionOrder shouldBe listOf(1, 2, 3)
            }
        }

        context("UC-4: User Actions") {
            test("UC-4.1: User cancels key generation flow") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.checkStatus()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.RequiresKey>()

                orchestrator.cancel()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.Cancelled>()
            }

            test("UC-4.2: Pending actions cleared on cancel") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                var action1Executed = false
                var action2Executed = false

                orchestrator.requireEnclave { action1Executed = true }
                orchestrator.requireEnclave { action2Executed = true }

                orchestrator.cancel()

                // Generate key - should NOT execute cancelled actions
                orchestrator.generateKey(userId, password, expiration)

                action1Executed shouldBe false
                action2Executed shouldBe false
            }

            test("UC-4.3: Retry after wrong password suspect") {
                val enclave =
                    MockEnclave(
                        MockEnclave.MockBehavior.DecryptFails(
                            EnclaveError.DecryptionFailed(DecryptFailure.WrongPassword),
                        ),
                    )
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.generateKey(userId, password, expiration)

                val ciphertext = "encrypted".encodeToByteArray()
                val pubkey = ByteArray(32)

                orchestrator.decrypt(ciphertext, pubkey)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.WrongPasswordSuspected>()

                orchestrator.retry()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.Available>()
            }

            test("UC-4.4: Reset clears key and returns to RequiresKey") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.generateKey(userId, password, expiration)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.Available>()

                orchestrator.reset()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.RequiresKey>()
            }
        }

        context("UC-5: Error Handling") {
            test("UC-5.1: Key generation fails") {
                val error = EnclaveError.KeyGenerationFailed("Test failure")
                val enclave = MockEnclave(MockEnclave.MockBehavior.KeyGenerationFails(error))
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.generateKey(userId, password, expiration)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.KeyGenerationError>()
            }

            test("UC-5.2: Decryption fails - wrong password suspected") {
                val enclave =
                    MockEnclave(
                        MockEnclave.MockBehavior.DecryptFails(
                            EnclaveError.DecryptionFailed(DecryptFailure.WrongPassword),
                        ),
                    )
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.generateKey(userId, password, expiration)

                val ciphertext = "encrypted".encodeToByteArray()
                val pubkey = ByteArray(32)

                orchestrator.decrypt(ciphertext, pubkey)

                val state = orchestrator.state.value
                state.shouldBeInstanceOf<EnclaveFlow.WrongPasswordSuspected>()
                state.attemptCount shouldBe 1
            }

            test("UC-5.3: Multiple decrypt failures increment attempt counter") {
                val enclave =
                    MockEnclave(
                        MockEnclave.MockBehavior.DecryptFails(
                            EnclaveError.DecryptionFailed(DecryptFailure.WrongPassword),
                        ),
                    )
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.generateKey(userId, password, expiration)

                val ciphertext = "encrypted".encodeToByteArray()
                val pubkey = ByteArray(32)

                orchestrator.decrypt(ciphertext, pubkey)
                (orchestrator.state.value as EnclaveFlow.WrongPasswordSuspected).attemptCount shouldBe 1

                orchestrator.retry()
                orchestrator.decrypt(ciphertext, pubkey)
                (orchestrator.state.value as EnclaveFlow.WrongPasswordSuspected).attemptCount shouldBe 2
            }

            test("UC-5.4: Encryption fails transitions to Error") {
                val error = EnclaveError.EncryptionFailed("Test failure")
                val enclave = MockEnclave(MockEnclave.MockBehavior.EncryptFails(error))
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.generateKey(userId, password, expiration)

                val message = "test".encodeToByteArray()
                val pubkey = ByteArray(32)

                orchestrator.encrypt(message, pubkey)
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.Error>()
            }

            test("UC-5.5: Key expired detected on operation") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.KeyExpired)
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.checkStatus()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.RequiresKey>()
            }
        }

        context("UC-6: Edge Cases") {
            test("UC-6.1: requireEnclave with Available state executes immediately") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.generateKey(userId, password, expiration)

                var actionExecuted = false
                val result =
                    orchestrator.requireEnclave {
                        actionExecuted = true
                    }

                result.isSuccess shouldBe true
                actionExecuted shouldBe true
            }

            test("UC-6.2: Cancel clears wrong password attempts") {
                val enclave =
                    MockEnclave(
                        MockEnclave.MockBehavior.DecryptFails(
                            EnclaveError.DecryptionFailed(DecryptFailure.WrongPassword),
                        ),
                    )
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.generateKey(userId, password, expiration)
                orchestrator.decrypt("test".encodeToByteArray(), ByteArray(32))

                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.WrongPasswordSuspected>()

                orchestrator.cancel()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.Cancelled>()

                // After cancel and regenerate, counter should be reset
                val enclave2 = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator2 = EnclaveOrchestrator(enclave2)
                orchestrator2.generateKey(userId, password, expiration)
                orchestrator2.state.value.shouldBeInstanceOf<EnclaveFlow.Available>()
            }

            test("UC-6.3: Successful operation resets wrong password counter") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.generateKey(userId, password, expiration)

                val message = "test".encodeToByteArray()
                val pubkey = ByteArray(32)

                val result = orchestrator.encrypt(message, pubkey)
                result.isSuccess shouldBe true
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.Available>()
            }

            test("UC-6.4: Pending action fails - stops execution") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                var action1Executed = false
                var action2Executed = false
                var action3Executed = false

                orchestrator.requireEnclave { action1Executed = true }
                orchestrator.requireEnclave {
                    action2Executed = true
                    throw RuntimeException("Test error")
                }
                orchestrator.requireEnclave { action3Executed = true }

                orchestrator.generateKey(userId, password, expiration)

                action1Executed shouldBe true
                action2Executed shouldBe true
                action3Executed shouldBe false // Should not execute after error
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.Error>()
            }

            test("UC-6.5: requireEnclave after Cancelled re-triggers flow") {
                val enclave = MockEnclave(MockEnclave.MockBehavior.Success)
                val orchestrator = EnclaveOrchestrator(enclave)

                orchestrator.checkStatus()
                orchestrator.cancel()
                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.Cancelled>()

                var actionExecuted = false
                orchestrator.requireEnclave { actionExecuted = true }

                orchestrator.state.value.shouldBeInstanceOf<EnclaveFlow.RequiresKey>()
                actionExecuted shouldBe false
            }
        }
    })
