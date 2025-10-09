package org.idos.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.idos.app.data.model.UserModel
import org.idos.app.security.KeyManager
import org.idos.kwil.types.HexString
import org.kethereum.model.Address
import timber.log.Timber

sealed class UserState

object LoadingUser : UserState()

object NoUser : UserState()

data class ConnectedWallet(
    val address: Address,
) : UserState()

data class ConnectedUser(
    val user: UserModel,
) : UserState()

data class UserError(
    val message: String,
) : UserState()

/**
 * StorageManager handles user data and app state using SharedPreferences with StateFlow.
 * This provides reactive state management for user authentication state.
 */
class StorageManager(
    private val context: Context,
    private val json: Json,
) {
    // SharedPreferences for user data
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // User state with Loading/Value/Error pattern
    private val _userState = MutableStateFlow<UserState>(LoadingUser)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    init {
        loadUserState()
    }

    /**
     * Load user state from SharedPreferences on startup
     */
    private fun loadUserState() {
        try {
            val userJson = prefs.getString(KEY_USER_PROFILE, null)
            if (userJson != null) {
                val userModel = json.decodeFromString<UserModel>(userJson)
                _userState.value = ConnectedUser(userModel)
                Timber.d("Loaded user profile: ${userModel.id}")
            } else {
                _userState.value = NoUser
                Timber.d("No user profile found in storage")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load user profile")
            _userState.value = UserError("Failed to load user profile: ${e.message}")
        }
    }

    /**
     * Save user profile and update state
     */
    fun saveUserProfile(userModel: UserModel) {
        try {
            val userJson = json.encodeToString(userModel)
            prefs.edit {
                putString(KEY_USER_PROFILE, userJson)
            }
            _userState.value = ConnectedUser(userModel)
            Timber.d("Saved user profile: ${userModel.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save user profile")
            _userState.value = UserError("Failed to save user profile: ${e.message}")
        }
    }

    fun saveWalletAddress(address: Address) {
        _userState.value = ConnectedWallet(address)
    }

    /**
     * Clear user profile and update state
     */
    fun clearUserProfile() {
        try {
            prefs.edit {
                remove(KEY_USER_PROFILE)
            }
            _userState.value = NoUser
            Timber.d("Cleared user profile")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear user profile")
            _userState.value = UserError("Failed to clear user profile: ${e.message}")
        }
    }

    /**
     * Utility methods
     */
    fun hasUserProfile(): Boolean = _userState.value is ConnectedUser

    fun getStoredUser(): UserModel? =
        when (val state = _userState.value) {
            is ConnectedUser -> state.user
            else -> null
        }

    fun getStoredWallet(): Address? =
        when (val state = _userState.value) {
            is ConnectedUser -> state.user.walletAddress
            is ConnectedWallet -> state.address
            else -> null
        }

    companion object {
        private const val PREFS_NAME = "idos_storage_manager"
        private const val KEY_USER_PROFILE = "user_profile"
    }
}
