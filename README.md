# idOS SDK Kotlin

Kotlin Multiplatform SDK for idOS - Android, iOS, and JVM support.

## 🚀 Quick Start

### 1. Create a Signer

```kotlin
import org.idos.kwil.security.signer.JvmEthSigner
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed

val mnemonic = MnemonicWords("your twelve or twenty four word mnemonic phrase")
val seed = mnemonic.toSeed("")
val key = seed.toKey("m/44'/60'/0'/0/0")  // Ethereum derivation path
val signer = JvmEthSigner(key.keyPair)
```

### 2. Create Client

```kotlin
import org.idos.IdosClient

val client = IdosClient.create(
    baseUrl = "https://nodes.staging.idos.network",
    chainId = "idos-testnet",
    signer = signer
).getOrThrow()
```

### 3. Use Organized APIs

```kotlin
// User operations
client.users.get()
    .onSuccess { user -> println("User: ${user.id}") }
    .onFailure { error -> println("Error: ${error.message}") }

// Wallet operations
client.wallets.getAll()
    .onSuccess { wallets -> println("Found ${wallets.size} wallets") }

client.wallets.add(AddWalletParams(id, address, publicKey, signature))
    .onSuccess { txHash -> println("Wallet added: $txHash") }

// Credential operations
client.credentials.getOwned()
    .onSuccess { credentials ->
        credentials.forEach { println(it.publicNotes) }
    }

// Access grant operations
client.accessGrants.getOwned()
    .onSuccess { grants -> println("${grants.size} grants") }
```

## 📚 API Reference

### IdosClient Groups

All operations return `Result<T>` for safe error handling.

#### Wallets (`client.wallets`)
- `add(input: AddWalletParams): Result<HexString>` - Add a new wallet
- `getAll(): Result<List<GetWalletsResponse>>` - Get all wallets
- `remove(id: UuidString): Result<HexString>` - Remove a wallet

#### Credentials (`client.credentials`)
- `add(input: AddCredentialParams): Result<HexString>` - Add a credential
- `getAll(): Result<List<GetCredentialsResponse>>` - Get all owned credentials
- `getOwned(id: UuidString): Result<GetCredentialOwnedResponse>` - Get specific owned credential
- `getShared(id: UuidString): Result<List<GetCredentialSharedResponse>>` - Get shared credentials
- `edit(input: EditCredentialParams): Result<HexString>` - Edit a credential
- `remove(id: UuidString): Result<HexString>` - Remove a credential
- `share(input: ShareCredentialParams): Result<HexString>` - Share a credential

#### Access Grants (`client.accessGrants`)
- `create(input: CreateAccessGrantParams): Result<HexString>` - Create an access grant
- `getOwned(): Result<List<GetAccessGrantsOwnedResponse>>` - Get owned grants
- `getGranted(userId, page, size): Result<List<GetAccessGrantsGrantedResponse>>` - Get granted grants
- `getForCredential(credentialId): Result<List<GetAccessGrantsForCredentialResponse>>` - Get grants for credential
- `revoke(id: UuidString): Result<HexString>` - Revoke an access grant

#### Users (`client.users`)
- `get(): Result<GetUserResponse>` - Get current user profile
- `hasProfile(address: HexString): Result<Boolean>` - Check if address has profile

#### Attributes (`client.attributes`)
- `add(input: AddAttributeParams): Result<HexString>` - Add an attribute
- `getAll(): Result<List<GetAttributesResponse>>` - Get all attributes
- `edit(input: EditAttributeParams): Result<HexString>` - Edit an attribute
- `remove(id: UuidString): Result<HexString>` - Remove an attribute
- `share(input: ShareAttributeParams): Result<HexString>` - Share an attribute

## 🔐 Enclave (Encryption)

The SDK provides stateful encryption management with reactive UI updates via `EnclaveOrchestrator`:

