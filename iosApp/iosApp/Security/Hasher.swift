import Foundation
import WalletCore
import idos_sdk

/// iOS implementation of Keccak256Hasher using WalletCore
/// Used by EnclaveOrchestrator for MPC operations
class Hasher: Keccak256Hasher {
    func digest(data: KotlinByteArray) -> KotlinByteArray {
        // Convert KotlinByteArray to Data
        let inputData = data.toNSData()

        // Use WalletCore's Keccak256 implementation
        let hash = Hash.keccak256(data: inputData)

        // Convert back to KotlinByteArray
        return hash.toKotlinByteArray()
    }
}
