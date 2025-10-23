# idOS SDK Architecture

Kotlin Multiplatform SDK for idOS - Android, iOS, and JVM support with KWIL database backend.

## 🏗️ Layer Architecture

```
┌─────────────────────────────────────────────┐
│  Public API (IdosClient + Extensions)       │  suspend + @Throws(DomainError)
├─────────────────────────────────────────────┤
│  Domain (ActionExecutor, Generated Actions) │  suspend + throws DomainError
├─────────────────────────────────────────────┤
│  Protocol (KWIL RPC, Transaction Signing)   │  throws ProtocolError
├─────────────────────────────────────────────┤
│  Transport (JSON-RPC over HTTP)             │  throws TransportError
└─────────────────────────────────────────────┘
```

---

## 📦 Package Structure

```
org.idos/
├── IdosClient.kt                    # Main SDK entry point
├── IdosClientExtensions.kt          # All public operations
│
├── enclave/                         # Encryption & key derivation
│   ├── Enclave.kt
│   ├── Encryption.kt
│   ├── KeyDerivation.kt
│   ├── SecureStorage.kt
│   └── MetadataStorage.kt
│
└── kwil/                            # KWIL protocol implementation
    ├── domain/                      # Domain layer (public API boundary)
    │   ├── ActionExecutor.kt        # Result-based executor
    │   ├── DomainError.kt           # Public error types
    │   ├── Schema.kt                # Type aliases (PositionalParams, etc.)
    │   ├── NoParamsAction.kt        # Helper for parameterless actions
    │   └── generated/               # ⚠️ Generated from schema - DO NOT EDIT
    │       ├── ViewAction.kt        # Interface for read-only queries
    │       ├── ExecuteAction.kt     # Interface for transactions
    │       ├── view/                # Query actions (GetUser, GetWallets, etc.)
    │       └── execute/             # Transaction actions (AddWallet, CreateAccessGrant, etc.)
    │
    ├── protocol/                    # Protocol layer (internal)
    │   ├── KwilProtocol.kt          # Main protocol client
    │   ├── CallActionExtensions.kt  # View action execution
    │   ├── ExecuteActionExtensions.kt # Transaction building & signing
    │   ├── Models.kt                # Protocol models (enums, requests, responses)
    │   ├── Transaction.kt           # Transaction types
    │   ├── Message.kt               # RPC message wrapper
    │   ├── MessageBuilder.kt        # Message creation utilities
    │   ├── ProtocolError.kt         # Internal protocol errors
    │   └── Constants.kt             # Protocol constants
    │
    ├── serialization/               # KWIL encoding/decoding
    │   ├── KwilType.kt              # Type system (Text, Int, Uuid, etc.)
    │   ├── KwilValue.kt             # Value encoding
    │   ├── ActionEncoding.kt        # Action payload encoding
    │   ├── ActionSerialization.kt   # Action → Message conversion
    │   ├── KwilEncoding.kt          # Low-level encoding utilities
    │   └── Bytes.kt                 # Byte manipulation helpers
    │
    ├── security/                    # Cryptographic signing & auth
    │   ├── signer/
    │   │   ├── Signer.kt            # Common signer interface
    │   │   ├── EthSigner.kt         # Ethereum signer (common)
    │   │   ├── Types.kt             # Signature types
    │   │   └── EthSigner.jvm.kt     # JVM-specific impl (KEthereum)
    │   └── auth/
    │       ├── Auth.kt              # KGW authentication
    │       └── AuthMessage.kt       # Auth message formatting
    │
    ├── transport/                   # HTTP/JSON-RPC layer
    │   ├── JsonRpcClient.kt         # JSON-RPC 2.0 client
    │   ├── Models.kt                # JSON-RPC models
    │   └── TransportError.kt        # Transport errors
    │
    ├── types/                       # Type-safe wrappers
    │   └── ValueTypes.kt            # HexString, Base64String, UuidString
    │
    └── utils/                       # Utilities
        └── Crypto.kt                # SHA-256 hashing
```

---

## 🔄 Data Flow

### Read Operation (View Action)
```
User Code
  ↓ client.users.get()
IdosClientExtensions
  ↓ executor.callSingle(GetUser)
ActionExecutor (error wrapper)
  ↓ runCatchingDomainError { client.callAction(GetUser, signer) }
KwilProtocol.callAction()
  ↓ action.toMessage(input, signer)
ActionSerialization
  ↓ encodeViewAction() → Message
JsonRpcClient
  ↓ POST {"method": "user.call", "params": {...}}
KWIL Network
  ← QueryResponse
JsonRpcClient (deserialize)
  ← parseQueryResponse<GetUserResponse>()
ActionExecutor
  ← return GetUserResponse (or throw DomainError)
User Code
  ← GetUserResponse (wrap in try-catch)
```

