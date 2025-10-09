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

try {
    val client = IdosClient.create(
        baseUrl = "https://nodes.staging.idos.network",
        chainId = "idos-testnet",
        signer = signer
    )
} catch (e: DomainError) {
    println("Failed to create client: ${e.message}")
}
```

### 3. Use Organized APIs

```kotlin
try {
    // User operations
    val user = client.users.get()
    println("User: ${user.id}")

    // Wallet operations
    val wallets = client.wallets.getAll()
    println("Found ${wallets.size} wallets")

    val txHash = client.wallets.add(AddWalletParams(id, address, publicKey, signature))
    println("Wallet added: $txHash")

    // Credential operations
    val credentials = client.credentials.getAll()
    credentials.forEach { println(it.publicNotes) }

    // Access grant operations
    val grants = client.accessGrants.getOwned()
    println("${grants.size} grants")
} catch (e: DomainError) {
    when (e) {
        is DomainError.ValidationError -> println("Invalid input: ${e.message}")
        is DomainError.AuthenticationRequired -> println("Auth required: ${e.message}")
        is DomainError.NotFound -> println("Not found: ${e.message}")
        else -> println("Error: ${e.message}")
    }
}
```

## 📚 API Reference

### IdosClient Groups

All operations are suspend functions that throw `DomainError` on failure.

#### Wallets (`client.wallets`)
- `suspend fun add(input: AddWalletParams): HexString` - Add a new wallet
- `suspend fun getAll(): List<GetWalletsResponse>` - Get all wallets
- `suspend fun remove(id: UuidString): HexString` - Remove a wallet

#### Credentials (`client.credentials`)
- `suspend fun add(input: AddCredentialParams): HexString` - Add a credential
- `suspend fun getAll(): List<GetCredentialsResponse>` - Get all owned credentials
- `suspend fun getOwned(id: UuidString): GetCredentialOwnedResponse` - Get specific owned credential
- `suspend fun getShared(id: UuidString): List<GetCredentialSharedResponse>` - Get shared credentials
- `suspend fun edit(input: EditCredentialParams): HexString` - Edit a credential
- `suspend fun remove(id: UuidString): HexString` - Remove a credential
- `suspend fun share(input: ShareCredentialParams): HexString` - Share a credential

#### Access Grants (`client.accessGrants`)
- `suspend fun create(input: CreateAccessGrantParams): HexString` - Create an access grant
- `suspend fun getOwned(): List<GetAccessGrantsOwnedResponse>` - Get owned grants
- `suspend fun getGranted(userId, page, size): List<GetAccessGrantsGrantedResponse>` - Get granted grants
- `suspend fun getForCredential(credentialId): List<GetAccessGrantsForCredentialResponse>` - Get grants for credential
- `suspend fun revoke(id: UuidString): HexString` - Revoke an access grant

#### Users (`client.users`)
- `suspend fun get(): GetUserResponse` - Get current user profile
- `suspend fun hasProfile(address: HexString): Boolean` - Check if address has profile

#### Attributes (`client.attributes`)
- `suspend fun add(input: AddAttributeParams): HexString` - Add an attribute
- `suspend fun getAll(): List<GetAttributesResponse>` - Get all attributes
- `suspend fun edit(input: EditAttributeParams): HexString` - Edit an attribute
- `suspend fun remove(id: UuidString): HexString` - Remove an attribute
- `suspend fun share(input: ShareAttributeParams): HexString` - Share an attribute

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
try {
    val credential = client.credentials.getOwned(credentialId)
    val decryptedData = orchestrator.decrypt(
        message = credential.content.toByteArray(),
        senderPublicKey = credential.encryptorPublicKey.toByteArray()
    )
    println(String(decryptedData))
} catch (e: EnclaveError) {
    when (e) {
        is EnclaveError.NoKey -> println("Generate key first")
        is EnclaveError.KeyExpired -> println("Key expired, regenerate")
        is EnclaveError.DecryptionFailed ->
            println("Decryption failed: ${e.reason}")
        else -> println("Error: ${e.message}")
    }
} catch (e: DomainError) {
    println("Failed to get credential: ${e.message}")
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

All public APIs use suspend functions that throw `DomainError` on failure. Wrap calls in try-catch for error handling:

```kotlin
try {
    val txHash = client.wallets.add(walletParams)
    println("Success: $txHash")
} catch (e: DomainError) {
    when (e) {
        is DomainError.ValidationError -> println("Invalid input: ${e.message}")
        is DomainError.AuthenticationRequired -> println("Auth failed: ${e.message}")
        is DomainError.NotFound -> println("Not found: ${e.message}")
        is DomainError.ActionFailed -> println("Action failed: ${e.message}")
        else -> println("Error: ${e.message}")
    }
}

// Clean, direct API - no Result wrapping needed
try {
    val user = client.users.get()
    println("User ID: ${user.id}")
} catch (e: DomainError) {
    println("Failed to get user: ${e.message}")
}
```

## 🔧 Platform Support

- **JVM**: Full support with KEthereum for Ethereum signing
- **Android**: Same as JVM, plus libsodium via Lazysodium for encryption, EncryptedFile with StrongBox for secure storage
- **iOS**: Darwin HTTP engine, libsodium XCFramework for encryption, iOS Keychain for secure storage
  - SKIE library auto-converts Kotlin suspend/throws to Swift async/throws
  - Seamless Swift interop with native error handling

## 📖 Further Reading

- [ARCHITECTURE.md](ARCHITECTURE.md) - Detailed architecture documentation
- [KWIL Protocol](https://github.com/kwilteam/kwil-db) - KWIL database documentation
- [idOS Schema](https://github.com/idos-networks/idos-schema) - Schema definitions

## 📄 License

See LICENSE file for details.
