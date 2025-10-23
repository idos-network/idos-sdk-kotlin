import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.idos.kwil.domain.generated.execute.AddCredential
import org.idos.kwil.domain.generated.execute.AddCredentialParams
import org.idos.kwil.domain.generated.execute.AddWallet
import org.idos.kwil.domain.generated.execute.AddWalletParams
import org.idos.kwil.domain.generated.execute.CreateAccessGrant
import org.idos.kwil.domain.generated.execute.CreateAccessGrantParams
import org.idos.kwil.domain.generated.execute.RevokeAccessGrant
import org.idos.kwil.domain.generated.execute.RevokeAccessGrantParams
import org.idos.kwil.domain.generated.view.GetCredentialOwned
import org.idos.kwil.domain.generated.view.GetCredentialOwnedParams
import org.idos.kwil.domain.generated.view.HasProfile
import org.idos.kwil.domain.generated.view.HasProfileParams
import org.idos.kwil.serialization.toMessage
import org.idos.kwil.serialization.toTransaction
import org.idos.signer.JvmEthSigner

/**
 * Serialization Integrity Tests
 *
 * These tests verify that action parameters are correctly serialized into base64 payloads.
 * Each test prints the payload for manual inspection and future regression testing.
 * When schema changes occur, these payloads will change, serving as a breaking change detector.
 */
