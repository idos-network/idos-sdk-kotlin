package org.idos.app.security

import org.idos.app.data.StorageManager
import org.idos.app.security.external.RemoteSigner
import org.idos.app.security.external.ReownWalletManager
import org.idos.app.security.local.KeyManager
import org.idos.app.security.local.LocalSigner
import org.idos.crypto.Keccak256Hasher
import org.idos.crypto.eip712.TypedData
import org.idos.kwil.types.HexString
import org.idos.signer.EthSigner
import org.kethereum.model.Address

class UnifiedSigner(
    keccak256: Keccak256Hasher,
    private val storageManager: StorageManager,
    private val keyManager: KeyManager,
    private val reownWalletManager: ReownWalletManager,
) : EthSigner(keccak256) {

    private var localSigner: LocalSigner? = null
    private var remoteSigner: RemoteSigner? = null

    override fun getIdentifier(): HexString {
        val address = storageManager.getStoredWallet()?.cleanHex
        requireNotNull(address) { "No wallet address stored" }
        return address
    }

    override suspend fun sign(msg: ByteArray): ByteArray {
        return localSigner?.sign(msg)
            ?: remoteSigner?.sign(msg)
            ?: throw IllegalStateException("No active signer")
    }

    override suspend fun signTypedData(typedData: TypedData): String {
        return localSigner?.signTypedData(typedData, keccak256)
            ?: remoteSigner?.signTypedData(typedData)
            ?: throw IllegalStateException("No active signer")
    }

    suspend fun getActiveAddress(): Address {
        return localSigner?.getActiveAddress()
            ?: remoteSigner?.getActiveAddress()
            ?: throw IllegalStateException("No active signer")
    }

    fun activateLocalSigner() {
        localSigner = LocalSigner(keyManager)
        remoteSigner = null
    }

    fun activateRemoteSigner() {
        remoteSigner = RemoteSigner(reownWalletManager)
        localSigner = null
    }

    suspend fun disconnect() {
        localSigner?.disconnect()
        remoteSigner?.disconnect()
        storageManager.clearUserProfile()
        localSigner = null
        remoteSigner = null
    }
}