### Write Operation (Execute Action)
```
User Code
  ↓ client.wallets.add(params)
IdosClientExtensions
  ↓ executor.execute(AddWallet, params)
ActionExecutor
  ↓ runCatchingDomainError { client.executeAction(AddWallet, params, signer) }
KwilProtocol.executeAction()
  ↓ 1. getAccount() → fetch nonce
  ↓ 2. encodeExecuteAction() → Base64 payload
  ↓ 3. buildTransaction() → unsigned tx
  ↓ 4. signTransaction() → KWIL signature scheme
  ↓ 5. broadcast() → submit to network
JsonRpcClient
  ↓ POST {"method": "user.broadcast", "params": {"tx": {...}}}
KWIL Network
  ← BroadcastResponse { txHash, result }
ActionExecutor
  ← return HexString(txHash) (or throw DomainError)
User Code
  ← HexString (wrap in try-catch)
```

---

## 🎯 Layer Details

### 🌐 Transport Layer (`kwil/transport/`)

**Purpose**: JSON-RPC 2.0 over HTTP communication

**Key Files**:
- `JsonRpcClient.kt` - Ktor-based RPC client with cookie auth

**Engineering Notes**:
- Uses Ktor with platform-specific engines (OkHttp for Android/JVM, Darwin for iOS)
- Automatic JSON serialization via `kotlinx.serialization`
- Cookie-based session management for KGW authentication
- Throws `TransportError` on network/serialization failures

**Error Handling**: Throws exceptions (converted to DomainError at domain boundary)

---

### 🔧 Protocol Layer (`kwil/protocol/`)

**Purpose**: KWIL-specific RPC methods and transaction handling

**Key Components**:

1. **KwilProtocol.kt** - Main protocol client
   - JSON-RPC method wrappers (ping, health, getAccount, broadcast, etc.)
   - Automatic authentication retry via interceptor
   - Challenge-response KGW authentication

2. **CallActionExtensions.kt** - View action execution
   ```kotlin
   suspend fun <I, O> KwilProtocol.callAction(
       action: ViewAction<I, O>,
       input: I,
       signer: Signer? = null
   ): List<O>
   ```

3. **ExecuteActionExtensions.kt** - Transaction building
   - Fetches nonce from account
   - Encodes action payload
   - Signs with KWIL signature scheme (payload digest + metadata)
   - Broadcasts and waits for confirmation

4. **Transaction Signing** - KWIL protocol specification:
   ```
   Message format:
   {description}

   PayloadType: {type}
   PayloadDigest: {first 20 bytes of SHA-256(payload)}
   Fee: {fee}
   Nonce: {nonce}

   Kwil Chain ID: {chainId}
   ```

**Engineering Notes**:
- All protocol methods throw `ProtocolError` or `MissingAuthenticationException`
- Authentication is automatic: catches `MissingAuthenticationException` and retries once
- Transaction signing follows KWIL's `SIGNED_MSG_CONCAT` scheme

---

### 🗄️ Serialization Layer (`kwil/serialization/`)

**Purpose**: KWIL-specific type encoding and action payload serialization

**Key Files**:

1. **KwilType.kt** - Type system
   ```kotlin
   sealed class KwilType {
       data class Text(val maxLength: Int? = null) : KwilType()
       data class Int(val minValue: Long? = null, val maxValue: Long? = null) : KwilType()
       data class Uuid(val isArray: Boolean = false) : KwilType()
       data class Numeric(val precision: Int, val scale: Int) : KwilType()
       // ... etc
   }
   ```

2. **ActionEncoding.kt** - Binary payload encoding
   - `encodeActionCall()` - View action payload (Base64)
   - `encodeActionExecution()` - Execute action payload (Base64)
   - Format: `version(2) + namespace_len(4) + namespace + action_len(4) + action + args`

3. **ActionSerialization.kt** - Action → Message conversion
   - `ViewAction.toMessage()` - Converts view action to RPC message
   - `ExecuteAction.toMessage()` - Converts execute action to RPC message

**Engineering Notes**:
- All encoding follows KWIL wire format specification
- Little-endian byte ordering for integers
- UTF-8 encoding for strings
- UUID as 16-byte binary (not string representation)

---

