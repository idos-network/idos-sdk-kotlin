import Foundation
import idos_sdk

/// Converts a Swift Error to a Kotlin Throwable of the specified type
/// - Parameter error: The error to convert
/// - Returns: The converted Kotlin Throwable of type T, or nil if conversion fails
func getKtThrowable<T: AnyObject>(error: Error) -> T? {
    guard let nsError = error as NSError?,
          let kotlinException = nsError.userInfo["KotlinException"] as? T else {
        return nil
    }
    return kotlinException
}

/// Extension to easily convert Swift errors to Kotlin throwables
public extension Error {
    /// Converts the error to a Kotlin throwable of the specified type
    /// - Returns: The converted Kotlin throwable, or nil if conversion fails
    func asKotlinThrowable<T: AnyObject>() -> T? {
        return getKtThrowable(error: self)
    }
    
    /// Convenience method to get EnclaveError
    /// - Returns: The EnclaveError if the error can be converted, nil otherwise
    func asEnclaveError() -> EnclaveError? {
        return asKotlinThrowable()
    }

    /// Convenience method to get DomainError
    /// - Returns: The DomainError if the error can be converted, nil otherwise
    func asDomainError() -> DomainError? {
        return asKotlinThrowable()
    }
}

/// Example usage:
/// ```
/// do {
///     try someKotlinCall()
/// } catch {
///     if let enclaveError = error.asEnclaveError() {
///         switch onEnum(of: enclaveError) {
///         case .noKey:
///             // Handle no key
///         case .keyExpired:
///             // Handle expired key
///         // Handle other cases...
///         }
///     } else {
///         // Handle non-EnclaveError errors
///         print("Unexpected error: \(error)")
///     }
/// }
/// ```
