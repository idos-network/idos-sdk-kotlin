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
                val accountId = signer.getIdentifier().prefixedValue
                println(accountId)
                client.hasUserProfile(accountId).hasProfile shouldBe true
                val wallets = client.getWallets()
                val credentials = client.getCredentials().filter { it.publicNotes.isNotEmpty() }
                val profile = client.getUser()
                val accessGrants = client.getAccessGrantsOwned()

                wallets.asClue { it.first().address.lowercase() shouldBe accountId }

                accessGrants.firstOrNull()?.run {
                    client.revokeAccessGrant(this.id).asClue { it shouldNotBe null }
                }
                val id = credentials.last().id
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
