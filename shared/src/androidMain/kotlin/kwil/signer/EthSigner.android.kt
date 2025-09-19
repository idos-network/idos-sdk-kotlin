package org.idos.kwil.signer

import org.idos.kwil.rpc.HexString

// TODO: Implement Android-specific Ethereum signer using appropriate crypto libraries
class AndroidEthSigner(override val privateKeyHex: String) : EthSigner() {
    
    override fun getIdentifier(): HexString {
        TODO("Android Ethereum address derivation implementation not yet available")
    }

    override fun sign(msg: ByteArray): ByteArray {
        TODO("Android Ethereum signing implementation not yet available")
    }
}

// Platform-specific implementation
actual fun createEthSigner(privateKeyHex: String): EthSigner = AndroidEthSigner(privateKeyHex)