### 🏛️ Domain Layer (`kwil/domain/`)

**Purpose**: Type-safe schema actions with suspend/throws error handling

**Key Components**:

1. **ActionExecutor.kt** - Public API executor
   ```kotlin
   class ActionExecutor {
       @Throws(DomainError::class)
       suspend fun <I, O> call(action: ViewAction<I, O>, input: I): List<O>

       @Throws(DomainError::class)
       suspend fun <O> callSingle(action: NoParamsAction<O>): O

       @Throws(DomainError::class)
       suspend fun <I> execute(action: ExecuteAction<I>, input: I): HexString
   }
   ```
   - Wraps all protocol calls in `runCatchingDomainError { ... }`
   - Converts exceptions to `DomainError` and re-throws
   - Auto-retries authentication once on `MissingAuthenticationException`

2. **Generated Actions** (`domain/generated/`)
   - `ViewAction<I, O>` - Interface for queries
   - `ExecuteAction<I>` - Interface for transactions
   - Example:
     ```kotlin
     object GetUser : NoParamsAction<GetUserResponse> {
         override val namespace = "idos"
         override val name = "get_user"
     }

     object AddWallet : ExecuteAction<AddWalletParams> {
         override val namespace = "idos"
         override val name = "add_wallet"
         override val positionalTypes = listOf(KwilType.Uuid(), KwilType.Text(), ...)
         override fun toPositionalParams(input: AddWalletParams) =
             listOf(input.id, input.address, ...)
     }
     ```

3. **DomainError.kt** - Public error hierarchy
   ```kotlin
   sealed class DomainError : Exception() {
       class ActionFailed(message: String, val txHash: String?) : DomainError()
       class ValidationError(message: String) : DomainError()
       class NotFound(message: String) : DomainError()
       class AuthenticationRequired(message: String) : DomainError()
       class Unknown(message: String, cause: Throwable? = null) : DomainError()
   }
   ```

**Engineering Notes**:
- Domain is the boundary where exceptions are converted to `DomainError`
- All lower-layer exceptions converted to `DomainError` and re-thrown
- `callSingle()` throws `DomainError.NotFound` if result is empty
- Generated actions are the single source of truth for schema

---

### 🎨 Public API Layer (`IdosClient` + Extensions)

**Purpose**: Organized, discoverable public API

**Pattern**:
```kotlin
// IdosClient.kt - Structure only
class IdosClient {
    val wallets: Wallets
    val credentials: Credentials
    val accessGrants: AccessGrants
    val users: Users
    val attributes: Attributes
}

// IdosClientExtensions.kt - All operations
@Throws(DomainError::class)
suspend fun IdosClient.Wallets.add(input: AddWalletParams): HexString =
    executor.execute(AddWallet, input)

@Throws(DomainError::class)
suspend fun IdosClient.Wallets.getAll(): List<GetWalletsResponse> =
    executor.call(GetWallets)
```

**Benefits**:
- ✅ IDE autocomplete groups operations by domain
- ✅ Easy to add/remove operations (copy-paste pattern)
- ✅ Clean separation: structure vs. behavior
- ✅ Simple to generate from schema

**Usage**:
```kotlin
try {
    val client = IdosClient.create(baseUrl, chainId, signer)

    // IDE shows: wallets, credentials, accessGrants, users, attributes
    val txHash = client.wallets.add(walletParams)
    println("Added: $txHash")
} catch (e: DomainError) {
    println("Error: ${e.message}")
}
```

---

## 🔐 Enclave Layer (`enclave/`)

**Purpose**: Dual-mode encryption with reactive state management for UI integration

The Enclave layer supports two distinct encryption modes:
- **LOCAL Mode**: Password-based encryption with local key derivation (Scrypt KDF)
- **MPC Mode**: Distributed encryption using Shamir's Secret Sharing across MPC nodes

### Architecture Pattern

