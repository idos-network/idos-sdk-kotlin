# idOS iOS App

Native iOS application for the idOS SDK, built with SwiftUI and Kotlin Multiplatform.

## 🏗 Architecture

The iOS app mirrors the Android app architecture for consistency:

- **MVVM Pattern**: State-driven ViewModels with event handling
- **SwiftUI**: Modern declarative UI framework
- **Repository Pattern**: Clean data layer abstraction
- **Dependency Injection**: Centralized DIContainer (matching Android's Koin)
- **SKIE Integration**: Seamless Kotlin ↔ Swift interop for shared business logic
- **Navigation Coordinator**: Centralized navigation management

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

## ✅ Completed iOS Platform Implementations

The shared Kotlin module uses SKIE for seamless Swift interop:

### Core Implementations

1. **Encryption.ios.kt** - ✅ Implemented using Kotlin Multiplatform libsodium bindings
   - NaCl Box encryption (Curve25519 + XSalsa20 + Poly1305)
   - Secure key storage via `KeychainSecureStorage` (bundled with SDK)
   - Full compatibility with Android encryption

2. **KeyDerivation.ios.kt** - ✅ Implemented using Kotlin Multiplatform libsodium
   - SCrypt KDF with parameters: n=16384, r=8, p=1, dkLen=32
   - Password normalization
   - UUID salt validation

3. **MetadataStorage.ios.kt** - ✅ UserDefaults-based key metadata storage

4. **KeychainSecureStorage.swift** - ✅ Production-ready Keychain implementation
   - `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` for maximum security
   - Hardware-backed via Secure Enclave when available
   - Bundled with SDK via SKIE

### SKIE Integration Benefits

- **Automatic Conversion**: Kotlin suspend functions → Swift async/await
- **Error Handling**: Kotlin exceptions → Swift throws
- **Type Safety**: Sealed classes → Swift enums with `onEnum()` helper
- **No Manual Bridging**: Direct Swift access to Kotlin APIs

## 🚀 Setup & Building

### Prerequisites

- **Xcode**: 15.0+
- **CocoaPods**: For TrustWallet dependency
- **macOS**: 13.0+
- **iOS**: 15.0+ deployment target

### Installation Steps

1. **Install CocoaPods Dependencies**
   ```bash
   cd iosApp
   pod install
   ```

2. **Configure Environment (Optional)**

   Create `.env` in project root for development mnemonic:
   ```bash
   # At repository root (idos-sdk-kotlin/)
   echo 'MNEMONIC_WORDS="your twelve word mnemonic phrase here"' > .env
   ```

3. **Open Workspace** ⚠️ Important
   ```bash
   open iosApp.xcworkspace  # Always use .xcworkspace, NOT .xcodeproj
   ```

4. **Build & Run**
   - Select "iosApp" scheme
   - Choose simulator or device
   - Press ⌘R to build and run

### Build Process

The Xcode build automatically:
1. ✅ Checks CocoaPods dependencies
2. ✅ Generates `Config.swift` from `.env` file
3. ✅ Builds Kotlin shared framework via Gradle
4. ✅ Compiles Swift code
5. ✅ Embeds TrustWallet framework

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
- Automatic key generation prompt when needed

### 4. Enclave Integration
- `CredentialViewModel` as example for screens requiring encryption
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

## 🔧 Build Configuration

### Build Phases

1. **[CP] Check Pods Manifest.lock** - CocoaPods verification
2. **Generate Config** - Creates `Config.swift` from `.env` file
3. **Build Kotlin Framework** - Compiles shared Kotlin module
4. **Sources** - Compiles Swift code
5. **[CP] Embed Pods Frameworks** - Embeds TrustWallet framework

### Key Build Settings

- **ENABLE_USER_SCRIPT_SANDBOXING**: `NO` (disabled for Kotlin framework build)
- **IPHONEOS_DEPLOYMENT_TARGET**: `15.6`
- **SWIFT_VERSION**: `5.0`
- **DEVELOPMENT_TEAM**: Set for code signing

### Build Configuration Notes

#### User Script Sandboxing Disabled

**Why it's disabled:**
- The "Build Kotlin Framework" phase runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`
- Gradle needs broad file system access to:
  - Read/write to `.gradle` cache
  - Access Kotlin compiler
  - Download dependencies
  - Generate framework files
- Xcode's sandbox restrictions are incompatible with Gradle's operation

**Security implications:**
- Build scripts have full file system access during build
- This is standard for projects with complex build dependencies
- CocoaPods and other build tools operate the same way

**Note:** If sandboxing were enabled, you'd see:
```
Sandbox: deny(1) file-read-data /Users/.../generate-config.sh
```

#### TrustWallet Quoted Include Warnings

TrustWallet uses `"quoted"` includes instead of `<angle-bracketed>`. Fixed in `Podfile`:

```ruby
post_install do |installer|
  installer.pods_project.targets.each do |target|
    target.build_configurations.each do |config|
      # Disable warning for TrustWallet's header style
      config.build_settings['CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER'] = 'NO'
    end
  end
end
```

**Error without fix**: `double-quoted include "TWBase.h" in framework header, expected angle-bracketed instead`

### Common Build Issues

| Issue | Solution |
|-------|----------|
| "Framework not found idos_sdk" | Run `./gradlew :shared:embedAndSignAppleFrameworkForXcode` from project root |
| "Module compiled with Swift X.Y" | Clean build folder (⌘⇧K) and rebuild |
| "Task ... execution failed" (Gradle) | Check Gradle daemon: `./gradlew --stop` then rebuild |
| Quoted include warnings | Fixed in Podfile post_install hook ✅ |
| Build script permissions | Sandboxing disabled for Gradle compatibility ✅ |

## 📚 Dependencies

### CocoaPods

```ruby
# Podfile
pod 'TrustWalletCore', '~> 4.0'  # Ethereum wallet operations
```

**TrustWalletCore** provides:
- BIP39 mnemonic generation
- BIP44 hierarchical key derivation
- secp256k1 signing for Ethereum
- Production-proven (used by Trust Wallet app)

### Kotlin Multiplatform Framework

- **idos_sdk**: Shared business logic framework
  - Automatically built by Gradle during Xcode build
  - Located at `../shared/build/xcode-frameworks/`
  - Includes SKIE for seamless Swift interop

## 📖 Additional Resources

- **[iOS SDK Documentation](../shared/src/iosMain/README.md)** - Kotlin ↔ Swift interop guide
- **[Main README](../README.md)** - Overall SDK documentation
- **[Architecture Guide](../ARCHITECTURE.md)** - Technical architecture details
- **[SKIE Documentation](https://skie.touchlab.co/)** - Kotlin/Swift interop

## 🤝 Contributing

When contributing iOS code:

1. Follow Swift naming conventions (camelCase, PascalCase for types)
2. Mirror Android's architecture patterns where possible
3. Use MVVM with state-driven ViewModels
4. Document any iOS-specific workarounds
5. Update this README for significant changes
6. Test on both simulator and device

## 📄 License

Same license as the main SDK - see repository root.
