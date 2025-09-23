package org.idos.kwil.signer

import org.idos.kwil.rpc.HexString

// TODO: Implement iOS-specific Ethereum signer using appropriate crypto libraries
class IosEthSigner(override val privateKeyHex: String) : EthSigner() {
    
    override fun getIdentifier(): HexString {
        TODO("iOS Ethereum address derivation implementation not yet available")
    }

    override fun sign(msg: ByteArray): ByteArray {
        TODO("iOS Ethereum signing implementation not yet available")
    }
}

// Platform-specific implementation
actual fun createEthSigner(privateKeyHex: String): EthSigner = IosEthSigner(privateKeyHex)