```
┌────────────────────────────────────────────────────────────┐
│  EnclaveOrchestrator (StateFlow)                           │  Public API
│  - Factory methods: create(), createLocal(), createMpc()   │
│  - 4-state model: Locked, Unlocking, Unlocked, NotAvail   │
│  - Mode detection from metadata or explicit selection      │
├────────────────────────────────────────────────────────────┤
│  LocalEnclave              │  MpcEnclave                    │
│  - Password → Scrypt KDF   │  - Shamir's Secret Sharing    │
│  - Local key storage       │  - Distributed across nodes   │
│  - Session expiration      │  - Wallet-authenticated       │
├────────────────────────────┼────────────────────────────────┤
│  Enclave (interface)                                        │  iOS-compatible
│  - encrypt() / decrypt()                                    │  via SKIE
│  - Key expiration checks                                    │
│  - Throws EnclaveError                                      │
├────────────────────────────────────────────────────────────┤
│  Encryption (Platform-specific, throws)                     │  Internal
│  - JVM: Lazysodium (libsodium)                             │
│  - Android: Lazysodium (libsodium JNI)                     │
│  - iOS: libsodium XCFramework (C interop)                  │
│  - NaCl Box: Curve25519 + XSalsa20 + Poly1305             │
├────────────────────────────────────────────────────────────┤
│  SecureStorage + MetadataStorage                            │  Platform
│  - Android: EncryptedFile (StrongBox)                      │
│  - iOS: Keychain                                            │
│  - Metadata: SharedPreferences/UserDefaults                │
│  - JVM: In-memory (testing)                                │
└────────────────────────────────────────────────────────────┘
```

### Key Components

#### 1. EnclaveOrchestrator (`EnclaveOrchestrator.kt`)
**Purpose**: Unified orchestrator for LOCAL and MPC encryption modes with reactive UI updates

```kotlin
class EnclaveOrchestrator internal constructor(
    private val localEnclave: LocalEnclave?,
    private val mpcEnclave: MpcEnclave?,
    private val enclaveType: EnclaveKeyType?
) {
    companion object {
        // Create for both modes (type determined from metadata)
        fun create(encryption, storage, mpcConfig, signer, hasher): EnclaveOrchestrator

        // Create LOCAL-only mode
        fun createLocal(encryption, storage): EnclaveOrchestrator

        // Create MPC-only mode
        fun createMpc(encryption, storage, mpcConfig, signer, hasher): EnclaveOrchestrator
    }

    val state: StateFlow<EnclaveState>  // Reactive state for UI

    suspend fun enroll(userId, type)     // Enroll user in chosen mode
    suspend fun checkStatus()            // Check if key exists and is valid
    suspend fun unlock(userId, sessionConfig, password?)  // Unlock (password for LOCAL)
    suspend fun lock()                   // Delete key and lock
    suspend fun <T> withEnclave(action: suspend (Enclave) -> T): T  // Execute with enclave
}
```

**State Model** (`EnclaveState.kt`):
```kotlin
sealed class EnclaveState {
    data object Locked : EnclaveState()           // No valid encryption key
    data object Unlocking : EnclaveState()        // Key generation in progress
    data class Unlocked(val enclave: Enclave) : EnclaveState()  // Ready for operations
    data object NotAvailable : EnclaveState()     // Not properly initialized
}
```

**Factory Methods**:
- `create()` - Both LOCAL and MPC support, mode detected from metadata
- `createLocal()` - LOCAL mode only (password-based)
- `createMpc()` - MPC mode only (distributed)

**Features**:
- ✅ Unified API for both LOCAL and MPC modes
- ✅ Lazy mode detection from stored metadata
- ✅ StateFlow for reactive UI updates (Android Compose, iOS SwiftUI)
- ✅ Automatic key expiration checks
- ✅ Session-based key management

#### 2. LocalEnclave (`LocalEnclave.kt`)
**Purpose**: Password-based encryption with Scrypt KDF

```kotlin
class LocalEnclave(
    private val encryption: Encryption,
    private val storage: MetadataStorage
) : Enclave {
    suspend fun generateKey(userId, password, sessionConfig): ByteArray
    suspend fun hasValidKey()
    suspend fun deleteKey()

    // From Enclave interface
    override suspend fun encrypt(message, receiverPublicKey): Pair<ByteArray, ByteArray>
    override suspend fun decrypt(message, senderPublicKey): ByteArray
}
```

**Key Derivation**:
- **Algorithm**: Scrypt (memory-hard KDF, OWASP recommended)
- **Parameters**: n=16384, r=8, p=1, dkLen=32
- **Salt**: userId (unique per user)
- **Output**: 32-byte encryption key

**Features**:
- Password normalization (Unicode NFC)
- Automatic key expiration checks
- Flexible expiration modes (TIMED, SESSION, ONE_SHOT)
- Metadata tracking (createdAt, lastUsedAt, expiresAt)

#### 3. MpcEnclave (`MpcEnclave.kt`)
**Purpose**: Distributed encryption using Shamir's Secret Sharing

