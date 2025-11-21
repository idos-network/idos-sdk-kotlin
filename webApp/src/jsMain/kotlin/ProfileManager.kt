import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.idos.IdosClient
import org.idos.crypto.JsKeccak256
import org.idos.enclave.BrowserMetadataStorage
import org.idos.enclave.BrowserSecureStorage
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.EnclaveOrchestrator
import org.idos.enclave.crypto.JsEncryption
import org.idos.enclave.mpc.MpcConfig
import org.idos.get
import org.idos.hasProfile
import org.idos.signer.Signer

/**
 * User profile state.
 */
sealed class ProfileState {
    object Loading : ProfileState()
    object NoProfile : ProfileState()
    data class Connected(val user: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

/**
 * User profile data.
 */
data class UserProfile(
    val id: String,
    val walletAddress: String,
    val enclaveKeyType: EnclaveKeyType?,
)

/**
 * Manages user profile initialization and state.
 *
 * Flow:
 * 1. Wallet connects with address
 * 2. Check if user has profile (hasProfile)
 * 3. Get user data (getUser)
 * 4. Store user profile with enclave type
 * 5. Initialize enclave orchestrator with correct type (LOCAL or MPC)
 */
object ProfileManager {
    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private var _userProfile: UserProfile? = null
    val userProfile: UserProfile? get() = _userProfile

    /**
     * Initialize profile for connected wallet address.
     *
     * @param address The connected wallet address
     * @param client The idOS client
     * @param signer The wallet signer (needed for MPC operations)
     * @return The initialized EnclaveOrchestrator, or null if no profile
     */
    suspend fun initializeProfile(
        address: String,
        client: IdosClient,
        signer: Signer
    ): EnclaveOrchestrator? {
        _state.value = ProfileState.Loading

        return try {
            // Check if user has a profile
            val hasProfile = client.users.hasProfile(address)
            console.log("User has profile:", hasProfile)

            if (!hasProfile) {
                _state.value = ProfileState.NoProfile
                console.warn("No profile found for address:", address)
                return null
            }

            // Get user data
            val user = client.users.get()
            console.log("User data:", user)

            // Determine enclave key type from encryption_password_store
            val enclaveKeyType = EnclaveKeyType.getByValue(user.encryptionPasswordStore)
            console.log("Enclave key type:", enclaveKeyType?.value ?: "unknown")

            // Create user profile
            val profile = UserProfile(
                id = user.id,
                walletAddress = address,
                enclaveKeyType = enclaveKeyType
            )
            _userProfile = profile

            // Create and configure enclave orchestrator based on type
            val orchestrator = createOrchestrator(enclaveKeyType, signer)

            _state.value = ProfileState.Connected(profile)
            console.log("Profile initialized successfully with enclave type:", enclaveKeyType?.value)

            orchestrator
        } catch (e: Exception) {
            console.error("Failed to initialize profile:", e)
            _state.value = ProfileState.Error(e.message ?: "Failed to initialize profile")
            null
        }
    }

    /**
     * Create enclave orchestrator based on key type.
     */
    private fun createOrchestrator(keyType: EnclaveKeyType?, signer: Signer): EnclaveOrchestrator {
        val storage = BrowserMetadataStorage()
        val secureStorage = BrowserSecureStorage()
        val encryption = JsEncryption(secureStorage)
        val keccak256 = JsKeccak256()

        return when (keyType) {
            EnclaveKeyType.USER -> {
                console.log("Creating LOCAL (password-based) enclave orchestrator")
                EnclaveOrchestrator.createLocal(
                    encryption = encryption,
                    storage = storage
                )
            }
            EnclaveKeyType.MPC -> {
                console.log("Creating MPC enclave orchestrator")
                val mpcConfig = MpcConfig(
                    partisiaRpcUrl = Config.MPC_PARTISIA_URL,
                    contractAddress = Config.MPC_CONTRACT_ADDRESS,
                    totalNodes = Config.MPC_TOTAL_NODES,
                    threshold = Config.MPC_THRESHOLD,
                    maliciousNodes = Config.MPC_MALICIOUS_NODES
                )
                EnclaveOrchestrator.createMpc(
                    encryption = encryption,
                    storage = storage,
                    mpcConfig = mpcConfig,
                    signer = signer,
                    hasher = keccak256
                )
            }
            null -> {
                console.warn("Unknown enclave type, defaulting to LOCAL")
                EnclaveOrchestrator.createLocal(
                    encryption = encryption,
                    storage = storage
                )
            }
        }
    }

    /**
     * Clear the current profile.
     */
    fun clear() {
        _userProfile = null
        _state.value = ProfileState.Loading
    }
}
