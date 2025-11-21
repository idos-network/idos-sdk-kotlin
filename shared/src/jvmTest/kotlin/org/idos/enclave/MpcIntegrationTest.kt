@file:Suppress("ktlint:standard:max-line-length")

package org.idos.enclave

import getSecrets
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.idos.IdosClient
import org.idos.enclave.crypto.JvmEncryption
import org.idos.enclave.mpc.DownloadRequest
import org.idos.enclave.mpc.MpcClient
import org.idos.enclave.mpc.MpcConfig
import org.idos.enclave.mpc.PartisiaRpcClient
import org.idos.enclave.mpc.getFormattedAddress
import org.idos.enclave.mpc.signMessageAsAuthHeader
import org.idos.get
import org.idos.getCurrentTimeMillis
import org.idos.kwil.types.Base64String
import org.idos.signer.JvmEthSigner

class MpcIntegrationTest :
    FunSpec({
        context("Partisia chain integration") {
            val chainId = "kwil-testnet"
            val api = "https://nodes.playground.idos.network"

            val url = "https://partisia-reader-node.playground.idos.network:8080"
            val address = "0223996d84146dbf310dd52a0e1d103e91bb8402b3"

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
                val secrets = getSecrets()

                val signer = JvmEthSigner(secrets.keyPair)
                val client = IdosClient.create(api, chainId, signer)
                val encryption = JvmEncryption()
                val config = MpcConfig(url, address, totalNodes = 6, threshold = 4, maliciousNodes = 2)
                val mpc = MpcClient(encryption, config)
                val userId = client.users.get().id


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

                secret.decodeToString() shouldBe "Pkc%#sB2R|Th10>)ASZ9"
            }
        }
    })