```kotlin
class MpcEnclave(
    private val encryption: Encryption,
    private val storage: MetadataStorage,
    private val mpcConfig: MpcConfig,
    private val signer: Signer,
    private val hasher: Keccak256Hasher
) : Enclave {
    suspend fun enroll(userId)  // Generate and upload secret
    suspend fun uploadSecret(userId, secret)
    suspend fun downloadSecret(userId): ByteArray
    suspend fun unlock(userId, sessionConfig)
    suspend fun addAddress(userId, addressToAdd)
    suspend fun removeAddress(userId, addressToRemove)
    suspend fun updateWallets(userId, addresses)

    // From Enclave interface
    override suspend fun encrypt(message, receiverPublicKey): Pair<ByteArray, ByteArray>
    override suspend fun decrypt(message, senderPublicKey): ByteArray
}
```

**MPC Architecture**:
```
┌─────────────────────────────────────────────┐
│  MpcEnclave                                 │
│  - enroll(), uploadSecret(), downloadSecret │
│  - Wallet-authenticated operations          │
├─────────────────────────────────────────────┤
│  MpcClient                                  │
│  - Parallel node communication              │
│  - Threshold-based success (k of n)         │
│  - Structured error aggregation             │
├─────────────────────────────────────────────┤
│  NodeClient (per node)                      │
│  - HTTP/JSON-RPC to individual MPC node     │
│  - EIP-712 typed data signing               │
├─────────────────────────────────────────────┤
│  PartisiaRpcClient                          │
│  - Blockchain RPC for node discovery        │
│  - Contract state queries                   │
└─────────────────────────────────────────────┘
```

**Shamir's Secret Sharing Flow**:

*Upload:*
1. Generate cryptographically secure random password (20 chars)
2. Split into n shares with threshold k using byte-wise Shamir
3. Blind each share with random 32 bytes
4. Compute Keccak256 commitment for each blinded share
5. Sign request with wallet (EIP-712 typed data)
6. Upload to all nodes in parallel
7. Require minimum k + malicious nodes to succeed
8. Store secret locally in secure storage

*Download:*
1. Generate ephemeral Curve25519 keypair
2. Sign download request with wallet
3. Download encrypted shares from nodes in parallel
4. Decrypt shares using NaCl Box with ephemeral key
5. Remove blinding from decrypted shares
6. Reconstruct secret using Shamir's algorithm (requires ≥ k shares)
7. Store secret locally in secure storage

**Configuration** (`MpcConfig`):
```kotlin
data class MpcConfig(
    val partisiaRpcUrl: String,      // Blockchain RPC endpoint
    val contractAddress: HexString,   // MPC contract address
    val totalNodes: Int,              // n (total shares)
    val threshold: Int,               // k (minimum to reconstruct)
    val maliciousNodes: Int = 0       // Additional nodes required beyond k
) {
    val minSuccessfulNodes: Int get() = threshold + maliciousNodes
}
```

**Features**:
- ✅ On-demand node discovery (no persistent connections)
- ✅ Parallel node communication with structured error tracking
- ✅ Threshold-based success (handles node failures)
- ✅ EIP-712 wallet authentication for all operations
- ✅ Multi-wallet recovery support (add/remove addresses)
- ✅ Same encrypt/decrypt API as LOCAL mode

#### 4. Enclave Interface (`Enclave.kt`)
**Purpose**: Common API for both LOCAL and MPC modes (iOS-compatible via SKIE)

```kotlin
interface Enclave {
    @Throws(CancellationException::class, EnclaveError::class)
    suspend fun encrypt(message: ByteArray, receiverPublicKey: ByteArray): Pair<ByteArray, ByteArray>

    @Throws(CancellationException::class, EnclaveError::class)
    suspend fun decrypt(message: ByteArray, senderPublicKey: ByteArray): ByteArray
}
```

**Responsibilities**:
- Unified encrypt/decrypt API for both modes
- Key expiration checking (via metadata)
- Exception wrapping to EnclaveError hierarchy
- iOS compatibility via SKIE (auto-converts to Swift async/throws)

#### 5. Encryption (`Encryption.kt`)
**Purpose**: Platform-specific NaCl Box encryption (throws exceptions internally)

```kotlin
abstract class Encryption(protected val storage: SecureStorage) {
    abstract suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType
    ): Pair<ByteArray, ByteArray>

    abstract suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType
    ): ByteArray

    abstract fun generateEphemeralKeyPair(): KeyPair  // For MPC downloads

    suspend fun generateKey(userId: String, password: String, keyType: EnclaveKeyType): ByteArray
    suspend fun deleteKey(keyType: EnclaveKeyType)
    protected suspend fun getSecretKey(keyType: EnclaveKeyType): ByteArray  // throws NoKey
}
```

