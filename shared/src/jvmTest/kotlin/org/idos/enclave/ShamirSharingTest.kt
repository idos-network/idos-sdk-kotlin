package org.idos.enclave

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.idos.crypto.BouncyCastleKeccak256
import org.idos.enclave.crypto.GF256
import org.idos.enclave.crypto.GF256Polynomial
import org.idos.enclave.crypto.ShamirSharing
import org.idos.enclave.crypto.ShareBlinding

/**
 * Tests for Shamir Secret Sharing implementation.
 *
 * These tests verify the core cryptographic operations used in the MPC enclave:
 * 1. Split secret into shares
 * 2. Blind shares for upload
 * 3. Unblind downloaded shares
 * 4. Reconstruct secret from shares
 */
class ShamirSharingTest :
    StringSpec({

        "should split and combine simple secret" {
            val secret = "password".encodeToByteArray()
            val n = 4 // Total shares
            val k = 2 // Threshold

            // Split
            val shares = ShamirSharing.splitByteWiseShamir(secret, n, k)

            shares.size shouldBe n
            shares.forEach { it.size shouldBe secret.size }

            // Combine with threshold shares
            val reconstructed = ShamirSharing.combineByteWiseShamir(shares.take(k), k)

            reconstructed.decodeToString() shouldBe "password"
        }

        "should reconstruct secret from any k shares" {
            val secret = "test-secret-123".encodeToByteArray()
            val n = 5
            val k = 3

            val shares = ShamirSharing.splitByteWiseShamir(secret, n, k)

            // Try different combinations of k shares
            val combinations =
                listOf(
                    listOf(0, 1, 2),
                    listOf(0, 2, 4),
                    listOf(1, 3, 4),
                    listOf(2, 3, 4),
                )

            combinations.forEach { indices ->
                val selectedShares = indices.map { shares[it] }
                val reconstructed = ShamirSharing.combineByteWiseShamirWithIndices(selectedShares, indices, k)
                reconstructed.decodeToString() shouldBe "test-secret-123"
            }
        }

        "should fail with insufficient shares" {
            val secret = "secret".encodeToByteArray()
            val n = 4
            val k = 3

            val shares = ShamirSharing.splitByteWiseShamir(secret, n, k)

            // Try with only 2 shares (less than threshold)
            val reconstructed = ShamirSharing.combineByteWiseShamir(shares.take(2), 2)

            // Should NOT match original (insufficient threshold)
            reconstructed.decodeToString() shouldNotBe "secret"
        }

        "should handle binary data" {
            val secret = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0xFF.toByte(), 0xAA.toByte())
            val n = 3
            val k = 2

            val shares = ShamirSharing.splitByteWiseShamir(secret, n, k)
            val reconstructed = ShamirSharing.combineByteWiseShamir(shares, k)

            reconstructed shouldBe secret
        }

        "should simulate full enclave workflow: split -> blind -> unblind -> combine" {
            // Simulate MPC enclave behavior without encryption
            val plaintext = "my-secret-password".encodeToByteArray()
            val n = 4 // Total nodes
            val k = 3 // Threshold (need 3 shares to reconstruct)

            println("\n=== Full Enclave Simulation ===")
            println("Secret: ${plaintext.decodeToString()}")
            println("Config: n=$n, k=$k")

            // Step 1: Split secret into shares (Upload phase)
            val rawShares = ShamirSharing.splitByteWiseShamir(plaintext, n, k)
            println("\n1. Split into $n shares (${rawShares[0].size} bytes each)")

            // Step 2: Blind shares before upload
            val blindedShares = rawShares.map { ShareBlinding.blind(it) }
            println("2. Blinded shares (${blindedShares[0].size} bytes each, +32 bytes)")

            // Verify blinding added 32 bytes
            blindedShares.forEach { blinded ->
                blinded.size shouldBe rawShares[0].size + 32
            }

            // Step 3: Simulate download - take k shares and unblind
            val downloadedShares = blindedShares.take(k)
            val unblindedShares = downloadedShares.map { ShareBlinding.unblind(it) }
            println("3. Downloaded and unblinded $k shares")

            // Verify unblinding restored original size
            unblindedShares.forEach { unblinded ->
                unblinded.size shouldBe rawShares[0].size
            }

            // Step 4: Reconstruct secret from unblinded shares
            val reconstructed = ShamirSharing.combineByteWiseShamir(unblindedShares, k)
            println("4. Reconstructed secret: ${reconstructed.decodeToString()}")

            // Verify reconstruction
            reconstructed shouldBe plaintext
            reconstructed.decodeToString() shouldBe "my-secret-password"

            println("\n✓ Full workflow successful!")
        }

        "should compute share commitments" {
            val hasher = BouncyCastleKeccak256()
            val secret = "test".encodeToByteArray()
            val n = 3
            val k = 2

            // Split and blind
            val rawShares = ShamirSharing.splitByteWiseShamir(secret, n, k)
            val blindedShares = rawShares.map { ShareBlinding.blind(it) }

            // Compute commitments for upload
            val commitments = blindedShares.map { ShareBlinding.computeCommitment(hasher, it) }

            println("\n=== Share Commitments ===")
            commitments.forEachIndexed { index, commitment ->
                println("Share $index: $commitment")
                commitment shouldBe commitment.lowercase() // Should be lowercase hex
                commitment.startsWith("0x") shouldBe true
                commitment.length shouldBe 66 // "0x" + 64 hex chars (32 bytes)
            }
        }

        "should handle edge case: threshold equals total shares" {
            val secret = "edge-case".encodeToByteArray()
            val n = 3
            val k = 3 // All shares required

            val shares = ShamirSharing.splitByteWiseShamir(secret, n, k)
            val reconstructed = ShamirSharing.combineByteWiseShamir(shares, k)

            reconstructed.decodeToString() shouldBe "edge-case"
        }

        "should handle single byte secret" {
            val secret = byteArrayOf(0x42)
            val n = 3
            val k = 2

            val shares = ShamirSharing.splitByteWiseShamir(secret, n, k)
            val reconstructed = ShamirSharing.combineByteWiseShamir(shares, k)

            reconstructed shouldBe secret
        }

        "should handle empty secret" {
            val secret = byteArrayOf()
            val n = 3
            val k = 2

            val shares = ShamirSharing.splitByteWiseShamir(secret, n, k)

            shares.size shouldBe n
            shares.forEach { it.size shouldBe 0 }

            val reconstructed = ShamirSharing.combineByteWiseShamir(shares, k)
            reconstructed shouldBe secret
        }

        "should verify different shares produce different values" {
            val secret = "unique".encodeToByteArray()
            val n = 4
            val k = 2

            val shares = ShamirSharing.splitByteWiseShamir(secret, n, k)

            // All shares should be different from each other
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    shares[i] shouldNotBe shares[j]
                }
            }

            // No share should equal the original secret
            shares.forEach { share ->
                share shouldNotBe secret
            }
        }

        "should test GF256 field operations" {
            // Test basic field properties
            val zero = GF256.ZERO
            val one = GF256.ONE

            // Additive identity
            (zero + zero) shouldBe zero
            (one + zero) shouldBe one

            // Multiplicative identity
            (one * one) shouldBe one
            (one * zero) shouldBe zero

            // Test some specific GF(2^8) properties
            val a = GF256.fromInt(5)
            val b = GF256.fromInt(7)

            // Addition is XOR
            (a + b).value shouldBe (5 xor 7)

            // Subtraction equals addition in GF(2^8)
            (a - b) shouldBe (a + b)

            // Multiplicative inverse
            val aInv = a.inverse()
            (a * aInv) shouldBe one
        }

        "should test GF256 polynomial evaluation" {
            // Test polynomial f(x) = 1 + 2x + 3x^2
            val coeffs =
                listOf(
                    GF256.fromInt(1),
                    GF256.fromInt(2),
                    GF256.fromInt(3),
                )
            val poly = GF256Polynomial.fromCoefficients(coeffs)

            // Evaluate at x=0 should give constant term
            poly.evaluate(GF256.ZERO) shouldBe GF256.fromInt(1)

            // Evaluate at x=1 should give 1 + 2 + 3 (in GF(2^8))
            val atOne = poly.evaluate(GF256.ONE)
            val expected = GF256.fromInt(1) + GF256.fromInt(2) + GF256.fromInt(3)
            atOne shouldBe expected
        }

        "should simulate realistic MPC scenario with all 4 nodes" {
            val secret = "ethereum-private-key-data".encodeToByteArray()
            val n = 4
            val k = 3

            println("\n=== Realistic MPC Scenario ===")
            println("Secret size: ${secret.size} bytes")

            // Upload: split and blind
            val shares = ShamirSharing.splitByteWiseShamir(secret, n, k)
            val blindedShares = shares.map { ShareBlinding.blind(it) }

            println("Created $n blinded shares of ${blindedShares[0].size} bytes each")

            // Download: simulate getting k shares from different nodes
            val nodeIndices = listOf(0, 1, 3) // Nodes 1, 2, and 4
            val downloadedBlindedShares = nodeIndices.map { blindedShares[it] }
            val downloadedShares = downloadedBlindedShares.map { ShareBlinding.unblind(it) }

            println("Downloaded $k shares from nodes: ${nodeIndices.map { it + 1 }}")

            // Reconstruct
            val reconstructed = ShamirSharing.combineByteWiseShamirWithIndices(downloadedShares, nodeIndices, k)

            reconstructed shouldBe secret
            println("✓ Successfully reconstructed secret")
        }
    })
