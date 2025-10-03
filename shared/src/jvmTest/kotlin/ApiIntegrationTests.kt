import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.idos.enclave.Enclave
import org.idos.enclave.JvmEncryption
import org.idos.enclave.JvmMetadataStorage
import org.idos.kwil.KwilActionClient
import org.idos.kwil.actions.getAccessGrantsOwned
import org.idos.kwil.actions.getCredentialOwned
import org.idos.kwil.actions.getCredentials
import org.idos.kwil.actions.getUser
import org.idos.kwil.actions.getWallets
import org.idos.kwil.actions.hasUserProfile
import org.idos.kwil.actions.revokeAccessGrant
import org.idos.kwil.rpc.Base64String
import org.idos.kwil.serialization.toByteArray
import org.idos.kwil.signer.JvmEthSigner

class ApiIntegrationTests :
    StringSpec(
        {
            val chainId = "idos-staging"

            "should get chain info" {
                val secret = getSecrets()
                val signer =
                    JvmEthSigner(
                        secret.keyPair.privateKey.key
                            .toString(16),
                    )
                val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
                client.chainInfo().chainId shouldBe chainId
            }

            "should get wallets" {
                val secrets = getSecrets()
                val signer =
                    JvmEthSigner(
                        secrets.keyPair.privateKey.key
                            .toString(16),
                    )
                val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
                val accountId = "0x" + signer.getIdentifier()
                println(accountId)
                client.hasUserProfile(accountId).hasProfile shouldBe true
                val wallets = client.getWallets()
                val credentials = client.getCredentials().filter { it.publicNotes.isNotEmpty() }
                val profile = client.getUser()
                val accessGrants = client.getAccessGrantsOwned()

                wallets.asClue { it.first().address.lowercase() shouldBe accountId }

                accessGrants.firstOrNull()?.run {
                    // Print message payload for revokeAccessGrant
                    val message =
                        client.buildMessage(
                            org.idos.kwil.rpc.CallBody(
                                namespace = org.idos.kwil.actions.generated.execute.RevokeAccessGrant.namespace,
                                name = org.idos.kwil.actions.generated.execute.RevokeAccessGrant.name,
                                inputs =
                                    org.idos.kwil.actions.generated.execute.RevokeAccessGrant.toPositionalParams(
                                        org.idos.kwil.actions.generated.execute
                                            .RevokeAccessGrantParams(this.id),
                                    ),
                                types = org.idos.kwil.actions.generated.execute.RevokeAccessGrant.positionalTypes,
                            ),
                            signer,
                        )
                    println("revokeAccessGrant payload: ${message.body.payload}")

                    client.revokeAccessGrant(this.id).asClue { it shouldNotBe null }
                }
                val id = credentials.last().id

                // Print message payload for getCredentialOwned
                val credMessage =
                    client.buildMessage(
                        org.idos.kwil.rpc.CallBody(
                            namespace = org.idos.kwil.actions.generated.view.GetCredentialOwned.namespace,
                            name = org.idos.kwil.actions.generated.view.GetCredentialOwned.name,
                            inputs =
                                org.idos.kwil.actions.generated.view.GetCredentialOwned.toPositionalParams(
                                    org.idos.kwil.actions.generated.view
                                        .GetCredentialOwnedParams(id),
                                ),
                            types = org.idos.kwil.actions.generated.view.GetCredentialOwned.positionalTypes,
                        ),
                        signer,
                    )
                println("getCredentialOwned payload: ${credMessage.body.payload}")

                val data = client.getCredentialOwned(id)
                val enclave = Enclave(JvmEncryption(), JvmMetadataStorage())
                enclave.generateKey(profile.id, secrets.password, 1000)
                val content = Base64String(data.content).toByteArray()
                val pubkey = Base64String(data.encryptorPublicKey).toByteArray()
                val raw = enclave.decrypt(content, pubkey)
                val decrypt = raw.decodeToString()
                decrypt.asClue { it shouldNotBe null }

                println(wallets)
                println(credentials)
                println(profile)
                println(accessGrants)
                println(decrypt)
            }
        },
    )
