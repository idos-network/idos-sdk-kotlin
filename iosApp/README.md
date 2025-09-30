# IDOS iOS App

iOS application for the IDOS SDK, built using Kotlin Multiplatform Mobile (KMM) with SwiftUI.

## Architecture

The iOS app follows the same architecture as the Android app:

- **MVVM Pattern**: ViewModels manage state and business logic, Views observe and display
- **SwiftUI**: Modern declarative UI framework
- **Shared Business Logic**: KMM shared module for cross-platform code
- **Dependency Injection**: DIContainer pattern (similar to Android's Koin)
- **Navigation**: NavigationCoordinator pattern (similar to Android's NavigationManager)

## Project Structure

```
iosApp/
├── iosApp/
│   ├── App/
│   │   ├── iosApp.swift           # App entry point
│   │   └── DIContainer.swift      # Dependency injection container
│   ├── Core/
│   │   ├── Theme/
│   │   │   └── AppTheme.swift     # App theme (colors, typography, spacing)
│   │   └── Navigation/
│   │       └── NavigationCoordinator.swift  # Navigation management
│   ├── Features/
│   │   ├── Login/
│   │   │   ├── LoginView.swift
│   │   │   ├── LoginViewModel.swift
│   │   │   └── MnemonicView.swift
│   │   ├── Dashboard/
│   │   │   └── DashboardView.swift
│   │   ├── Credentials/
│   │   │   ├── CredentialsView.swift
│   │   │   ├── CredentialsViewModel.swift
│   │   │   ├── CredentialDetailView.swift
│   │   │   └── CredentialDetailViewModel.swift
│   │   ├── Wallets/
│   │   │   ├── WalletsView.swift
│   │   │   └── WalletsViewModel.swift
│   │   └── Settings/
│   │       ├── SettingsView.swift
│   │       └── SettingsViewModel.swift
│   ├── Data/
│   │   └── Storage/
│   │       └── StorageManager.swift  # UserDefaults wrapper
│   ├── Security/
│   │   └── KeyManager.swift      # Keychain management
│   ├── UI/
│   │   ├── Base/
│   │   │   ├── BaseViewModel.swift
│   │   │   ├── BaseEnclaveViewModel.swift
│   │   │   ├── EnclaveState.swift
│   │   │   └── KeyGenerationDialog.swift
│   │   └── Components/
│   │       └── CommonViews.swift
│   └── Resources/
│       └── Assets.xcassets
└── iosApp.xcodeproj/
```

## iOS Platform Implementations (Shared Module)

The shared KMM module requires iOS-specific implementations:

### Completed Implementations

1. **MetadataStorage.ios.kt** - Uses UserDefaults for key metadata storage
2. **Encryption.ios.kt** - ⚠️ Stub (requires implementation)
3. **KeyDerivation.ios.kt** - ⚠️ Stub (requires implementation)

### Required Implementations

#### 1. Encryption (Encryption.ios.kt)

**Requirements:**
- Implement NaCl Box encryption (Curve25519 + XSalsa20 + Poly1305)
- Store keys securely in Keychain with `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`
- Use Secure Enclave when available
- Match Android's message format: nonce (24 bytes) + ciphertext

**Recommended Libraries:**
- CryptoKit (built-in, iOS 13+) - for basic crypto operations
- SwiftSodium (libsodium wrapper) - for NaCl compatibility
- Sodium framework - alternative libsodium binding

#### 2. KeyDerivation (KeyDerivation.ios.kt)

**Requirements:**
- Implement SCrypt KDF with parameters: n=16384, r=8, p=1, dkLen=32
- Normalize passwords using NFKC (String.precomposedStringWithCompatibilityMapping)
- Validate salt as UUID

**Recommended Libraries:**
- CryptoKit (built-in) - has some KDF support
- OpenSSL via CocoaPods - for SCrypt
- Custom SCrypt implementation

## Building the Project

### Prerequisites

- macOS with Xcode 14.0+
- CocoaPods (if using third-party crypto libraries)
- Gradle (for building KMM shared module)

### Build Steps

1. **Build the shared framework:**
   ```bash
   cd ../
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```

2. **Open in Xcode:**
   ```bash
   open iosApp.xcodeproj
   ```

3. **Select target and run:**
   - Select "iosApp" scheme
   - Choose simulator or device
   - Press Cmd+R to build and run

## Key Features

### 1. Login Flow
- Welcome screen with "Connect Wallet" button
- Mnemonic import (12/24 words)
- Automatic navigation to Dashboard after wallet connection

### 2. Dashboard
- Tab-based navigation (Credentials, Wallets, Settings)
- Matches Android's drawer navigation pattern

### 3. Credentials Management
- List of encrypted credentials
- Pull-to-refresh functionality
- Credential detail view with JSON viewer
- Copy to clipboard support
- Automatic key generation prompt when needed

### 4. Enclave Integration
- `BaseEnclaveViewModel` for screens requiring encryption
- Automatic key generation dialog when key missing/expired
- Password input with visibility toggle
- Key expiration selection (1 Day, 1 Week, 1 Month)

### 5. Settings
- View encryption key status
- Delete encryption key
- Disconnect wallet
- App version information

## State Management

### ViewModel Pattern

```swift
class CredentialsViewModel: BaseEnclaveViewModel<CredentialsState, CredentialsEvent> {
    @Published var state = CredentialsState()

    func onEvent(_ event: CredentialsEvent) {
        // Handle events
    }
}
```

### State/Event Pattern

```swift
struct CredentialsState {
    var credentials: [Credential] = []
    var isLoading: Bool = false
    var error: String? = nil
}

enum CredentialsEvent {
    case loadCredentials
    case credentialSelected(Credential)
    case clearError
}
```

## Navigation

### NavigationCoordinator

```swift
// Navigate forward
navigationCoordinator.navigate(to: .credentialDetail(credentialId: "123"))

// Navigate back
navigationCoordinator.navigateUp()

// Replace navigation stack
navigationCoordinator.replace(with: .dashboard)

// Present sheet
navigationCoordinator.presentSheet(.settings)
```

## Security

### KeyManager (Keychain)

```swift
let keyManager = KeyManager()

// Store key
try keyManager.storeKey(keyData)

// Retrieve key
let keyData = try keyManager.getKey()

// Delete key
try keyManager.deleteKey()
```

### Security Best Practices

- All encryption keys stored in Keychain
- `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` for maximum security
- Automatic key expiration handling
- Secure key generation with user-provided password
- No sensitive data in UserDefaults

## Testing

### Unit Tests
- ViewModel state transitions
- Navigation coordinator logic
- KeyManager operations

### UI Tests
- Login flow
- Navigation between screens
- Key generation dialog
- Credential list and detail views

## Known Limitations

1. **Encryption not implemented** - iOS platform encryption requires SwiftSodium or similar
2. **Key derivation not implemented** - Requires SCrypt library
3. **Mnemonic derivation not implemented** - Requires BIP39/BIP44 library (e.g., web3.swift)
4. **API integration incomplete** - Currently uses mock data
5. **No Ethereum signing** - Requires secp256k1 implementation

## Next Steps

### Phase 1: Complete Cryptography
1. Add SwiftSodium via CocoaPods
2. Implement `Encryption.ios.kt` using libsodium
3. Implement `KeyDerivation.ios.kt` with SCrypt
4. Test encryption compatibility with Android

### Phase 2: Wallet Integration
1. Add web3.swift or similar library
2. Implement BIP39/BIP44 mnemonic derivation
3. Implement Ethereum key derivation
4. Add transaction signing

### Phase 3: API Integration
1. Create API client wrapper
2. Implement UserRepository
3. Implement CredentialsRepository
4. Implement WalletRepository
5. Add proper error handling

### Phase 4: Polish
1. Add splash screen
2. Improve animations and transitions
3. Add loading indicators
4. Improve error messages
5. Add localization support

## Dependencies

### Required CocoaPods (to be added)

```ruby
pod 'SwiftSodium', '~> 0.9.1'        # For NaCl encryption
pod 'web3.swift', '~> 1.6.0'         # For Ethereum wallet
pod 'CryptoSwift', '~> 1.8.0'        # For additional crypto
```

## Compatibility

- **iOS**: 15.0+
- **Swift**: 5.0+
- **Xcode**: 14.0+

## Support

For issues and questions:
- Check the main project README
- Review the Android implementation for reference
- Consult the IOS_IMPLEMENTATION_CONTEXT.md for detailed architecture

## License

See main project LICENSE file.