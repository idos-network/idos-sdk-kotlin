import Foundation
import Combine
import OSLog

/// Mnemonic state matching Android's MnemonicState
struct MnemonicState {
    var mnemonic: String = ""
    var derivationPath: String = LocalSigner.defaultDerivationPath
    var isLoading: Bool = false
    var isSuccess: Bool = false
    var error: String? = nil
}

/// Mnemonic events matching Android's MnemonicEvent
enum MnemonicEvent {
    case updateMnemonic(String)
    case updateDerivationPath(String)
    case importWallet
    case fetchUser
    case clearError
}

/// MnemonicViewModel matching Android's MnemonicViewModel
class MnemonicViewModel: BaseViewModel<MnemonicState, MnemonicEvent> {
    private let keyManager: KeyManager
    private let userRepository: UserRepositoryProtocol
    private let navigationCoordinator: NavigationCoordinator
    private let unifiedSigner: UnifiedSigner

    init(
        keyManager: KeyManager,
        userRepository: UserRepositoryProtocol,
        navigationCoordinator: NavigationCoordinator,
        unifiedSigner: UnifiedSigner
    ) {
        self.keyManager = keyManager
        self.userRepository = userRepository
        self.navigationCoordinator = navigationCoordinator
        self.unifiedSigner = unifiedSigner

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
        case .updateDerivationPath(let path):
            updateDerivationPath(path)
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

    private func updateDerivationPath(_ path: String) {
        updateState { $0.derivationPath = path }
    }

    private func importWallet() {
        Logger.viewModel.debug("MnemonicViewModel: Starting wallet import")
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
                    Logger.viewModel.error("MnemonicViewModel: Invalid mnemonic word count: \(words.count)")
                    await MainActor.run {
                        updateState {
                            $0.error = "Please enter a valid 12 or 24 word recovery phrase"
                            $0.isLoading = false
                        }
                    }
                    return
                }

                Logger.viewModel.info("MnemonicViewModel: Mnemonic validated (\(words.count) words)")
                Logger.viewModel.debug("MnemonicViewModel: Generating and storing private key with derivation path: \(self.state.derivationPath)")

                // Generate and store key using KeyManager (matches Android implementation)
                // This derives the private key using BIP39/BIP44 and stores it securely
                let address = try keyManager.generateAndStoreKey(words: state.mnemonic, derivationPath: state.derivationPath)

                Logger.viewModel.info("MnemonicViewModel: Key generated, address: \(address, privacy: .private)")

                // Activate local signer (like Android)
                unifiedSigner.activateLocalSigner()
                Logger.viewModel.info("MnemonicViewModel: Local signer activated")

                await MainActor.run {
                    Logger.viewModel.info("MnemonicViewModel: Wallet import complete, showing success dialog")

                    updateState {
                        $0.isLoading = false
                        $0.isSuccess = true
                    }
                }
            } catch LocalSigner.LocalSignerError.invalidMnemonic {
                Logger.viewModel.error("MnemonicViewModel: Invalid mnemonic phrase")
                await MainActor.run {
                    updateState {
                        $0.error = "Invalid mnemonic phrase. Please check your recovery words."
                        $0.isLoading = false
                    }
                }
            } catch {
                Logger.viewModel.error("MnemonicViewModel: Import failed - \(error.localizedDescription)")
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
        Logger.viewModel.debug("MnemonicViewModel: Starting user fetch")

        // Set loading state while keeping success dialog visible
        updateState {
            $0.isLoading = true
            $0.error = nil
        }

        Task {
            do {
                // Fetch user profile from API and store (using local wallet type)
                try await userRepository.fetchAndStoreUser(walletType: .local)

                Logger.viewModel.info("MnemonicViewModel: User fetched and stored successfully")
                // Success - dialog will auto-dismiss when UserRepository navigates
                // No need to update state here, navigation will happen automatically
            } catch UserError.noUserProfile {
                Logger.viewModel.notice("MnemonicViewModel: No user profile found")
                await MainActor.run {
                    updateState {
                        $0.isLoading = false
                        $0.isSuccess = false
                        $0.error = "No user profile found for this wallet. Please create your profile first."
                    }
                }
            } catch {
                Logger.viewModel.error("MnemonicViewModel: Failed to fetch user - \(error.localizedDescription)")
                await MainActor.run {
                    updateState {
                        $0.isLoading = false
                        $0.isSuccess = false
                        $0.error = "Failed to fetch user: \(error.localizedDescription)"
                    }
                }
            }
        }
    }
}
