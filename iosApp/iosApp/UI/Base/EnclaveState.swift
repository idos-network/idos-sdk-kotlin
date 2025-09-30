import Foundation
import idos_sdk

/// Enclave UI state matching Android's EnclaveUiState.kt
enum EnclaveUiState: Equatable {
    case loading
    case close
    case available(enclave: Enclave)
    case requiresKey
    case generating
    case keyGenerationError(message: String)
    case error(message: String, canRetry: Bool = true)

    // Custom Equatable implementation since Enclave doesn't conform to Equatable
    static func == (lhs: EnclaveUiState, rhs: EnclaveUiState) -> Bool {
        switch (lhs, rhs) {
        case (.loading, .loading),
             (.close, .close),
             (.requiresKey, .requiresKey),
             (.generating, .generating):
            return true
        case let (.available(lEnclave), .available(rEnclave)):
            // Compare by reference since Enclave is a class
            return lEnclave === rEnclave
        case let (.keyGenerationError(lMsg), .keyGenerationError(rMsg)):
            return lMsg == rMsg
        case let (.error(lMsg, lRetry), .error(rMsg, rRetry)):
            return lMsg == rMsg && lRetry == rRetry
        default:
            return false
        }
    }
}

/// Key expiration duration options
enum KeyExpiration: Int64, CaseIterable {
    case oneDay = 86400000        // 1 day in milliseconds
    case oneWeek = 604800000      // 7 days in milliseconds
    case oneMonth = 2592000000    // 30 days in milliseconds

    var displayName: String {
        switch self {
        case .oneDay: return "1 Day"
        case .oneWeek: return "1 Week"
        case .oneMonth: return "1 Month"
        }
    }
}