**Platform Implementations**:
- **JVM**: `JvmEncryption` - Lazysodium (libsodium)
- **Android**: `AndroidEncryption` - Lazysodium (libsodium JNI)
- **iOS**: `IosEncryption` - libsodium XCFramework (C interop)

**NaCl Box Encryption**:
- **Key Exchange**: Curve25519 (ECDH)
- **Cipher**: XSalsa20 stream cipher
- **MAC**: Poly1305 authenticator
- **Nonce**: 24 bytes random (generated per message)
- **Output**: nonce || ciphertext || mac (24 + message.length + 16 bytes)

**Why exceptions throughout?**
- Natural for platform code (Swift, Java throw natively)
- SKIE converts suspend + @Throws to Swift async/throws seamlessly
- Cleaner API than Result<T> for both Kotlin and Swift users

#### 6. Storage Abstractions

**SecureStorage** - Encryption key persistence:
```kotlin
interface SecureStorage {
    suspend fun storeKey(key: ByteArray, enclaveKeyType: EnclaveKeyType)
    suspend fun retrieveKey(enclaveKeyType: EnclaveKeyType): ByteArray?
    suspend fun deleteKey(enclaveKeyType: EnclaveKeyType)
}
```

**MetadataStorage** - Key metadata (expiration, type, userId):
```kotlin
interface MetadataStorage {
    suspend fun store(meta: KeyMetadata, enclaveKeyType: EnclaveKeyType)
    suspend fun get(enclaveKeyType: EnclaveKeyType): KeyMetadata?
    suspend fun delete(enclaveKeyType: EnclaveKeyType)
}

data class KeyMetadata(
    val publicKey: HexString,
    val type: EnclaveKeyType,
    val expirationType: ExpirationType,
    val expiresAt: Long?,
    val createdAt: Long,
    val lastUsedAt: Long
)

enum class EnclaveKeyType { USER, MPC }
enum class ExpirationType { TIMED, SESSION, ONE_SHOT }
```

**Platform Implementations**:
- **Android**: `AndroidSecureStorage` (EncryptedFile with StrongBox) + `AndroidMetadataStorage` (SharedPreferences)
- **iOS**: `KeychainSecureStorage` + `IosMetadataStorage` (UserDefaults)
- **JVM**: `JvmSecureStorage` (in-memory) + `JvmMetadataStorage` (in-memory, for testing)

### Error Hierarchy

```kotlin
sealed class EnclaveError : Exception {
    // Common errors (both LOCAL and MPC)
    class NoKey : EnclaveError()
    class KeyExpired : EnclaveError()
    data class DecryptionFailed(reason: DecryptFailure, details: String) : EnclaveError()
    data class EncryptionFailed(details: String) : EnclaveError()
    data class InvalidPublicKey(details: String) : EnclaveError()
    data class KeyGenerationFailed(details: String) : EnclaveError()
    data class StorageFailed(details: String, cause: Throwable?) : EnclaveError()
    data class SignatureFailed(details: String, cause: Throwable?) : EnclaveError()

    // MPC-specific errors
    data class MpcNotInitialized(details: String) : EnclaveError()
    data class MpcNotEnoughNodes(details: String) : EnclaveError()
    data class MpcUploadFailed(
        val successCount: Int,
        val required: Int,
        val failures: List<MpcNodeFailure>
    ) : EnclaveError()
    data class MpcNotEnoughShares(
        val obtained: Int,
        val required: Int,
        val failures: List<MpcNodeFailure>
    ) : EnclaveError()
    data class MpcManagementFailed(
        val operation: String,
        val successCount: Int,
        val required: Int,
        val failures: List<MpcNodeFailure>
    ) : EnclaveError()
}

sealed class DecryptFailure {
    data object WrongPassword : DecryptFailure()
    data object CorruptedData : DecryptFailure()
    data object InvalidCiphertext : DecryptFailure()
}

data class MpcNodeFailure(
    val nodeIndex: Int,
    val error: Throwable
)
```

**Benefits**:
- ✅ Type-safe error handling (exhaustive when expressions)
- ✅ iOS-compatible (no Kotlin Result type erasure)
- ✅ Detailed MPC error information (which nodes failed, why)
- ✅ Distinguishes wrong password from corrupted data
- ✅ Structured failure tracking for distributed operations

### Usage Example

