import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.utils.io.core.toByteArray
import org.idos.IdosClient
import org.idos.add
import org.idos.enclave.EnclaveSessionConfig
import org.idos.enclave.ExpirationType
import org.idos.enclave.JvmMetadataStorage
import org.idos.enclave.crypto.JvmEncryption
import org.idos.enclave.local.LocalEnclave
import org.idos.get
import org.idos.getAll
import org.idos.getOwned
import org.idos.hasProfile
import org.idos.kwil.domain.generated.execute.AddWalletParams
import org.idos.kwil.protocol.KwilProtocol
import org.idos.kwil.serialization.toByteArray
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.Uuid
import org.idos.remove
import org.idos.revoke
import org.idos.signer.JvmEthSigner
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.crypto.toAddress
import org.kethereum.extensions.toHexString
import org.kethereum.extensions.toHexStringZeroPadded
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ApiIntegrationTests :
    StringSpec(
        {
            val chainId = "kwil-testnet"
            val url = "https://nodes.playground.idos.network"

            "should get chain info" {
                val secret = getSecrets()
                val signer = JvmEthSigner(secret.keyPair)
                val protocol = KwilProtocol(url, chainId)
                protocol.chainInfo().chainId shouldBe chainId
            }

            "should get wallets" {
                val secrets = getSecrets("m/44'/60'/0'/0/4")
                val signer = JvmEthSigner(secrets.keyPair)
                val client = IdosClient.create(url, chainId, signer)
                client.users.hasProfile(signer.address.hex) shouldBe true
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

                wallets.asClue { it.address.lowercase() shouldBe signer.address.hex }

                accessGrants.firstOrNull()?.run {
                    client.accessGrants.revoke(this.id).asClue { it shouldNotBe null }
                }
                val id = credentials.last().id

                val data = client.credentials.getOwned(id)
                val enclave = LocalEnclave(JvmEncryption(), JvmMetadataStorage())
                val session = EnclaveSessionConfig(ExpirationType.TIMED, 1000)
                enclave.generateKey(profile.id, secrets.password, session)
                val content = Base64String(data.content).toByteArray()
                val pubkey = Base64String(data.encryptorPublicKey).toByteArray()
                val raw = enclave.decrypt(content, pubkey)
                val decrypt = String(raw)
                decrypt.asClue { it shouldNotBe null }
            }

            "should create a new wallet" {
                val secrets = getSecrets()
                val client = IdosClient.create(url, chainId, JvmEthSigner(secrets.keyPair))
                val expectedCount = client.wallets.getAll().size

                val newKey =
                    MnemonicWords("artwork teach annual inner muffin slim concert diagram width summer soap scrub")
                        .toSeed()
                        .toKey("m/44'/60'/0'/0/0")
                val pubkey = newKey.keyPair.publicKey.key.toHexStringZeroPadded(130)
                val address = newKey.keyPair.publicKey
                    .toAddress()
                    .toString()
                val signer = JvmEthSigner(newKey.keyPair)
                val uuid = Uuid.generate()
                val msg = "Sign this message to prove you own this wallet"
                val sign = signer.sign(msg.toByteArray())
                val add =
                    AddWalletParams(
                        uuid,
                        address,
                        pubkey,
                        msg,
                        sign.toHexString(),
                    )


                client.wallets.add(add)
                client.wallets.getAll()
                client.wallets.remove(uuid)

                client.wallets.getAll().size shouldBe expectedCount
            }
        },
    )
