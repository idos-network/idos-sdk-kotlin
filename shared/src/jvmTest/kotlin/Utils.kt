import io.github.cdimascio.dotenv.dotenv
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldNotBe
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.model.ECKeyPair

data class Secrets(
    val keyPair: ECKeyPair,
    val password: String,
)

val getSecrets = {
    val dotenv =
        dotenv {
            directory = "../"
            ignoreIfMalformed = false
            ignoreIfMissing = false
        }

    val words = dotenv["MNEMONIC_WORDS"]
    { "MNEMONIC_WORDS not found in .env file. Please copy .env.example to .env and set your mnemonic phrase." }.asClue {
        words shouldNotBe null
        words.split(" ").size shouldBeIn listOf(12, 24)
    }

    val password = dotenv["PASSWORD"]
    { "PASSWORD not found in .env file. Please copy .env.example to .env and set your password." }.asClue {
        password shouldNotBe null
    }

    val mnemonic = MnemonicWords(words)
    val seed = mnemonic.toSeed("")

    val key = seed.toKey("m/44'/60'/0'/0/47")
    Secrets(key.keyPair, password)
}
