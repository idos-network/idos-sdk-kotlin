package org.idos.signer.eip712

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.idos.crypto.eip712.Eip712Utils
import org.idos.crypto.eip712.TypedData
import org.idos.crypto.eip712.TypedDataDomain
import org.idos.crypto.eip712.TypedDataField
import org.idos.signer.JvmEthSigner
import org.kethereum.crypto.CryptoAPI
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toECKeyPair
import org.kethereum.model.PrivateKey
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData

/**
 * Test cases for EIP-712 implementation.
 *
 * These tests verify our manual EIP-712 implementation produces correct hashes
 * according to the EIP-712 specification.
 *
 * Reference: https://eips.ethereum.org/EIPS/eip-712
 */

class Eip712UtilsTest :
    StringSpec({

        val hasher = BouncyCastleKeccak256()

        "should hash simple EIP-712 domain" {
            val domain =
                TypedDataDomain(
                    name = "Test Domain",
                    version = "1",
                    verifyingContract = "0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC",
                )

            val hash = Eip712Utils.hashDomain(hasher, domain)

            // Should produce 32-byte hash
            hash.size shouldBe 32
            // Should be deterministic
            val hash2 = Eip712Utils.hashDomain(hasher, domain)
            hash.toHexString() shouldBe hash2.toHexString()
        }

        "should encode type string correctly" {
            val types =
                mapOf(
                    "Person" to
                        listOf(
                            TypedDataField("name", "string"),
                            TypedDataField("wallet", "address"),
                        ),
                )

            val typeString = Eip712Utils.encodeType("Person", types)

            typeString shouldBe "Person(string name,address wallet)"
        }

        "should encode type with dependencies" {
            val types =
                mapOf(
                    "Mail" to
                        listOf(
                            TypedDataField("from", "Person"),
                            TypedDataField("to", "Person"),
                            TypedDataField("contents", "string"),
                        ),
                    "Person" to
                        listOf(
                            TypedDataField("name", "string"),
                            TypedDataField("wallet", "address"),
                        ),
                )

            val typeString = Eip712Utils.encodeType("Mail", types)

            // Dependencies should be sorted alphabetically after primary type
            typeString shouldBe "Mail(Person from,Person to,string contents)Person(string name,address wallet)"
        }

        "should hash EIP-712 example from spec" {
            // Example from EIP-712 specification
            // Reference: https://eips.ethereum.org/assets/eip-712/Example.js
            val domain =
                TypedDataDomain(
                    name = "Ether Mail",
                    version = "1",
                    chainId = 1,
                    verifyingContract = "0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC",
                )

            val types =
                mapOf(
                    "Person" to
                        listOf(
                            TypedDataField("name", "string"),
                            TypedDataField("wallet", "address"),
                        ),
                    "Mail" to
                        listOf(
                            TypedDataField("from", "Person"),
                            TypedDataField("to", "Person"),
                            TypedDataField("contents", "string"),
                        ),
                )

            val message =
                buildJsonObject {
                    putJsonObject("from") {
                        put("name", "Cow")
                        put("wallet", "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826")
                    }
                    putJsonObject("to") {
                        put("name", "Bob")
                        put("wallet", "0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB")
                    }
                    put("contents", "Hello, Bob!")
                }

            val typedData =
                TypedData(
                    domain = domain,
                    types = types,
                    primaryType = "Mail",
                    message = message,
                )

            // Expected struct hash for domain
            val domainHash = Eip712Utils.hashDomain(hasher, domain)
            val expectedDomainHash = "f2cee375fa42b42143804025fc449deafd50cc031ca257e0b194a650a912090f"
            domainHash.toHexString() shouldBe expectedDomainHash

            // Expected struct hash for message
            val messageStructHash = Eip712Utils.hashStruct(hasher, "Mail", types, message)
            val expectedMessageHash = "c52c0ee5d84264471806290a3f2c4cecfc5490626bf912d01f240d7a274b371e"
            messageStructHash.toHexString() shouldBe expectedMessageHash

            // Final sign hash
            val hash = Eip712Utils.hashTypedData(hasher, typedData)

            // Should produce 32-byte hash
            hash.size shouldBe 32

            // Verify it's deterministic
            val hash2 = Eip712Utils.hashTypedData(hasher, typedData)
            hash.toHexString() shouldBe hash2.toHexString()

            // Expected hash from EIP-712 spec example
            val expectedHash = "be609aee343fb3c4b28e1df9e632fca64fcfaede20f02e86244efddf30957bd2"
            hash.toHexString() shouldBe expectedHash
        }

        "should handle uint64 values" {
            val domain =
                TypedDataDomain(
                    name = "Test",
                    version = "1",
                    verifyingContract = "0x0000000000000000000000000000000000000001",
                )

            val types =
                mapOf(
                    "Message" to
                        listOf(
                            TypedDataField("timestamp", "uint64"),
                            TypedDataField("amount", "uint64"),
                        ),
                )

            val message =
                buildJsonObject {
                    put("timestamp", 1234567890)
                    put("amount", 1000000)
                }

            val typedData =
                TypedData(
                    domain = domain,
                    types = types,
                    primaryType = "Message",
                    message = message,
                )

            val hash = Eip712Utils.hashTypedData(hasher, typedData)
            hash.size shouldBe 32

            println("Uint64 message hash: 0x${hash.toHexString()}")
        }

        "should handle bytes32 arrays" {
            val domain =
                TypedDataDomain(
                    name = "MPC Test",
                    version = "1",
                    verifyingContract = "0x0000000000000000000000000000000000000001",
                )

            val types =
                mapOf(
                    "Upload" to
                        listOf(
                            TypedDataField("share_commitments", "bytes32[]"),
                            TypedDataField("recovering_addresses", "string[]"),
                        ),
                )

            val message =
                buildJsonObject {
                    putJsonArray("share_commitments") {
                        add(JsonPrimitive("0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"))
                        add(JsonPrimitive("0xfedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321"))
                    }
                    putJsonArray("recovering_addresses") {
                        add(JsonPrimitive("eip712:0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"))
                    }
                }

            val typedData =
                TypedData(
                    domain = domain,
                    types = types,
                    primaryType = "Upload",
                    message = message,
                )

            val hash = Eip712Utils.hashTypedData(hasher, typedData)
            hash.size shouldBe 32

            println("MPC Upload hash: 0x${hash.toHexString()}")
        }

        "should handle string arrays" {
            val domain =
                TypedDataDomain(
                    name = "Test",
                    version = "1",
                    verifyingContract = "0x0000000000000000000000000000000000000001",
                )

            val types =
                mapOf(
                    "Message" to
                        listOf(
                            TypedDataField("addresses", "string[]"),
                        ),
                )

            val message =
                buildJsonObject {
                    putJsonArray("addresses") {
                        add(JsonPrimitive("eip712:0x123"))
                        add(JsonPrimitive("NEAR:abc.near"))
                        add(JsonPrimitive("XRPL:rxyz"))
                    }
                }

            val typedData =
                TypedData(
                    domain = domain,
                    types = types,
                    primaryType = "Message",
                    message = message,
                )

            val hash = Eip712Utils.hashTypedData(hasher, typedData)
            hash.size shouldBe 32
        }

        "should handle address type" {
            val types =
                mapOf(
                    "Test" to
                        listOf(
                            TypedDataField("addr", "address"),
                        ),
                )

            val message =
                buildJsonObject {
                    put("addr", "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826")
                }

            val encoded = Eip712Utils.encodeData(hasher, "Test", types, message)

            // Address should be encoded as 32 bytes (left-padded)
            encoded.size shouldBe 32
        }

        "should handle boolean type" {
            val types =
                mapOf(
                    "Test" to
                        listOf(
                            TypedDataField("flag", "bool"),
                        ),
                )

            val messageTrue =
                buildJsonObject {
                    put("flag", true)
                }

            val messageFalse =
                buildJsonObject {
                    put("flag", false)
                }

            val encodedTrue = Eip712Utils.encodeData(hasher, "Test", types, messageTrue)
            val encodedFalse = Eip712Utils.encodeData(hasher, "Test", types, messageFalse)

            // Should be 32 bytes
            encodedTrue.size shouldBe 32
            encodedFalse.size shouldBe 32

            // True should have 1 at the end, false should be all zeros
            encodedTrue[31] shouldBe 1.toByte()
            encodedFalse.all { it == 0.toByte() } shouldBe true
        }

        "should sign EIP-712 typed data and recover signer address" {
            // Example from EIP-712 specification with known private key
            // Reference: https://eips.ethereum.org/assets/eip-712/Example.js
            val domain =
                TypedDataDomain(
                    name = "Ether Mail",
                    version = "1",
                    chainId = 1,
                    verifyingContract = "0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC",
                )

            val types =
                mapOf(
                    "Person" to
                        listOf(
                            TypedDataField("name", "string"),
                            TypedDataField("wallet", "address"),
                        ),
                    "Mail" to
                        listOf(
                            TypedDataField("from", "Person"),
                            TypedDataField("to", "Person"),
                            TypedDataField("contents", "string"),
                        ),
                )

            val message =
                buildJsonObject {
                    putJsonObject("from") {
                        put("name", "Cow")
                        put("wallet", "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826")
                    }
                    putJsonObject("to") {
                        put("name", "Bob")
                        put("wallet", "0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB")
                    }
                    put("contents", "Hello, Bob!")
                }

            val typedData =
                TypedData(
                    domain = domain,
                    types = types,
                    primaryType = "Mail",
                    message = message,
                )

            // Hash the typed data
            val hash = Eip712Utils.hashTypedData(hasher, typedData)
            val expectedHash = "be609aee343fb3c4b28e1df9e632fca64fcfaede20f02e86244efddf30957bd2"
            hash.toHexString() shouldBe expectedHash

            // Private key from the EIP-712 example
            val privateKeyHex = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4"

            // Create signer and sign the hash
            val signer = JvmEthSigner(PrivateKey(privateKeyHex.hexToByteArray()).toECKeyPair())
            val signature = SignatureData.fromHex(signer.signTypedData(typedData))

            // Expected signer address from the example
            val expectedSignerAddress = "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826".lowercase()
            signer.address.hex shouldBe expectedSignerAddress

            val recId = signature.v.toInt() - 27
            val sig = ECDSASignature(signature.r, signature.s)
            val recovered = CryptoAPI.signer.recover(recId, sig, hash)?.let { PublicKey(it).toAddress().hex }
            recovered shouldBe expectedSignerAddress
        }
    })
