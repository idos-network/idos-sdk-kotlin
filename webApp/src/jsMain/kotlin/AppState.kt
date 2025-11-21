import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.idos.IdosClient
import org.idos.enclave.EnclaveOrchestrator
import org.idos.signer.Signer
import viewmodel.ProfileViewModel

/**
 * Global application state container.
 */
object AppState {
    val scope: CoroutineScope = MainScope()

    var client: IdosClient? = null
    var viewModel: ProfileViewModel? = null
    var orchestrator: EnclaveOrchestrator? = null
    var signer: Signer? = null
    var appKit: dynamic = null

    var isConnecting: Boolean = false
    var connectedAddress: String? = null
    var selectedCredentialId: String? = null

    fun reset() {
        client = null
        viewModel = null
        orchestrator = null
        signer = null
        connectedAddress = null
        isConnecting = false
        selectedCredentialId = null
    }
}
