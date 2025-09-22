import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.idos.enclave.Enclave
import org.idos.kwil.KwilActionClient
import org.idos.kwil.actions.generated.main.GetAccessGrantsForCredential
import org.idos.kwil.actions.generated.main.GetAccessGrantsForCredentialInput
import org.idos.kwil.actions.getAccessGrantsOwned
import org.idos.kwil.actions.getCredentialOwned
import org.idos.kwil.actions.getCredentials
import org.idos.kwil.actions.getUser
import org.idos.kwil.actions.getWallets
import org.idos.kwil.actions.hasProfile
import org.idos.kwil.actions.revokeAccessGrant
import org.idos.kwil.rpc.Base64String
import org.idos.kwil.serialization.toByteArray
import org.idos.kwil.signer.createEthSigner

class ApiIntegrationTests :
    StringSpec(
        {
            val chainId = "idos-staging"

            "should get chain info" {
                val secret = getSecrets()
                val signer =
                    createEthSigner(
                        secret.keyPair.privateKey.key
                            .toString(16),
                    )
                val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
                client.chainInfo().chainId shouldBe chainId
            }

            "should get wallets" {
                val secrets = getSecrets()
                val signer =
                    createEthSigner(
                        secrets.keyPair.privateKey.key
                            .toString(16),
                    )
                val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
                val accountId = "0x" + signer.getIdentifier().value
                println(accountId)
                hasProfile(client, accountId).hasProfile shouldBe true
                val wallets = getWallets(client)
                val credentials = getCredentials(client).filter { it.publicNotes.isNotEmpty() }
                val profile = getUser(client)
                val accessGrants = getAccessGrantsOwned(client)

                wallets.asClue { it.first().address.lowercase() shouldBe accountId }

                accessGrants.firstOrNull()?.run {
                    revokeAccessGrant(client, this.id).asClue { it shouldNotBe null }
                }
                val id = credentials.last().id
                val data = getCredentialOwned(client, id).single()
                val enclave =
                    Enclave(
                        userId = profile.id,
                        secrets.password,
                    )
                val raw = enclave.decrypt(Base64String(data.content).toByteArray(), Base64String(data.encryptorPublicKey).toByteArray())
                val decrypt = raw.decodeToString()
                decrypt.asClue { it shouldNotBe null }

                // test new structure
                val result = client.callAction(GetAccessGrantsForCredential, GetAccessGrantsForCredentialInput(id))
                result
                    .onSuccess { println("found grants: $it") }
                    .onFailure { println("failed to get grants for credential: $it") }

//        println(wallets)
//        println(credentials)
//        println(profile)
//        println(accessGrants)
//        println(decrypt)
            }
        },
    )
