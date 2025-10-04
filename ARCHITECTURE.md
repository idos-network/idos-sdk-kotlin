# idOS SDK Architecture

Kotlin Multiplatform SDK for idOS - Android, iOS, and JVM support with KWIL database backend.

## 🏗️ Layer Architecture

```
┌─────────────────────────────────────────────┐
│  Public API (IdosClient + Extensions)       │  Result<T>
├─────────────────────────────────────────────┤
│  Domain (ActionExecutor, Generated Actions) │  Result<T>
├─────────────────────────────────────────────┤
│  Protocol (KWIL RPC, Transaction Signing)   │  throws
├─────────────────────────────────────────────┤
│  Transport (JSON-RPC over HTTP)             │  throws
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
ActionExecutor (Result wrapper)
  ↓ runCatchingError { client.callAction(GetUser, signer) }
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
  ← Result.success(GetUserResponse)
User Code
  ← Result<GetUserResponse>
```

### Write Operation (Execute Action)
```
User Code
  ↓ client.wallets.add(params)
IdosClientExtensions
  ↓ executor.execute(AddWallet, params)
ActionExecutor
  ↓ runCatchingError { client.executeAction(AddWallet, params, signer) }
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
  ← Result.success(HexString(txHash))
User Code
  ← Result<HexString>
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

**Error Handling**: Throws exceptions (converted to Result at domain boundary)

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

**Purpose**: Type-safe schema actions and Result-based error handling

**Key Components**:

1. **ActionExecutor.kt** - Public API executor
   ```kotlin
   class ActionExecutor {
       suspend fun <I, O> call(action: ViewAction<I, O>, input: I): Result<List<O>>
       suspend fun <O> callSingle(action: NoParamsAction<O>): Result<O>
       suspend fun <I> execute(action: ExecuteAction<I>, input: I): Result<HexString>
   }
   ```
   - Wraps all protocol calls in `runCatchingError { ... }`
   - Converts exceptions to `DomainError`
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
- Domain is the boundary between internal (throws) and public (Result) APIs
- All exceptions converted to `DomainError` types
- `callSingle()` uses `.mapCatching { it.single() }` - "not found" becomes error
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
suspend fun IdosClient.Wallets.add(input: AddWalletParams): Result<HexString> =
    executor.execute(AddWallet, input)

suspend fun IdosClient.Wallets.getAll(): Result<List<GetWalletsResponse>> =
    executor.call(GetWallets)
```

**Benefits**:
- ✅ IDE autocomplete groups operations by domain
- ✅ Easy to add/remove operations (copy-paste pattern)
- ✅ Clean separation: structure vs. behavior
- ✅ Simple to generate from schema

**Usage**:
```kotlin
val client = IdosClient.create(baseUrl, chainId, signer).getOrThrow()

// IDE shows: wallets, credentials, accessGrants, users, attributes
client.wallets.add(walletParams)
    .onSuccess { txHash -> println("Added: $txHash") }
    .onFailure { error -> println("Error: ${error.message}") }
```

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

### Public API (Result-based)
```kotlin
// ✅ All public APIs return Result<T>
suspend fun getUser(): Result<GetUserResponse>
suspend fun addWallet(input: AddWalletParams): Result<HexString>

// ❌ Never return nullable for suspend functions (hides exceptions)
suspend fun getUser(): GetUserResponse? // WRONG!
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
All errors extend `Exception` to work with Kotlin/Native's exception model. iOS wrapper can be added in `iosMain/` if needed.

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
// 1. Create Ethereum signer
val signer = JvmEthSigner(ecKeyPair)

// 2. Create client
val client = IdosClient.create(
    baseUrl = "https://nodes.staging.idos.network",
    chainId = "idos-testnet",
    signer = signer
).getOrThrow()

// 3. Use grouped APIs
client.users.get()
    .onSuccess { user -> println("User: ${user.id}") }
    .onFailure { error -> println("Error: ${error.message}") }

client.wallets.add(AddWalletParams(id, address, publicKey, signature))
    .onSuccess { txHash -> println("Wallet added: $txHash") }
    .onFailure { error -> println("Failed: ${error.message}") }
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
suspend fun IdosClient.MyGroup.myOperation(input: MyParams): Result<MyResponse> =
    executor.execute(MyAction, input)
```

### 4. Document in IdosClient
Update KDoc in operation group class

---

## 📚 Key Takeaways

1. **4-Layer Architecture**: Transport → Protocol → Domain → Public API
2. **Result-based Public API**: All suspend functions return `Result<T>`
3. **Generated Schema**: `domain/generated/` is the source of truth
4. **Clean Separation**: Structure (IdosClient) + Behavior (Extensions)
5. **KWIL Signature Scheme**: Payload digest + metadata, not raw JSON
6. **Auto-retry Auth**: `ActionExecutor` handles authentication transparently
7. **Type Safety**: `KwilType` sealed class, value class wrappers
8. **Platform Support**: JVM, Android, iOS via Kotlin Multiplatform

---

## 📖 Additional Resources

- KWIL Protocol: https://github.com/kwilteam/kwil-db
- idOS Schema: https://github.com/idos-networks/idos-schema
- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform.html
