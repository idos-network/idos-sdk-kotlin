package org.idos.enclave

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.idos.crypto.BouncyCastleKeccak256
import org.idos.enclave.crypto.JvmEncryption
import org.idos.enclave.mpc.DownloadRequest
import org.idos.enclave.mpc.MpcClient
import org.idos.enclave.mpc.MpcConfig
import org.idos.enclave.mpc.PartisiaRpcClient
import org.idos.enclave.mpc.getFormattedAddress
import org.idos.enclave.mpc.signMessageAsAuthHeader
import org.idos.getCurrentTimeMillis
import org.idos.kwil.types.Base64String
import org.idos.signer.JvmEthSigner
import org.kethereum.crypto.toECKeyPair
import org.kethereum.model.PrivateKey

class MpcIntegrationTest :
    FunSpec({
        context("Partisia chain integration") {
            val url = "https://partisia-reader-node.playground.idos.network:8080"
            val address = "025f6d71e82b9396e09e20c93f660d0ae36ebb4a68"

            test("Get contract state") {
                val client = PartisiaRpcClient(url, address)
                client.getState()
                // just a smoke test
            }

            test("decode contract base64 serialized data") {
                val base64data =
                    "BAAAAAAsM1QA6MfDKkK6HMYtPBDkAaff7SwAAABodHRwczovL2FsYW4ubm9kZXMuc3RhZ2luZy5pZG9zLm5ldHdvcms6ODQwMAAv1IKM0v2SlGZRQm4KgkuvxUQIcC8AAABodHRwczovL2JhcmJhcmEubm9kZXMuc3RhZ2luZy5pZG9zLm5ldHdvcms6ODQwMAD3SVCbKdLLZ2brG6EBVgKHFTSHRy8AAABodHRwczovL2NoYXJsZXMubm9kZXMuc3RhZ2luZy5pZG9zLm5ldHdvcms6ODQwMADr7jnCXMryiKfNWwNyYYPo1vHnay8AAABodHRwczovL2RvdWdsYXMubm9kZXMuc3RhZ2luZy5pZG9zLm5ldHdvcms6ODQwMAC3DHp/D+A1gDpTuCklPjRR+PfwrQ=="
                val nodes = PartisiaRpcClient.readContractState(Base64String(base64data))
                nodes.forEach {
                    println("Address: ${it.address}")
                    println("Endpoint: ${it.url}")
                    println("-----")
                }

                nodes.size shouldBe 4
                nodes.forEach { (address, url) ->
                    address.isEmpty() shouldBe false
                    address.hexToByteArray().isEmpty() shouldBe false
                    url.isEmpty() shouldBe false
                }
            }

            test("Fetch shares from MPC") {
                val pubkey = "cf5210204ccd03621807747be6105a0e779747fb"
//                val userId = "d3a5ca37-59c8-4fad-8303-fc17cc503ec1"
                val userId = "test-id-2"
                val private = PrivateKey("304ad494f0d59f0c1d48f01dfacda9becbcb0e5cb9f9a460f107eae5f8cc0890".hexToByteArray())
                val signer = JvmEthSigner(private.toECKeyPair())
                val encryption = JvmEncryption()
                val hasher = BouncyCastleKeccak256()
                val config = MpcConfig(4, 2, 2)
                val mpc = MpcClient(url, address, encryption, hasher, config)
                mpc.initialize()

                mpc.nodeClients.size shouldBe config.totalNodes
                signer.getIdentifier() shouldBe pubkey

                val ephemeralKeyPair = encryption.generateEphemeralKeyPair()

                val request =
                    DownloadRequest(
                        recoveringAddress = signer.getFormattedAddress(),
                        timestamp = getCurrentTimeMillis(),
                        publicKey = "0x${ephemeralKeyPair.publicKey.toHexString()}",
                    )

                // Sign the request
                val signableMessage = request.toSignableMessage(address)
                val signature = signer.signMessageAsAuthHeader(signableMessage)

                // Download secret from nodes
                val secret = mpc.downloadSecret(userId, request, signature, ephemeralKeyPair.secretKey)

                secret?.decodeToString() shouldBe "Marian"
            }
        }
    })
