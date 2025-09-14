# idOS SDK kotlin

## Signer

You can use KEthereum like this:

```kotlin
import org.idos.kwil.signer.EthSigner
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed

val words = "my secret phrase" // Replace by your secret phrase from metamask
val mnemonic = MnemonicWords(words)
val seed = mnemonic.toSeed("")

val key = seed.toKey("m/44'/60'/0'/0/REPLACE_WITH_WALLET_NO")
val ecKeyPair = key.keyPair

val signer = EthSigner(ecKeyPair)
```

## Kwil client

```kotlin
// Signer is required
val client = KwilActionClient("https://nodes.staging.idos.network", signer, "idos-staging")
val chainInfo = client.chainInfo()
val userProfile = getUser(client)
var accessGrants = getAccessGrantsOwned(client)

var grant = accessGrants.first()
if (grant != null) {
  revokeAccessGrant(client, grant.id)
}
```

## Enclave

Just key-derivation passwords works now:

```kotlin
val enclave = Enclave(
  userId = userProfile.id,
  password = "password",
)

val credentialId = credentials.filter { it.originalId === null }.last()
println(credentialId.publicNotes)

val data = getCredentialOwned(client, credentialId.id)
val decrypt = enclave.decrypt(base64ToBytes(data.first().content), base64ToBytes(data.first().encryptorPublicKey))
println(bytesToString(decrypt))
```