@Suppress("ktlint:standard:max-line-length")
class SerializationIntegrityTests :
    StringSpec(
        {
            val chainId = "idos-staging"

            "hasProfile should serialize correctly" {
                val secrets = getSecrets()
                val signer = JvmEthSigner(secrets.keyPair)
                val address = "0x1234567890abcdef1234567890abcdef12345678"
                val params = HasProfileParams(address)

                val message = HasProfile.toMessage(params, signer)

                println("hasProfile payload: ${message.body.payload}")
                println("  - address: $address")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload?.value shouldBe
                    "AAAEAAAAbWFpbgsAAABoYXNfcHJvZmlsZQEARgAAAAAADwAAAAAAAAAABHRleHQAAAAAAAEAKwAAAAEweDEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg="
            }

            "getCredentialOwned should serialize correctly" {
                val secrets = getSecrets()
                val signer = JvmEthSigner(secrets.keyPair)
                val id = "550e8400-e29b-41d4-a716-446655440000"
                val params = GetCredentialOwnedParams(id)

                val message = GetCredentialOwned.toMessage(params, signer)

                println("getCredentialOwned payload: ${message.body.payload}")
                println("  - id: $id")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload?.value shouldBe
                    "AAAEAAAAbWFpbhQAAABnZXRfY3JlZGVudGlhbF9vd25lZAEALAAAAAAADwAAAAAAAAAABHV1aWQAAAAAAAEAEQAAAAFVDoQA4ptB1KcWRGZVRAAA"
            }

            "revokeAccessGrant should serialize correctly" {
                val secrets = getSecrets()
                val signer = JvmEthSigner(secrets.keyPair)
                val id = "550e8400-e29b-41d4-a716-446655440001"
                val params = RevokeAccessGrantParams(id)

                val message = RevokeAccessGrant.toTransaction(params, signer)

                println("revokeAccessGrant payload: ${message.body.payload}")
                println("  - id: $id")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload.value shouldBe
                    "AAAEAAAAbWFpbhMAAAByZXZva2VfYWNjZXNzX2dyYW50AQABACwAAAAAAA8AAAAAAAAAAAR1dWlkAAAAAAABABEAAAABVQ6EAOKbQdSnFkRmVUQAAQ=="
            }

            "addWallet should serialize correctly" {
                val secrets = getSecrets()
                val signer = JvmEthSigner(secrets.keyPair)
                val params =
                    AddWalletParams(
                        id = "550e8400-e29b-41d4-a716-446655440002",
                        address = "0x1234567890abcdef1234567890abcdef12345678",
                        publicKey = "0x04abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                        message = "Sign this message to add wallet",
                        signature = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab",
                    )

                val message = AddWallet.toTransaction(params, signer)

                println("addWallet payload: ${message.body.payload}")
                println("  - id: ${params.id}")
                println("  - address: ${params.address}")
                println("  - publicKey: ${params.publicKey}")
                println("  - message: ${params.message}")
                println("  - signature: ${params.signature}")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload.value shouldBe
                    "AAAEAAAAbWFpbgoAAABhZGRfd2FsbGV0AQAFACwAAAAAAA8AAAAAAAAAAAR1dWlkAAAAAAABABEAAAABVQ6EAOKbQdSnFkRmVUQAAkYAAAAAAA8AAAAAAAAAAAR0ZXh0AAAAAAABACsAAAABMHgxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4oAAAAAAADwAAAAAAAAAABHRleHQAAAAAAAEAhQAAAAEweDA0YWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTA7AAAAAAAPAAAAAAAAAAAEdGV4dAAAAAAAAQAgAAAAAVNpZ24gdGhpcyBtZXNzYWdlIHRvIGFkZCB3YWxsZXSgAAAAAAAPAAAAAAAAAAAEdGV4dAAAAAAAAQCFAAAAATB4YWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYg=="
            }

            "createAccessGrant should serialize correctly" {
                val secrets = getSecrets()
                val signer = JvmEthSigner(secrets.keyPair)
                val params =
                    CreateAccessGrantParams(
                        granteeWalletIdentifier = "0x1234567890abcdef1234567890abcdef12345678",
                        dataId = "550e8400-e29b-41d4-a716-446655440003",
                        lockedUntil = 1735689600,
                        contentHash = "QmYwAPJzv5CZsnA625s3Xf2nemtYgPpHdWEz79ojWnPbdG",
                        inserterType = "wallet",
                        inserterId = "0x9876543210fedcba9876543210fedcba98765432",
                    )

                val message = CreateAccessGrant.toTransaction(params, signer)

                println("createAccessGrant payload: ${message.body.payload}")
                println("  - granteeWalletIdentifier: ${params.granteeWalletIdentifier}")
                println("  - dataId: ${params.dataId}")
                println("  - lockedUntil: ${params.lockedUntil}")
                println("  - contentHash: ${params.contentHash}")
                println("  - inserterType: ${params.inserterType}")
                println("  - inserterId: ${params.inserterId}")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload.value shouldBe
                    "AAAEAAAAbWFpbhMAAABjcmVhdGVfYWNjZXNzX2dyYW50AQAGAEYAAAAAAA8AAAAAAAAAAAR0ZXh0AAAAAAABACsAAAABMHgxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4LAAAAAAADwAAAAAAAAAABHV1aWQAAAAAAAEAEQAAAAFVDoQA4ptB1KcWRGZVRAADJAAAAAAADwAAAAAAAAAABGludDgAAAAAAAEACQAAAAEAAAAAZ3SFgEoAAAAAAA8AAAAAAAAAAAR0ZXh0AAAAAAABAC8AAAABUW1Zd0FQSnp2NUNac25BNjI1czNYZjJuZW10WWdQcEhkV0V6NzlvalduUGJkRyIAAAAAAA8AAAAAAAAAAAR0ZXh0AAAAAAABAAcAAAABd2FsbGV0RgAAAAAADwAAAAAAAAAABHRleHQAAAAAAAEAKwAAAAEweDk4NzY1NDMyMTBmZWRjYmE5ODc2NTQzMjEwZmVkY2JhOTg3NjU0MzI="
            }

            "addCredential should serialize correctly" {
                val secrets = getSecrets()
                val signer = JvmEthSigner(secrets.keyPair)
                val params =
                    AddCredentialParams(
                        id = "550e8400-e29b-41d4-a716-446655440004",
                        issuerAuthPublicKey = "0x04abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                        encryptorPublicKey = "0x04fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321",
                        content = "encrypted_credential_content_base64_encoded",
                        publicNotes = "{\"type\":\"passport\",\"country\":\"US\"}",
                        publicNotesSignature = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab",
                        broaderSignature = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef12",
                    )

                val message = AddCredential.toTransaction(params, signer)

                println("addCredential payload: ${message.body.payload}")
                println("  - id: ${params.id}")
                println("  - issuerAuthPublicKey: ${params.issuerAuthPublicKey}")
                println("  - encryptorPublicKey: ${params.encryptorPublicKey}")
                println("  - content: ${params.content}")
                println("  - publicNotes: ${params.publicNotes}")
                println("  - publicNotesSignature: ${params.publicNotesSignature}")
                println("  - broaderSignature: ${params.broaderSignature}")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload.value shouldBe
                    "AAAEAAAAbWFpbg4AAABhZGRfY3JlZGVudGlhbAEABwAsAAAAAAAPAAAAAAAAAAAEdXVpZAAAAAAAAQARAAAAAVUOhADim0HUpxZEZlVEAASgAAAAAAAPAAAAAAAAAAAEdGV4dAAAAAAAAQCFAAAAATB4MDRhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MKAAAAAAAA8AAAAAAAAAAAR0ZXh0AAAAAAABAIUAAAABMHgwNGZlZGNiYTA5ODc2NTQzMjFmZWRjYmEwOTg3NjU0MzIxZmVkY2JhMDk4NzY1NDMyMWZlZGNiYTA5ODc2NTQzMjFmZWRjYmEwOTg3NjU0MzIxZmVkY2JhMDk4NzY1NDMyMWZlZGNiYTA5ODc2NTQzMjFmZWRjYmEwOTg3NjU0MzIxRwAAAAAADwAAAAAAAAAABHRleHQAAAAAAAEALAAAAAFlbmNyeXB0ZWRfY3JlZGVudGlhbF9jb250ZW50X2Jhc2U2NF9lbmNvZGVkPgAAAAAADwAAAAAAAAAABHRleHQAAAAAAAEAIwAAAAF7InR5cGUiOiJwYXNzcG9ydCIsImNvdW50cnkiOiJVUyJ9oAAAAAAADwAAAAAAAAAABHRleHQAAAAAAAEAhQAAAAEweGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWKgAAAAAAAPAAAAAAAAAAAEdGV4dAAAAAAAAQCFAAAAATB4MTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMg=="
            }

            "should detect breaking changes in payload format" {
                // This test documents the expected payload structure
                // When schema changes, this will fail and needs to be updated
                val secrets = getSecrets()
                val signer = JvmEthSigner(secrets.keyPair)
                val testId = "550e8400-e29b-41d4-a716-446655440000"
                val params = GetCredentialOwnedParams(testId)

                val message = GetCredentialOwned.toMessage(params, signer)

                // The payload should be a non-empty base64 string
                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload?.value shouldBe
                    "AAAEAAAAbWFpbhQAAABnZXRfY3JlZGVudGlhbF9vd25lZAEALAAAAAAADwAAAAAAAAAABHV1aWQAAAAAAAEAEQAAAAFVDoQA4ptB1KcWRGZVRAAA"

                // Print for baseline documentation
                println("Breaking change detector baseline:")
                println("  action: get_credential_owned")
                println("  payload: ${message.body.payload}")
                println("  input types: ${GetCredentialOwned.positionalTypes}")
                println("  input values: [$testId]")
                println("")
                println("If this test fails after schema changes, update the baseline and document the breaking change.")
            }
        },
    )