#### LOCAL Mode
```kotlin
// 1. Create orchestrator for LOCAL mode
val encryption = JvmEncryption()
val storage = JvmMetadataStorage()
val orchestrator = EnclaveOrchestrator.createLocal(encryption, storage)

// 2. Observe state in UI
orchestrator.state.collect { state ->
    when (state) {
        is EnclaveState.Locked -> showPasswordPrompt()
        is EnclaveState.Unlocking -> showLoadingSpinner()
        is EnclaveState.Unlocked -> enableEncryptedFeatures()
        is EnclaveState.NotAvailable -> showSetupScreen()
    }
}

// 3. Unlock with password
val sessionConfig = EnclaveSessionConfig(ExpirationType.TIMED, 3600000)
orchestrator.unlock(userId, sessionConfig, password)

// 4. Decrypt data
try {
    val plaintext = orchestrator.withEnclave { enclave ->
        enclave.decrypt(ciphertext, senderPubKey)
    }
    display(plaintext)
} catch (e: EnclaveError) {
    when (e) {
        is EnclaveError.NoKey -> orchestrator.checkStatus()
        is EnclaveError.KeyExpired -> promptUnlock()
        is EnclaveError.DecryptionFailed ->
            when (e.reason) {
                DecryptFailure.WrongPassword -> showError("Wrong password")
                DecryptFailure.CorruptedData -> reportError()
                else -> showError(e.details)
            }
        else -> showError(e.message)
    }
}
```

#### MPC Mode
```kotlin
// 1. Create orchestrator for MPC mode
val mpcConfig = MpcConfig(
    partisiaRpcUrl = "https://rpc.partisia.com",
    contractAddress = HexString("0x..."),
    totalNodes = 3,
    threshold = 2
)
val orchestrator = EnclaveOrchestrator.createMpc(
    encryption, storage, mpcConfig, signer, Keccak256Hasher()
)

// 2. Enroll user (first time)
try {
    orchestrator.enroll(userId, EnclaveKeyType.MPC)
    // State automatically transitions to Unlocked
} catch (e: EnclaveError.MpcUploadFailed) {
    showError("Upload failed: ${e.successCount}/${e.required} nodes succeeded")
    e.failures.forEach { failure ->
        log("Node ${failure.nodeIndex}: ${failure.error.message}")
    }
}

// 3. Unlock on subsequent sessions
val sessionConfig = EnclaveSessionConfig(ExpirationType.TIMED, 3600000)
orchestrator.unlock(userId, sessionConfig)  // No password needed

// 4. Use same decrypt API
orchestrator.withEnclave { enclave ->
    enclave.decrypt(ciphertext, senderPubKey)
}
```

### Engineering Notes

**Error Handling Pattern**:
- All layers throw exceptions (natural for platform code)
- SKIE automatically converts to Swift async/throws
- Consistent error handling across all platforms
- MPC errors include detailed failure information

**State Management**:
- StateFlow for reactive UI updates
- Works with Android Compose `collectAsState()`
- Works with iOS SwiftUI via Combine wrapper
- 4-state model: Locked, Unlocking, Unlocked, NotAvailable

**Thread Safety**:
- Mutex-protected storage access in platform implementations
- Safe concurrent operations on EnclaveOrchestrator
- Coroutine-based concurrency throughout

**Key Derivation (LOCAL)**:
- Scrypt (memory-hard, OWASP recommended)
- Parameters: n=16384, r=8, p=1, dkLen=32
- Salt: userId (unique per user)
- Password normalization (Unicode NFC)

**MPC Security**:
- Shamir's Secret Sharing (threshold k of n nodes)
- Share blinding for added security
- Keccak256 commitments for integrity
- EIP-712 wallet authentication
- Ephemeral key encryption for downloads
- No single point of failure (distributed trust)

**Security Features**:
- Key expiration (TIMED, SESSION, ONE_SHOT)
- Secure key erasure (`ByteArray.fill(0)`)
- Platform secure storage (Keychain, EncryptedFile with StrongBox)
- Hardware-backed encryption when available

---

## 🔐 Security Layer (`kwil/security/`)

### Authentication (KGW)
```kotlin
// 1. Get challenge
val challenge = protocol.getChallenge()

// 2. Sign challenge
val signature = signer.sign(challenge.toByteArray())

// 3. Authenticate (creates session cookie)
protocol.authenticate(challenge, signature, signer.accountId())

// 4. Subsequent requests automatically use cookie
```

### Signers
- **Common**: `Signer` interface
- **JVM**: `JvmEthSigner` (uses KEthereum)
- **iOS/Android**: Platform-specific implementations

