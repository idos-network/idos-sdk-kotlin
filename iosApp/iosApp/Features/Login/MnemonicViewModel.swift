import Foundation
import Combine

/// Mnemonic state matching Android's MnemonicState
struct MnemonicState {
    var mnemonic: String = ""
    var isLoading: Bool = false
    var isSuccess: Bool = false
    var error: String? = nil
}

/// Mnemonic events matching Android's MnemonicEvent
enum MnemonicEvent {
    case updateMnemonic(String)
    case importWallet
    case fetchUser
    case clearError
}

/// MnemonicViewModel matching Android's MnemonicViewModel
class MnemonicViewModel: BaseViewModel<MnemonicState, MnemonicEvent> {
    private let keyManager: KeyManager
    private let storageManager: StorageManager
    private let userRepository: UserRepositoryProtocol
    private let navigationCoordinator: NavigationCoordinator

    init(
        keyManager: KeyManager,
        storageManager: StorageManager,
        userRepository: UserRepositoryProtocol,
        navigationCoordinator: NavigationCoordinator
    ) {
        self.keyManager = keyManager
        self.storageManager = storageManager
        self.userRepository = userRepository
        self.navigationCoordinator = navigationCoordinator

        // Initialize with development mnemonic if available
        var initialState = MnemonicState()
        #if DEBUG
        initialState.mnemonic = Config.developmentMnemonic
        #endif

        super.init(initialState: initialState)
    }

    override func onEvent(_ event: MnemonicEvent) {
        switch event {
        case .updateMnemonic(let mnemonic):
            updateMnemonic(mnemonic)
        case .importWallet:
            importWallet()
        case .fetchUser:
            fetchUser()
        case .clearError:
            updateState { $0.error = nil }
        }
    }

    private func updateMnemonic(_ mnemonic: String) {
        updateState { $0.mnemonic = mnemonic }
    }

    private func importWallet() {
        print("🔐 MnemonicViewModel: Starting wallet import")
        updateState {
            $0.isLoading = true
            $0.error = nil
        }

        Task {
            do {
                // Validate mnemonic (12 or 24 words)
                let words = state.mnemonic
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                    .components(separatedBy: .whitespaces)
                    .filter { !$0.isEmpty }

                guard words.count == 12 || words.count == 24 else {
                    print("❌ MnemonicViewModel: Invalid mnemonic word count: \(words.count)")
                    await MainActor.run {
                        updateState {
                            $0.error = "Please enter a valid 12 or 24 word recovery phrase"
                            $0.isLoading = false
                        }
                    }
                    return
                }

                print("✅ MnemonicViewModel: Mnemonic validated (\(words.count) words)")
                print("🔑 MnemonicViewModel: Generating and storing private key")

                // Generate and store key using KeyManager (matches Android implementation)
                // This derives the private key using BIP39/BIP44 and stores it securely
                let address = try keyManager.generateAndStoreKey(words: state.mnemonic)

                print("✅ MnemonicViewModel: Key generated, address: \(address)")

                await MainActor.run {
                    print("💾 MnemonicViewModel: Storing wallet address to storage")
                    // Store the wallet address
                    storageManager.saveWalletAddress(address)
                    print("✅ MnemonicViewModel: Wallet import complete, showing success dialog")
                    
                    updateState {
                        $0.isLoading = true
                        $0.isSuccess = true
                    }
                }
            } catch EthSigner.EthSignerError.invalidMnemonic {
                print("❌ MnemonicViewModel: Invalid mnemonic phrase")
                await MainActor.run {
                    updateState {
                        $0.error = "Invalid mnemonic phrase. Please check your recovery words."
                        $0.isLoading = false
                    }
                }
            } catch {
                print("❌ MnemonicViewModel: Import failed - \(error.localizedDescription)")
                await MainActor.run {
                    updateState {
                        $0.error = error.localizedDescription
                        $0.isLoading = false
                    }
                }
            }
        }
    }

    private func fetchUser() {
        print("🔄 MnemonicViewModel: Starting user fetch")

        Task {
            do {
                // Fetch user profile from API and store
                try await userRepository.fetchAndStoreUser()

                print("✅ MnemonicViewModel: User fetched and stored successfully")
            } catch UserError.noUserProfile {
                print("⚠️ MnemonicViewModel: No user profile found")
                await MainActor.run {
                    updateState {
                        $0.isLoading = false
                        $0.error = "No user profile found for this wallet. Please create your profile first."
                    }
                }
            } catch {
                print("❌ MnemonicViewModel: Failed to fetch user - \(error.localizedDescription)")
                await MainActor.run {
                    updateState {
                        $0.isLoading = false
                        $0.error = "Failed to fetch user: \(error.localizedDescription)"
                    }
                }
            }
        }
    }
}
