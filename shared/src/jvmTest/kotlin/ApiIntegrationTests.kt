import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.idos.enclave.Enclave
import org.idos.kwil.KwilActionClient
import org.idos.kwil.actions.*
import org.idos.kwil.signer.createEthSigner
import org.idos.kwil.utils.base64ToBytes
import org.idos.kwil.utils.bytesToString

class ApiIntegrationTests : StringSpec({
    val chainId = "idos-staging"

    "should get chain info" {
        val secret = getSecrets()
        val signer = createEthSigner(secret.keyPair.privateKey.key.toString(16))
        val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
        client.chainInfo().chainId shouldBe chainId
    }

    "should get wallets" {
        val secrets = getSecrets()
        val signer = createEthSigner(secrets.keyPair.privateKey.key.toString(16))
        val client = KwilActionClient("https://nodes.staging.idos.network", signer, chainId)
        val accountId = "0x" + signer.getIdentifier()
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
        val enclave = Enclave(
            userId = profile.id,
            secrets.password,
        )
        val raw = enclave.decrypt(base64ToBytes(data.content), base64ToBytes(data.encryptorPublicKey))
        val decrypt = bytesToString(raw)
        decrypt.asClue { it shouldNotBe null }

//        println(wallets)
//        println(credentials)
//        println(profile)
//        println(accessGrants)
//        println(decrypt)
    }
})