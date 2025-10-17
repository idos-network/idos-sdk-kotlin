package org.idos.app.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.idos.app.data.DataProvider
import org.idos.app.data.StorageManager
import org.idos.app.data.UserState
import org.idos.app.data.model.UserModel
import org.idos.app.security.EthSigner.Companion.privateToAddress
import org.idos.app.security.KeyManager
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.EnclaveOrchestrator
import timber.log.Timber

class NoProfileException(
    message: String,
) : Exception(message)

interface UserRepository {
    val userState: StateFlow<UserState>

    suspend fun initialize()

    suspend fun fetchAndStoreUser()

    fun getStoredUser(): UserModel?

    suspend fun clearUserProfile()

    fun hasStoredProfile(): Boolean
}

class UserRepositoryImpl(
    private val dataProvider: DataProvider,
    private val storageManager: StorageManager,
    private val keyManager: KeyManager,
    private val enclaveOrchestrator: EnclaveOrchestrator,
) : UserRepository {
    override val userState: StateFlow<UserState> = storageManager.userState

    /**
     * Initialize the user repository by loading stored user data
     */
    override suspend fun initialize() {
        storageManager.initialize()
        getStoredUser()?.enclaveKeyType?.let {
            enclaveOrchestrator.initializeType(it)
        }
    }

    /**
     * Orchestrates the proper flow:
     * 1. Get address from KeyManager (assumes key was already generated)
     * 2. Check if user has profile
     * 3. Fetch user data
     * 4. Combine address + user data
     * 5. Save to StorageManager
     */
    override suspend fun fetchAndStoreUser() {
        // Step 1: Save address to StorageManager
        val address =
            keyManager.getStoredKey()?.privateToAddress()
                ?: throw IllegalStateException("No key found, import wallet first")
        storageManager.saveWalletAddress(address)

        // Step 2: Check if user has profile first
        val hasProfile = dataProvider.hasUserProfile(address.hex)
        if (!hasProfile) {
            throw NoProfileException("User profile does not exist - please create profile first")
        }

        try {
            // Step 3: Fetch user data
            val user = dataProvider.getUser()

            // Step 4: Combine address + user data
            val userModel =
                UserModel(
                    id = user.id,
                    walletAddress = address,
                    enclaveKeyType = EnclaveKeyType.getByValue(user.encryptionPasswordStore),
                    lastUpdated = System.currentTimeMillis(),
                )

            // Step 5: Save to StorageManager (triggers StateFlow update)
            storageManager.saveUserProfile(userModel)
            userModel.enclaveKeyType?.let { enclaveOrchestrator.initializeType(it) } ?: {
                Timber.w("No enclave key type found in user profile, skipping Enclave initialization.")
            }
            Timber.d("Successfully fetched and stored user profile")
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch user profile")
            clearUserProfile()
            throw e
        }
    }

    override fun getStoredUser(): UserModel? = storageManager.getStoredUser()

    override suspend fun clearUserProfile() {
        storageManager.clearUserProfile()
        keyManager.clearStoredKeys() // Also clear the keys when logging out
        Timber.d("Cleared user profile and keys")
    }

    override fun hasStoredProfile(): Boolean = storageManager.hasUserProfile()
}