### Signature Types
- `SECP256K1_PERSONAL` - EIP-191 personal sign (Ethereum)
- `ED25519` - Edwards curve (Solana, etc.)

---

## 🧪 Testing

### Test Files
- `jvmTest/ApiIntegrationTests.kt` - Full integration tests
- `jvmTest/SerializationIntegrityTests.kt` - Encoding tests
- `jvmTest/Utils.kt` - Test utilities

### Test Pattern
```kotlin
@Test
fun testWalletOperations() = runBlocking {
    val client = createTestClient()

    client.wallets.add(walletParams)
        .onSuccess { txHash ->
            println("✅ Wallet added: $txHash")
        }
        .onFailure { error ->
            fail("❌ Failed: ${error.message}")
        }
}
```

---

## ❌ Error Handling Strategy

### Public API (Suspend + @Throws)
```kotlin
// ✅ All public APIs use suspend + @Throws
@Throws(DomainError::class)
suspend fun getUser(): GetUserResponse

@Throws(DomainError::class)
suspend fun addWallet(input: AddWalletParams): HexString

// User code wraps in try-catch
try {
    val user = client.users.get()
} catch (e: DomainError) {
    // Handle error
}
```

### Error Hierarchy
```
Exception
├── DomainError (public)
│   ├── ActionFailed
│   ├── ValidationError
│   ├── NotFound
│   ├── AuthenticationRequired
│   └── Unknown
│
├── ProtocolError (internal)
│   ├── RpcError
│   ├── InvalidResponse
│   ├── TransactionFailed
│   └── MissingAuthenticationException
│
└── TransportError (internal)
    ├── NetworkError
    ├── HttpError
    └── SerializationError
```

### iOS Compatibility
All errors extend `Exception` and are automatically converted to Swift errors by SKIE. The `@Throws` annotation ensures proper Swift error handling with do-try-catch.

---

## 📱 Platform Specifics

### JVM
- Signer: `JvmEthSigner` using KEthereum
- HTTP: Ktor OkHttp engine
- Crypto: kotlincrypto for SHA-256

### Android
- Same as JVM (shares `jvmMain` code)
- Additional: Lazysodium for libsodium

### iOS
- HTTP: Ktor Darwin engine
- Crypto: libsodium XCFramework
- Value classes not yet supported (use data classes)

---

## 🚀 Quick Start

```kotlin
try {
    // 1. Create Ethereum signer
    val signer = JvmEthSigner(ecKeyPair)

    // 2. Create client
    val client = IdosClient.create(
        baseUrl = "https://nodes.staging.idos.network",
        chainId = "idos-testnet",
        signer = signer
    )

    // 3. Use grouped APIs
    val user = client.users.get()
    println("User: ${user.id}")

    val txHash = client.wallets.add(AddWalletParams(id, address, publicKey, signature))
    println("Wallet added: $txHash")
} catch (e: DomainError) {
    println("Error: ${e.message}")
}
```

---

## 🔄 Adding New Operations

### 1. Update Schema
Update Kuneiform schema in `idos-schema` repository

### 2. Regenerate Actions
Run code generator to update `domain/generated/`

### 3. Add Extension
Add to `IdosClientExtensions.kt`:
```kotlin
@Throws(DomainError::class)
suspend fun IdosClient.MyGroup.myOperation(input: MyParams): MyResponse =
    executor.execute(MyAction, input)
```

### 4. Document in IdosClient
Update KDoc in operation group class

---

## 📚 Key Takeaways

1. **4-Layer Architecture**: Transport → Protocol → Domain → Public API
2. **Suspend + @Throws Public API**: All operations use suspend with domain error throwing
3. **Generated Schema**: `domain/generated/` is the source of truth
4. **Clean Separation**: Structure (IdosClient) + Behavior (Extensions)
5. **KWIL Signature Scheme**: Payload digest + metadata, not raw JSON
6. **Auto-retry Auth**: `ActionExecutor` handles authentication transparently
7. **Type Safety**: `KwilType` sealed class, type alias wrappers (HexString, UuidString, Base64String)
8. **Platform Support**: JVM, Android, iOS via Kotlin Multiplatform + SKIE
9. **iOS Compatibility**: SKIE auto-converts suspend/throws to Swift async/throws

---

## 📖 Additional Resources

- KWIL Protocol: https://github.com/kwilteam/kwil-db
- idOS Schema: https://github.com/idos-networks/idos-schema
- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform.html
