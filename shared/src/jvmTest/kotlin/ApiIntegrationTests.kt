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
                val client = IdosClient.create("https://nodes.staging.idos.network", chainId, signer)
                client.users.hasProfile(signer.getIdentifier()) shouldBe true
                val wallets =
                    client.wallets
                        .getAll()
                        .firstOrNull()!!
                val credentials =
                    client.credentials
                        .getAll()
                        .filter { it.publicNotes.isNotEmpty() }
                val profile = client.users.get()
                val accessGrants = client.accessGrants.getOwned()

                wallets.asClue { it.address.lowercase().toHexString() shouldBe signer.getIdentifier() }

                accessGrants.firstOrNull()?.run {
                    client.accessGrants.revoke(this.id).asClue { it shouldNotBe null }
                }
                val id = credentials.last().id

                val data = client.credentials.getOwned(id)!!
                val enclave = Enclave(JvmEncryption(), JvmMetadataStorage())
                enclave.generateKey(profile.id, secrets.password, 1000)
                val content = Base64String(data.content).toByteArray()
                val pubkey = Base64String(data.encryptorPublicKey).toByteArray()
                val raw = enclave.decrypt(content, pubkey)
                val decrypt = String(raw)
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
                val client = IdosClient.create("https://nodes.staging.idos.network", chainId, signer)

                val newKey =
                    MnemonicWords("artwork teach annual inner muffin slim concert diagram width summer soap scrub")
                        .toSeed()
                        .toKey("m/44'/60'/0'/0/47")

//                val signer = JvmEthSigner(newKey.keyPair)
//                val client = IdosClient.create("https://nodes.staging.idos.network", chainId, signer)
                println(
                    newKey.keyPair.publicKey
                        .toAddress()
                        .toString(),
                )
                // a32fcaa49484b85a70227e61c75c2d9c73e2582f
                val msg = "Sign this message to prove you own this wallet"
                val sign = signer.sign(msg.toByteArray())
                println(sign.toHexString())
                println(UuidString.generate())
                val uuid = UuidString("121ff3f0-8466-4ac3-9984-bb8fa221e565")
                val res =
                    client.wallets
                        .add(
                            AddWalletParams(
                                uuid,
                                newKey.keyPair.publicKey
                                    .toAddress()
                                    .toString(),
                                newKey.keyPair.publicKey.toString(),
                                "i am the owner of this key adkajdlakdjaa",
                                sign.toHexString(),
                            ),
                        )

                println(res)

                val wallets = client.wallets.getAll()
                println(wallets)
            }
        },
    )
