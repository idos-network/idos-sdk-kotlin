package org.idos

import kotlin.test.Test
import kotlin.test.assertTrue
import io.github.cdimascio.dotenv.dotenv
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.bip32.toKey
import org.idos.kwil.signer.createEthSigner

class EnvironmentTest {
    @Test
    fun testEnvironmentLoading() {
        // Load environment variables from .env file
        val dotenv = dotenv {
            directory = "../"
            ignoreIfMalformed = false
            ignoreIfMissing = false
        }
        
        val words = dotenv["MNEMONIC_WORDS"] ?: throw IllegalStateException(
            "MNEMONIC_WORDS not found in .env file. Please copy .env.example to .env and set your mnemonic phrase."
        )
        
        // Validate mnemonic has proper format (12 or 24 words)
        val wordCount = words.split(" ").size
        require(wordCount == 12 || wordCount == 24) {
            "Invalid mnemonic: expected 12 or 24 words, got $wordCount"
        }
        
        println("✅ Successfully loaded mnemonic with $wordCount words")
        
        // Test that we can create a signer from the mnemonic
        val mnemonic = MnemonicWords(words)
        val seed = mnemonic.toSeed("")
        val key = seed.toKey("m/44'/60'/0'/0/47")
        val ecKeyPair = key.keyPair
        
        val signer = createEthSigner(ecKeyPair.privateKey.key.toString(16))
        val accountId = signer.accountId()
        
        println("✅ Created signer with account: ${accountId.identifier}")
        
        // Validate that we have a proper Ethereum address
        val identifier = accountId.identifier ?: throw IllegalStateException("Account identifier should not be null")
        assertTrue(identifier.length == 40, "Ethereum address should be 40 characters")
        assertTrue(identifier.matches(Regex("[0-9a-fA-F]+")), "Address should be hexadecimal")
        
        println("✅ Environment setup is valid!")
    }
}
