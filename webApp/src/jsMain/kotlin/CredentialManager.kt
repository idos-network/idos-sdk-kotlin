import kotlinx.coroutines.launch
import org.idos.enclave.EnclaveError
import org.idos.getOwned
import org.idos.kwil.serialization.toByteArray
import org.idos.kwil.types.Base64String

/**
 * Handles credential decryption operations.
 */
object CredentialManager {

    fun decryptCredential(credentialId: String) {
        AppState.scope.launch {
            try {
                val orchestrator = AppState.orchestrator
                if (orchestrator == null) {
                    console.error("Enclave orchestrator not initialized")
                    return@launch
                }

                val client = AppState.client
                if (client == null) {
                    console.error("IdOS client not initialized")
                    return@launch
                }

                console.log("Decrypting credential:", credentialId)

                // Get credential details with content
                val credentialResponse = client.credentials.getOwned(credentialId)

                // Decrypt using enclave
                orchestrator.withEnclave { enclave ->
                    val content = Base64String(credentialResponse.content).toByteArray()
                    val pubkey = Base64String(credentialResponse.encryptorPublicKey).toByteArray()
                    val decryptedBytes = enclave.decrypt(content, pubkey)
                    val decryptedString = decryptedBytes.decodeToString()

                    console.log("Decrypted credential:", decryptedString)

                    // TODO: Display decrypted content in UI
                    js("alert('Decrypted content:\\n' + decryptedString)")
                }
            } catch (e: Exception) {
                console.error("Failed to decrypt credential:", e)
                when (e) {
                    is EnclaveError.NoKey,
                    is EnclaveError.KeyExpired -> {
                        // Unlock based on enclave type
                        EnclaveManager.unlock()
                    }
                    else -> {
                        js("alert('Failed to decrypt: ' + (e.message || 'Unknown error'))")
                    }
                }
            }
        }
    }

    fun checkEnclaveAndDecrypt(credentialId: String) {
        AppState.scope.launch {
            try {
                val orchestrator = AppState.orchestrator
                if (orchestrator == null) {
                    console.error("Enclave orchestrator not initialized")
                    return@launch
                }

                val state = orchestrator.state.value
                console.log("Enclave state:", state)

                when (state) {
                    is org.idos.enclave.EnclaveState.Unlocked -> {
                        decryptCredential(credentialId)
                    }
                    else -> {
                        // Unlock based on enclave type (password dialog or MPC)
                        EnclaveManager.unlock()
                    }
                }
            } catch (e: Exception) {
                console.error("Failed to check enclave state:", e)
                EnclaveManager.unlock()
            }
        }
    }
}
