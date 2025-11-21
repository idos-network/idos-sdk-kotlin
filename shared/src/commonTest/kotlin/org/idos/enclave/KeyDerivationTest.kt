package org.idos.enclave

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.idos.enclave.crypto.KeyDerivation

open class KeyDerivationTest :
    StringSpec({
        "should derive key with valid input" {
            // Test with known values that produce consistent output across platforms
            val password = "test-password-123!@#"
            val salt = "550e8400-e29b-41d4-a716-446655440000" // Valid UUID

            val expectedHash = "9de171c0abc1cb2e4ff4078605d9edaa7175cb66419b6151fa65c4ab774ff3b6"

            val result =
                shouldNotThrowAny {
                    KeyDerivation.deriveKey(password, salt, 0.1)
                }

            result.toHexString() shouldBe expectedHash
        }

        "should throw exception with invalid salt" {
            val password = "test-password"
            val invalidSalt = "not-a-valid-uuid"

            shouldThrow<IllegalArgumentException> {
                KeyDerivation.deriveKey(password, invalidSalt, 0.1)
            }
        }
    })
