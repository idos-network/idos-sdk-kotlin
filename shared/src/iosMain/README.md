# iOS Interop Layer

This package provides seamless integration between the Kotlin SDK and Swift/iOS applications via SKIE.

## 📦 What's Included

### 1. ByteArray Extensions (`ByteArrayExtensions.kt`)

Bidirectional conversion between Kotlin `ByteArray` and Swift `Data`:

```swift
import idos_sdk

// Kotlin ByteArray -> Swift Data (SKIE automatic conversion)
let data: Data = kotlinByteArray.toData()

// Swift Data -> Kotlin ByteArray (SKIE automatic conversion)
let byteArray: KotlinByteArray = data.toKotlinByteArray()
```

### 2. Error Extensions (`ErrorExtensions.kt`)

Extract Kotlin exceptions from Swift `NSError` for proper error handling:

```swift
import idos_sdk

do {
    try await client.wallets.add(params)
} catch let error as NSError {
    // Extract DomainError
    if let domainError = error.asDomainError() {
        switch onEnum(of: domainError) {
        case .validationError(let e):
            print("Validation failed: \(e.message)")
        case .authenticationRequired(let e):
            print("Auth required: \(e.message)")
        case .notFound(let e):
            print("Not found: \(e.message)")
        default:
            print("Error: \(domainError.message)")
        }
    }

    // Extract EnclaveError
    if let enclaveError = error.asEnclaveError() {
        switch onEnum(of: enclaveError) {
        case .noKey:
            showPasswordPrompt()
        case .keyExpired:
            regenerateKey()
        case .decryptionFailed(let e):
            print("Decryption failed: \(e.reason)")
        default:
            print("Enclave error: \(enclaveError.message)")
        }
    }
}
```

### 3. Type Aliases (Automatic SKIE Export)

**Type aliases for Kotlin type wrappers** (UuidString, HexString, Base64String).

In Kotlin, `UuidString`, `HexString`, and `Base64String` are defined as typealiases to `String`. SKIE automatically exports them to Swift as plain `String` types - no special unwrapping needed!

```swift
import idos_sdk

// Kotlin typealiases are exported as String to Swift - direct access!
let id: String = response.id
let userId: String = response.userId

// Use String directly in API calls
let credential = try await client.credentials.getOwned(id: "550e8400-...")
await orchestrator.unlock(userId: "some-uuid", password: "password", expirationMillis: 3600000)

// Generate UUIDs with Swift's UUID type
let newId = UUID().uuidString.lowercased()
```

**No helper functions needed!** Since typealiases are now just `String` in both Kotlin and Swift, you work with them as regular strings.

### 4. KeychainSecureStorage (`iosMain/swift/enclave/KeychainSecureStorage.swift`)

**Production-ready iOS Keychain implementation** that implements the `SecureStorage` interface.

The Swift implementation is bundled with the SDK via SKIE and is automatically available when you import the SDK.

## 🚀 Quick Start

### Step 1: Setup EnclaveOrchestrator (Recommended)

```swift
import idos_sdk

// Use the built-in Keychain storage (recommended for production)
let storage = KeychainSecureStorage()  // From iosSwift package - bundled with SDK
let orchestrator = EnclaveOrchestrator.create(
    encryption: IosEncryption(storage: storage),
    metadataStorage: IosMetadataStorage()
)
```

### Step 2: Observe Enclave State

```swift
import Combine

class EnclaveViewModel: ObservableObject {
    @Published var state: EnclaveState = EnclaveState.Locked()
    private var cancellables = Set<AnyCancellable>()
    private let orchestrator: EnclaveOrchestrator

    init(orchestrator: EnclaveOrchestrator) {
        self.orchestrator = orchestrator

        // Observe state changes
        createPublisher(orchestrator.state)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in
                self?.state = newState
            }
            .store(in: &cancellables)
    }

    func unlock(userId: String, password: String) async {
        await orchestrator.unlock(
            userId: userId,  // UuidString is just String in Swift
            password: password,
            expirationMillis: 3600000 // 1 hour
        )
    }

    func lock() async {
        await orchestrator.lock()
    }
}
```

### Step 3: Use in SwiftUI

