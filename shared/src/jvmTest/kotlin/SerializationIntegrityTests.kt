import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.idos.kwil.KwilActionClient
import org.idos.kwil.actions.generated.execute.AddCredential
import org.idos.kwil.actions.generated.execute.AddCredentialParams
import org.idos.kwil.actions.generated.execute.AddWallet
import org.idos.kwil.actions.generated.execute.AddWalletParams
import org.idos.kwil.actions.generated.execute.CreateAccessGrant
import org.idos.kwil.actions.generated.execute.CreateAccessGrantParams
import org.idos.kwil.actions.generated.execute.RevokeAccessGrant
import org.idos.kwil.actions.generated.execute.RevokeAccessGrantParams
import org.idos.kwil.actions.generated.view.GetCredentialOwned
import org.idos.kwil.actions.generated.view.GetCredentialOwnedParams
import org.idos.kwil.actions.generated.view.HasProfile
import org.idos.kwil.actions.generated.view.HasProfileParams
import org.idos.kwil.rpc.CallBody
import org.idos.kwil.rpc.UuidString
import org.idos.kwil.signer.JvmEthSigner

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
                val signer =
                    JvmEthSigner(
                        secrets.keyPair.privateKey.key
                            .toString(16),
                    )
                val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
                client.ensureAuthenticationMode()

                val address = "0x1234567890abcdef1234567890abcdef12345678"
                val params = HasProfileParams(address)

                val message =
                    client.buildMessage(
                        CallBody(
                            namespace = HasProfile.namespace,
                            name = HasProfile.name,
                            inputs = HasProfile.toPositionalParams(params),
                            types = HasProfile.positionalTypes,
                        ),
                        signer,
                    )

                println("hasProfile payload: ${message.body.payload}")
                println("  - address: $address")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload?.value shouldBe
                    "AAAEAAAAbWFpbgsAAABoYXNfcHJvZmlsZQEARgAAAAAADwAAAAAAAAAABHRleHQAAAAAAAEAKwAAAAEweDEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg="
            }

            "getCredentialOwned should serialize correctly" {
                val secrets = getSecrets()
                val signer =
                    JvmEthSigner(
                        secrets.keyPair.privateKey.key
                            .toString(16),
                    )
                val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
                client.ensureAuthenticationMode()

                val id = UuidString("550e8400-e29b-41d4-a716-446655440000")
                val params = GetCredentialOwnedParams(id)

                val message =
                    client.buildMessage(
                        CallBody(
                            namespace = GetCredentialOwned.namespace,
                            name = GetCredentialOwned.name,
                            inputs = GetCredentialOwned.toPositionalParams(params),
                            types = GetCredentialOwned.positionalTypes,
                        ),
                        signer,
                    )

                println("getCredentialOwned payload: ${message.body.payload}")
                println("  - id: ${id.value}")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload?.value shouldBe
                    "AAAEAAAAbWFpbhQAAABnZXRfY3JlZGVudGlhbF9vd25lZAEALAAAAAAADwAAAAAAAAAABHV1aWQAAAAAAAEAEQAAAAFVDoQA4ptB1KcWRGZVRAAA"
            }

            "revokeAccessGrant should serialize correctly" {
                val secrets = getSecrets()
                val signer =
                    JvmEthSigner(
                        secrets.keyPair.privateKey.key
                            .toString(16),
                    )
                val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
                client.ensureAuthenticationMode()

                val id = UuidString("550e8400-e29b-41d4-a716-446655440001")
                val params = RevokeAccessGrantParams(id)

                val message =
                    client.buildMessage(
                        CallBody(
                            namespace = RevokeAccessGrant.namespace,
                            name = RevokeAccessGrant.name,
                            inputs = RevokeAccessGrant.toPositionalParams(params),
                            types = RevokeAccessGrant.positionalTypes,
                        ),
                        signer,
                    )

                println("revokeAccessGrant payload: ${message.body.payload}")
                println("  - id: ${id.value}")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload?.value shouldBe
                    "AAAEAAAAbWFpbhMAAAByZXZva2VfYWNjZXNzX2dyYW50AQAsAAAAAAAPAAAAAAAAAAAEdXVpZAAAAAAAAQARAAAAAVUOhADim0HUpxZEZlVEAAE="
            }

            "addWallet should serialize correctly" {
                val secrets = getSecrets()
                val signer =
                    JvmEthSigner(
                        secrets.keyPair.privateKey.key
                            .toString(16),
                    )
                val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
                client.ensureAuthenticationMode()

                val params =
                    AddWalletParams(
                        id = UuidString("550e8400-e29b-41d4-a716-446655440002"),
                        address = "0x1234567890abcdef1234567890abcdef12345678",
                        publicKey = "0x04abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                        message = "Sign this message to add wallet",
                        signature = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab",
                    )

                val message =
                    client.buildMessage(
                        CallBody(
                            namespace = AddWallet.namespace,
                            name = AddWallet.name,
                            inputs = AddWallet.toPositionalParams(params),
                            types = AddWallet.positionalTypes,
                        ),
                        signer,
                    )

                println("addWallet payload: ${message.body.payload}")
                println("  - id: ${params.id.value}")
                println("  - address: ${params.address}")
                println("  - publicKey: ${params.publicKey}")
                println("  - message: ${params.message}")
                println("  - signature: ${params.signature}")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload?.value shouldBe
                    "AAAEAAAAbWFpbgoAAABhZGRfd2FsbGV0BQAsAAAAAAAPAAAAAAAAAAAEdXVpZAAAAAAAAQARAAAAAVUOhADim0HUpxZEZlVEAAJGAAAAAAAPAAAAAAAAAAAEdGV4dAAAAAAAAQArAAAAATB4MTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3OKAAAAAAAA8AAAAAAAAAAAR0ZXh0AAAAAAABAIUAAAABMHgwNGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwOwAAAAAADwAAAAAAAAAABHRleHQAAAAAAAEAIAAAAAFTaWduIHRoaXMgbWVzc2FnZSB0byBhZGQgd2FsbGV0oAAAAAAADwAAAAAAAAAABHRleHQAAAAAAAEAhQAAAAEweGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWI="
            }

            "createAccessGrant should serialize correctly" {
                val secrets = getSecrets()
                val signer =
                    JvmEthSigner(
                        secrets.keyPair.privateKey.key
                            .toString(16),
                    )
                val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
                client.ensureAuthenticationMode()

                val params =
                    CreateAccessGrantParams(
                        granteeWalletIdentifier = "0x1234567890abcdef1234567890abcdef12345678",
                        dataId = UuidString("550e8400-e29b-41d4-a716-446655440003"),
                        lockedUntil = 1735689600,
                        contentHash = "QmYwAPJzv5CZsnA625s3Xf2nemtYgPpHdWEz79ojWnPbdG",
                        inserterType = "wallet",
                        inserterId = "0x9876543210fedcba9876543210fedcba98765432",
                    )

                val message =
                    client.buildMessage(
                        CallBody(
                            namespace = CreateAccessGrant.namespace,
                            name = CreateAccessGrant.name,
                            inputs = CreateAccessGrant.toPositionalParams(params),
                            types = CreateAccessGrant.positionalTypes,
                        ),
                        signer,
                    )

                println("createAccessGrant payload: ${message.body.payload}")
                println("  - granteeWalletIdentifier: ${params.granteeWalletIdentifier}")
                println("  - dataId: ${params.dataId.value}")
                println("  - lockedUntil: ${params.lockedUntil}")
                println("  - contentHash: ${params.contentHash}")
                println("  - inserterType: ${params.inserterType}")
                println("  - inserterId: ${params.inserterId}")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload?.value shouldBe
                    "AAAEAAAAbWFpbhMAAABjcmVhdGVfYWNjZXNzX2dyYW50BgBGAAAAAAAPAAAAAAAAAAAEdGV4dAAAAAAAAQArAAAAATB4MTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3OCwAAAAAAA8AAAAAAAAAAAR1dWlkAAAAAAABABEAAAABVQ6EAOKbQdSnFkRmVUQAAyQAAAAAAA8AAAAAAAAAAARpbnQ4AAAAAAABAAkAAAABAAAAAGd0hYBKAAAAAAAPAAAAAAAAAAAEdGV4dAAAAAAAAQAvAAAAAVFtWXdBUEp6djVDWnNuQTYyNXMzWGYybmVtdFlnUHBIZFdFejc5b2pXblBiZEciAAAAAAAPAAAAAAAAAAAEdGV4dAAAAAAAAQAHAAAAAXdhbGxldEYAAAAAAA8AAAAAAAAAAAR0ZXh0AAAAAAABACsAAAABMHg5ODc2NTQzMjEwZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMy"
            }

            "addCredential should serialize correctly" {
                val secrets = getSecrets()
                val signer =
                    JvmEthSigner(
                        secrets.keyPair.privateKey.key
                            .toString(16),
                    )
                val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
                client.ensureAuthenticationMode()

                val params =
                    AddCredentialParams(
                        id = UuidString("550e8400-e29b-41d4-a716-446655440004"),
                        issuerAuthPublicKey = "0x04abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                        encryptorPublicKey = "0x04fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321",
                        content = "encrypted_credential_content_base64_encoded",
                        publicNotes = "{\"type\":\"passport\",\"country\":\"US\"}",
                        publicNotesSignature = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab",
                        broaderSignature = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef12",
                    )

                val message =
                    client.buildMessage(
                        CallBody(
                            namespace = AddCredential.namespace,
                            name = AddCredential.name,
                            inputs = AddCredential.toPositionalParams(params),
                            types = AddCredential.positionalTypes,
                        ),
                        signer,
                    )

                println("addCredential payload: ${message.body.payload}")
                println("  - id: ${params.id.value}")
                println("  - issuerAuthPublicKey: ${params.issuerAuthPublicKey}")
                println("  - encryptorPublicKey: ${params.encryptorPublicKey}")
                println("  - content: ${params.content}")
                println("  - publicNotes: ${params.publicNotes}")
                println("  - publicNotesSignature: ${params.publicNotesSignature}")
                println("  - broaderSignature: ${params.broaderSignature}")

                message.body.payload shouldNotBe null
                message.body.payload shouldNotBe ""
                message.body.payload?.value shouldBe
                    "AAAEAAAAbWFpbg4AAABhZGRfY3JlZGVudGlhbAcALAAAAAAADwAAAAAAAAAABHV1aWQAAAAAAAEAEQAAAAFVDoQA4ptB1KcWRGZVRAAEoAAAAAAADwAAAAAAAAAABHRleHQAAAAAAAEAhQAAAAEweDA0YWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTCgAAAAAAAPAAAAAAAAAAAEdGV4dAAAAAAAAQCFAAAAATB4MDRmZWRjYmEwOTg3NjU0MzIxZmVkY2JhMDk4NzY1NDMyMWZlZGNiYTA5ODc2NTQzMjFmZWRjYmEwOTg3NjU0MzIxZmVkY2JhMDk4NzY1NDMyMWZlZGNiYTA5ODc2NTQzMjFmZWRjYmEwOTg3NjU0MzIxZmVkY2JhMDk4NzY1NDMyMUcAAAAAAA8AAAAAAAAAAAR0ZXh0AAAAAAABACwAAAABZW5jcnlwdGVkX2NyZWRlbnRpYWxfY29udGVudF9iYXNlNjRfZW5jb2RlZD4AAAAAAA8AAAAAAAAAAAR0ZXh0AAAAAAABACMAAAABeyJ0eXBlIjoicGFzc3BvcnQiLCJjb3VudHJ5IjoiVVMifaAAAAAAAA8AAAAAAAAAAAR0ZXh0AAAAAAABAIUAAAABMHhhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFioAAAAAAADwAAAAAAAAAABHRleHQAAAAAAAEAhQAAAAEweDEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTIzNDU2Nzg5MGFiY2RlZjEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmMTI="
            }

            "should detect breaking changes in payload format" {
                // This test documents the expected payload structure
                // When schema changes, this will fail and needs to be updated
                val secrets = getSecrets()
                val signer =
                    JvmEthSigner(
                        secrets.keyPair.privateKey.key
                            .toString(16),
                    )
                val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
                client.ensureAuthenticationMode()

                val testId = UuidString("550e8400-e29b-41d4-a716-446655440000")
                val params = GetCredentialOwnedParams(testId)

                val message =
                    client.buildMessage(
                        CallBody(
                            namespace = GetCredentialOwned.namespace,
                            name = GetCredentialOwned.name,
                            inputs = GetCredentialOwned.toPositionalParams(params),
                            types = GetCredentialOwned.positionalTypes,
                        ),
                        signer,
                    )

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
                println("  input values: [${testId.value}]")
                println("")
                println("If this test fails after schema changes, update the baseline and document the breaking change.")
            }
        },
    )
