package viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.idos.IdosClient
import org.idos.get
import org.idos.getAll
import org.idos.kwil.domain.DomainError
import org.idos.kwil.domain.generated.view.GetCredentialsResponse
import org.idos.kwil.domain.generated.view.GetWalletsResponse

/**
 * ViewModel for the idOS Profile page.
 *
 * This class contains business logic that will be portable when migrating to Compose Multiplatform.
 * It manages state and data fetching independently of the UI rendering layer.
 */
class ProfileViewModel(private val client: IdosClient) {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    /**
     * Load the user's profile data including credentials and wallets.
     * Filters out credentials without public notes, matching Android behavior.
     */
    suspend fun loadProfile() {
        _profileState.value = ProfileState.Loading

        try {
            val user = client.users.get()
            val allCredentials = client.credentials.getAll()
            val wallets = client.wallets.getAll()

            // Filter credentials with public notes only (matching Android behavior)
            val credentialsWithNotes = allCredentials.filter { it.publicNotes.isNotBlank() }

            _profileState.value = ProfileState.Success(
                userId = user.id,
                credentials = credentialsWithNotes,
                wallets = wallets
            )
        } catch (e: DomainError) {
            _profileState.value = ProfileState.Error(e.message ?: "Unknown error occurred")
        } catch (e: Exception) {
            _profileState.value = ProfileState.Error(e.message ?: "Failed to load profile")
        }
    }

    /**
     * Refresh the profile data.
     */
    suspend fun refresh() {
        loadProfile()
    }
}

/**
 * Sealed class representing the different states of the profile.
 */
sealed class ProfileState {
    object Loading : ProfileState()

    data class Success(
        val userId: String,
        val credentials: List<GetCredentialsResponse>,
        val wallets: List<GetWalletsResponse>
    ) : ProfileState()

    data class Error(val message: String) : ProfileState()
}