```swift
import SwiftUI

struct EnclaveView: View {
    @StateObject var viewModel: EnclaveViewModel
    @State private var password = ""

    var body: some View {
        VStack {
            switch onEnum(of: viewModel.state) {
            case .locked:
                // Show password prompt
                SecureField("Password", text: $password)
                Button("Unlock") {
                    Task {
                        await viewModel.unlock(
                            userId: userProfile.id,
                            password: password
                        )
                    }
                }

            case .unlocking:
                ProgressView("Generating key...")

            case .unlocked(let state):
                // Show encrypted features
                Text("Enclave unlocked")
                Button("Lock") {
                    Task {
                        await viewModel.lock()
                    }
                }

                Button("Decrypt Data") {
                    Task {
                        do {
                            let plaintext = try await state.enclave.decrypt(
                                message: encryptedData.toKotlinByteArray(),
                                senderPublicKey: pubkey.toKotlinByteArray()
                            )
                            // Use decrypted data
                        } catch let error as NSError {
                            if let enclaveError = error.asEnclaveError() {
                                // Handle error
                            }
                        }
                    }
                }
            }
        }
    }
}
```

## 🔐 Security Notes

### Production Storage

The SDK includes `KeychainSecureStorage` (via SKIE bundling) which provides:

✅ **Persistent storage** - Keys survive app restarts
✅ **Maximum security** - `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`
✅ **No iCloud backup** - Keys never leave the device
✅ **Hardware-backed** - Uses Secure Enclave when available

This matches Android's `EncryptedFile` + `StrongBox` security level.

```swift
// ✅ PRODUCTION (recommended)
let storage = KeychainSecureStorage()  // Bundled with SDK via SKIE
```

## 📚 Complete Example

```swift
import idos_sdk
import Combine

class IdosService {
    let client: IdosClient
    let orchestrator: EnclaveOrchestrator

    init(signer: EthSigner) throws {
        // Create IdosClient
        self.client = try IdosClient.Companion.shared.create(
            baseUrl: "https://nodes.staging.idos.network",
            chainId: "idos-testnet",
            signer: signer
        )

        // Setup Enclave with production Keychain storage
        let storage = KeychainSecureStorage()  // Bundled with SDK
        self.orchestrator = EnclaveOrchestrator.create(
            encryption: IosEncryption(storage: storage),
            metadataStorage: IosMetadataStorage()
        )
    }

    func unlockEnclave(userId: String, password: String) async {
        await orchestrator.unlock(
            userId: userId,  // UuidString is just String in Swift
            password: password,
            expirationMillis: 3600000
        )
    }

    func decryptCredential(id: String) async throws -> Data {
        // Get credential from idOS
        let credential = try await client.credentials.getOwned(id: id)  // UuidString is just String

        // Decrypt using enclave
        return try await orchestrator.withEnclave { enclave in
            let plaintext = try await enclave.decrypt(
                message: credential.content.toByteArray(),
                senderPublicKey: credential.encryptorPublicKey.toByteArray()
            )
            return plaintext.toData()
        }
    }
}
```

## 🔧 Advanced: Custom Storage Implementation

If you need custom storage behavior, implement `SecureStorage` interface in Swift:

```swift
import idos_sdk

class CustomSecureStorage: SecureStorage {
    func storeKey(key: KotlinByteArray) async throws {
        // Your custom implementation
    }

    func retrieveKey() async throws -> KotlinByteArray? {
        // Your custom implementation
    }

    func deleteKey() async throws {
        // Your custom implementation
    }
}

// Use it:
let storage = CustomSecureStorage()
let orchestrator = EnclaveOrchestrator.create(
    encryption: IosEncryption(storage: storage),
    metadataStorage: IosMetadataStorage()
)
```

For most cases, the built-in `KeychainSecureStorage` is recommended.

## 📚 See Also

- Keychain implementation: `shared/src/iosMain/swift/enclave/KeychainSecureStorage.swift`
- ByteArray extensions: `shared/src/iosMain/kotlin/interop/ByteArrayExtensions.kt`
- Error extensions: `shared/src/iosMain/kotlin/interop/ErrorExtensions.kt`
- iOS platform implementations: `shared/src/iosMain/kotlin/`
- SDK documentation: `README.md`
- Architecture details: `ARCHITECTURE.md`
- SKIE documentation: https://skie.touchlab.co/