```kotlin
import org.idos.enclave.*

// Create enclave components
val encryption = JvmEncryption()  // or AndroidEncryption(context), IosEncryption()
val storage = JvmMetadataStorage()  // or AndroidMetadataStorage(context), IosMetadataStorage()
val enclave = Enclave(encryption, storage)
val orchestrator = EnclaveOrchestrator(enclave)

// Observe state for UI updates
orchestrator.state.collect { state ->
    when (state) {
        is EnclaveFlow.RequiresKey -> showPasswordPrompt()
        is EnclaveFlow.Available -> enableEncryptedFeatures()
        is EnclaveFlow.WrongPasswordSuspected ->
            showError("Decryption failed ${state.attemptCount} times")
        is EnclaveFlow.Error -> showError(state.message)
        // ... handle other states
    }
}

// Generate key when user provides password
orchestrator.generateKey(
    userId = userProfile.id,
    password = userPassword,
    expirationMillis = 3600000  // 1 hour
)

// Decrypt credential data
val credential = client.credentials.getOwned(credentialId).getOrThrow()
orchestrator.decrypt(
    message = credential.content.toByteArray(),
    senderPublicKey = credential.encryptorPublicKey.toByteArray()
).onSuccess { decryptedData ->
    println(String(decryptedData))
}.onFailure { error ->
    when (error) {
        is EnclaveError.NoKey -> println("Generate key first")
        is EnclaveError.KeyExpired -> println("Key expired, regenerate")
        is EnclaveError.DecryptionFailed ->
            println("Decryption failed: ${error.reason}")
        else -> println("Error: ${error.message}")
    }
}
```

### Enclave Error Types

```kotlin
sealed class EnclaveError : Exception {
    class NoKey : EnclaveError()
    class KeyExpired : EnclaveError()
    data class DecryptionFailed(reason: DecryptFailure) : EnclaveError()
    data class EncryptionFailed(details: String) : EnclaveError()
    data class InvalidPublicKey(details: String) : EnclaveError()
    data class KeyGenerationFailed(details: String) : EnclaveError()
    data class StorageFailed(details: String) : EnclaveError()
}

sealed class DecryptFailure {
    data object WrongPassword : DecryptFailure()
    data object CorruptedData : DecryptFailure()
    data object InvalidCiphertext : DecryptFailure()
}
```

## 🧪 Testing

### Setup Environment

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` with your test credentials:
   ```
   MNEMONIC_WORDS=your twelve or twenty four word mnemonic phrase
   PASSWORD=your-password
   ```

3. Run tests:
   ```bash
   ./gradlew jvmTest
   ```

**Note**: Your mnemonic should be for a wallet that has an idOS profile for integration tests to work.

## 🏗️ Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed documentation on:
- 4-layer architecture (Transport → Protocol → Domain → Public API)
- Package structure and responsibilities
- KWIL protocol implementation details
- Transaction signing scheme
- Error handling strategy
- Platform-specific implementations

## 📦 Type Safety

The SDK uses type-safe wrappers for common values:

```kotlin
import org.idos.kwil.types.HexString
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.UuidString

val address = HexString("0x1234...")  // Ethereum address
val txHash = HexString("0xabcd...")   // Transaction hash
val credentialId = UuidString("550e8400-e29b-41d4-a716-446655440000")
```

## ⚠️ Error Handling

All public APIs return `Result<T>` for safe, explicit error handling:

```kotlin
// Handle success and failure
client.wallets.add(walletParams)
    .onSuccess { txHash ->
        println("Success: $txHash")
    }
    .onFailure { error ->
        when (error) {
            is DomainError.ValidationError -> println("Invalid input: ${error.message}")
            is DomainError.AuthenticationRequired -> println("Auth failed: ${error.message}")
            is DomainError.NotFound -> println("Not found: ${error.message}")
            else -> println("Error: ${error.message}")
        }
    }

// Or use getOrNull() / getOrThrow()
val user = client.users.get().getOrNull()
if (user != null) {
    println("User ID: ${user.id}")
}
```

## 🔧 Platform Support

- **JVM**: Full support with KEthereum for Ethereum signing
- **Android**: Same as JVM, plus libsodium via Lazysodium for encryption
- **iOS**: Darwin HTTP engine, libsodium XCFramework for encryption

## 📖 Further Reading

- [ARCHITECTURE.md](ARCHITECTURE.md) - Detailed architecture documentation
- [KWIL Protocol](https://github.com/kwilteam/kwil-db) - KWIL database documentation
- [idOS Schema](https://github.com/idos-networks/idos-schema) - Schema definitions

## 📄 License

See LICENSE file for details.
