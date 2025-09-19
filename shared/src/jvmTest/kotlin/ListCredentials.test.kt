package org.idos

import org.idos.enclave.Enclave
import org.idos.kwil.KwilActionClient
import org.idos.kwil.actions.getAccessGrantsOwned
import org.idos.kwil.actions.getAttributes
import org.idos.kwil.actions.getCredentialOwned
import org.idos.kwil.actions.getCredentials
import org.idos.kwil.actions.getUser
import org.idos.kwil.actions.getWallets
import org.idos.kwil.actions.hasProfile
import org.idos.kwil.actions.revokeAccessGrant
import org.idos.kwil.signer.createEthSigner
import org.idos.kwil.utils.base64ToBytes
import org.idos.kwil.utils.bytesToString
import org.idos.kwil.utils.stringToBytes
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.crypto.toHex
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import io.github.cdimascio.dotenv.dotenv

class ListCredentialsTest {
    @Test
    fun testListCredentials() = runTest {
        // Load environment variables from .env file
        val dotenv = dotenv {
            directory = "../"
            ignoreIfMalformed = false
            ignoreIfMissing = false
        }
        
        val words = dotenv["MNEMONIC_WORDS"] ?: throw IllegalStateException(
            "MNEMONIC_WORDS not found in .env file. Please copy .env.example to .env and set your mnemonic phrase."
        )
        val password = dotenv["PASSWORD"] ?: throw IllegalStateException(
            "PASSWORD not found in .env file. Please copy .env.example to .env and set your password."
        )
        
        // Validate mnemonic has proper format (12 or 24 words)
        val wordCount = words.split(" ").size
        require(wordCount == 12 || wordCount == 24) {
            "Invalid mnemonic: expected 12 or 24 words, got $wordCount"
        }
        
        println("Using mnemonic: ${words.take(20)}... ($wordCount words)")
        
        val mnemonic = MnemonicWords(words)
        val seed = mnemonic.toSeed("")

        val key = seed.toKey("m/44'/60'/0'/0/47")
        val ecKeyPair = key.keyPair

        val signer = createEthSigner(ecKeyPair.privateKey.key.toString(16))
        println(signer.accountId())

        println("Init:")
        val client = KwilActionClient("https://nodes.staging.idos.network", signer, "idos-staging")
        val response = client.chainInfo()
        println(response)

        val response3 = client.ping("Test")
        println(response3)

        println()
        println()

        val hasProfile = hasProfile(client, "0xBf847E2565E3767232C18B5723e957053275B28F")
        println("Has profile?")
        println(hasProfile)
        require(hasProfile.hasProfile) { "This wallet has no profile, can't continue!" }

        println()
        println()
        println()

        println("Authorized getWallets for this wallet:")
        val wallets = getWallets(client)
        println(wallets)

        println()
        println()
        println()

        println("Authorized getCredentials for this wallet:")
        val credentials = getCredentials(client)
        println(credentials)

        println()
        val profile = getUser(client)
        println(profile)

        println("Access grants:")
        val accessGrants = getAccessGrantsOwned(client)
        println(accessGrants)

        println("Revoking access grant:")
        val grant = accessGrants.firstOrNull()
        if (grant != null) {
            val rag = revokeAccessGrant(client, grant.id)
            println(rag)
        }

        val enclave =
            Enclave(
                userId = profile.id,
                password,
            )

        val credentialId = credentials.filter { it.originalId == null }.last()
        println(credentialId.publicNotes)

        val data = getCredentialOwned(client, credentialId.id)
        val decrypt = enclave.decrypt(base64ToBytes(data.first().content), base64ToBytes(data.first().encryptorPublicKey))

        println()
        println("Decrytped:")
        println(bytesToString(decrypt))
    }
}
