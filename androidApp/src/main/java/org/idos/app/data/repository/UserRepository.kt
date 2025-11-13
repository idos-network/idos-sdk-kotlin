package org.idos.app.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.idos.app.data.DataProvider
import org.idos.app.data.StorageManager
import org.idos.app.data.UserState
import org.idos.app.data.model.UserModel
import org.idos.app.data.model.WalletType
import org.idos.app.security.UnifiedSigner
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.EnclaveOrchestrator
import org.kethereum.model.Address
import timber.log.Timber

class NoProfileException(
    message: String,
    val address: String,
) : Exception(message)

interface UserRepository {
    val userState: StateFlow<UserState>

    suspend fun initialize()

    suspend fun fetchAndStoreUser(walletType: WalletType)

    fun getStoredUser(): UserModel?

    suspend fun clearUserProfile()

    fun hasStoredProfile(): Boolean
}

class UserRepositoryImpl(
    private val dataProvider: DataProvider,
    private val storageManager: StorageManager,
    private val unifiedSigner: UnifiedSigner,
    private val enclaveOrchestrator: EnclaveOrchestrator,
) : UserRepository {
    override val userState: StateFlow<UserState> = storageManager.userState

    /**
     * Initialize the user repository by loading stored user data
     */
    override suspend fun initialize() {
        storageManager.initialize { user ->
            val shouldClearProfile = try {
                // Restore signer based on wallet type
                when (user.walletType) {
                    WalletType.LOCAL -> unifiedSigner.activateLocalSigner()
                    WalletType.REMOTE -> unifiedSigner.activateRemoteSigner()
                }

                // Validate that the actual address matches stored address
                val actualAddress = unifiedSigner.getActiveAddress()
                actualAddress != user.walletAddress
            } catch (e: Exception) {
                Timber.w(e, "Failed to restore signer or retrieve address. Keys may be missing.")
                true
            }

            if (shouldClearProfile) {
                Timber.w("Clearing profile due to missing or mismatched keys")
                clearUserProfile()
                return@initialize false // Don't emit ConnectedUser state
            }

            // Initialize enclave if needed
            user.enclaveKeyType?.let {
                enclaveOrchestrator.initializeType(it)
            }

            Timber.d("Restored ${user.walletType} signer for user ${user.id} & ${user.walletAddress.hex}")
            true // Emit ConnectedUser state
        }
    }

    override suspend fun fetchAndStoreUser(walletType: WalletType) {
        try {
            val address = unifiedSigner.getActiveAddress()
            storageManager.saveWalletAddress(address)

            val hasProfile = dataProvider.hasUserProfile(address.hex)
            if (!hasProfile) {
                throw NoProfileException(
                    "User profile does not exist - please create profile first",
                    address.hex
                )
            }

            val user = dataProvider.getUser()
            val userModel =
                UserModel(
                    id = user.id,
                    walletAddress = address,
                    enclaveKeyType = EnclaveKeyType.getByValue(user.encryptionPasswordStore),
                    walletType = walletType,
                    lastUpdated = System.currentTimeMillis(),
                )

            storageManager.saveUserProfile(userModel)
            userModel.enclaveKeyType?.let { enclaveOrchestrator.initializeType(it) }
                ?: Timber.w("No enclave key type found in user profile")

            Timber.d("Successfully fetched and stored user profile")
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch user profile")
            clearUserProfile()
            throw e
        }
    }

    override fun getStoredUser(): UserModel? = storageManager.getStoredUser()

    override suspend fun clearUserProfile() {
        unifiedSigner.disconnect()
        enclaveOrchestrator.lock()
        Timber.d("Cleared user profile and keys")
    }

    override fun hasStoredProfile(): Boolean = storageManager.hasUserProfile()
}
