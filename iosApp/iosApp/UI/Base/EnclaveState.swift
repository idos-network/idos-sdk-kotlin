import Foundation
import idos_sdk

/// Key expiration duration options
/// Maps to EnclaveSessionConfig with TIMED expiration type
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

    /// Convert to EnclaveSessionConfig with TIMED expiration
    func toSessionConfig() -> EnclaveSessionConfig {
        return EnclaveSessionConfig(
            expirationType: .timed,
            expirationMillis: KotlinLong(value: self.rawValue)
        )
    }
}
