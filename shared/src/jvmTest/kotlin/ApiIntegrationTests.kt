import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.utils.io.core.toByteArray
import org.idos.IdosClient
import org.idos.add
import org.idos.enclave.Enclave
import org.idos.enclave.JvmEncryption
import org.idos.enclave.JvmMetadataStorage
import org.idos.get
import org.idos.getAll
import org.idos.getOwned
import org.idos.hasProfile
import org.idos.kwil.domain.generated.execute.AddWalletParams
import org.idos.kwil.protocol.KwilProtocol
import org.idos.kwil.security.signer.JvmEthSigner
import org.idos.kwil.serialization.toByteArray
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString.Companion.toHexString
import org.idos.kwil.types.UuidString
import org.idos.revoke
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.crypto.toAddress

class ApiIntegrationTests :
    StringSpec(
        {
            val chainId = "idos-staging"

            "should get chain info" {
                val secret = getSecrets()
                val signer = JvmEthSigner(secret.keyPair)
                val protocol = KwilProtocol("https://nodes.staging.idos.network", chainId)
                protocol.chainInfo().chainId shouldBe chainId
            }

            "should get wallets" {
                val secrets = getSecrets()
                val signer = JvmEthSigner(secrets.keyPair)
                val client = IdosClient.create("https://nodes.staging.idos.network", chainId, signer).getOrThrow()
                client.users.hasProfile(signer.getIdentifier()).getOrThrow() shouldBe true
                val wallets =
                    client.wallets
                        .getAll()
                        .getOrThrow()
                        .firstOrNull()!!
                val credentials =
                    client.credentials
                        .getAll()
                        .getOrThrow()
                        .filter { it.publicNotes.isNotEmpty() }
                val profile = client.users.get().getOrThrow()!!
                val accessGrants = client.accessGrants.getOwned().getOrThrow()

                wallets.asClue { it.address.lowercase().toHexString() shouldBe signer.getIdentifier() }

                accessGrants.firstOrNull()?.run {
                    client.accessGrants.revoke(this.id).asClue { it shouldNotBe null }
                }
                val id = credentials.last().id

                val data = client.credentials.getOwned(id).getOrThrow()!!
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

            "should create a new wallet" {
                val secrets = getSecrets()
                val signer = JvmEthSigner(secrets.keyPair)
                val client = IdosClient.create("https://nodes.staging.idos.network", chainId, signer).getOrThrow()

                val newKey =
                    MnemonicWords("artwork teach annual inner muffin slim concert diagram width summer soap scrub")
                        .toSeed()
                        .toKey("m/44'/60'/0'/0/47")

//                val signer = JvmEthSigner(newKey.keyPair)
//                val client = IdosClient.create("https://nodes.staging.idos.network", chainId, signer).getOrThrow()
                val msg = "i am the owner if this key"
                val sign = signer.sign(msg.toByteArray())
                val res =
                    client.wallets
                        .add(
                            AddWalletParams(
                                UuidString.generate(),
                                newKey.keyPair.publicKey
                                    .toAddress()
                                    .toString(),
                                newKey.keyPair.publicKey.toString(),
                                "i am the owner of this key",
                                sign.toHexString(),
                            ),
                        )

                println(res)

                val wallets = client.wallets.getAll().getOrThrow()
                println(wallets)
            }
        },
    )
