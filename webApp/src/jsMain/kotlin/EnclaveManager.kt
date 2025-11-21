import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.EnclaveSessionConfig
import org.idos.enclave.EnclaveState
import org.idos.enclave.ExpirationType
import org.idos.get
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement

/**
 * Handles enclave unlock operations.
 *
 * Supports two modes based on user's enclave type:
 * - USER (local): Password-based unlock - shows dialog
 * - MPC: Passwordless unlock - downloads secret from network
 */
object EnclaveManager {

    fun setup() {
        setupCloseButton()
        setupPasswordToggle()
        setupFormSubmission()
        setupOverlayClick()
    }

    private fun setupCloseButton() {
        val closeBtn = document.getElementById("close-enclave-dialog") as? HTMLButtonElement
        closeBtn?.addEventListener("click", { hideDialog() })
    }

    private fun setupPasswordToggle() {
        val toggleBtn = document.getElementById("toggle-password") as? HTMLButtonElement
        val passwordInput = document.getElementById("enclave-password") as? HTMLInputElement
        toggleBtn?.addEventListener("click", {
            if (passwordInput != null) {
                passwordInput.type = if (passwordInput.type == "password") "text" else "password"
            }
        })
    }

    private fun setupFormSubmission() {
        val form = document.getElementById("enclave-unlock-form") as? HTMLFormElement
        form?.addEventListener("submit", { event ->
            event.preventDefault()
            handleUnlockSubmit()
        })
    }

    private fun setupOverlayClick() {
        val overlay = document.querySelector(".modal-overlay")
        overlay?.addEventListener("click", { hideDialog() })
    }

    /**
     * Unlock enclave - always shows dialog with expiration options.
     * Password field is only shown for USER type.
     */
    fun unlock() {
        val profile = ProfileManager.userProfile
        if (profile == null) {
            console.error("No user profile available")
            return
        }

        console.log("Showing unlock dialog for enclave type:", profile.enclaveKeyType?.value)
        showDialog()
    }

    fun showDialog() {
        val dialog = document.getElementById("enclave-dialog") as? HTMLElement
        dialog?.classList?.remove("hidden")

        // Update dialog based on enclave type
        val profile = ProfileManager.userProfile
        val passwordSection = document.getElementById("password-section") as? HTMLElement

        console.log("showDialog - enclave type:", profile?.enclaveKeyType?.value)
        console.log("showDialog - passwordSection element:", passwordSection)

        when (profile?.enclaveKeyType) {
            EnclaveKeyType.MPC -> {
                // Hide password section for MPC
                passwordSection?.classList?.add("hidden")
                updateDialogText(
                    title = "Unlock idOS",
                    subtitle = "Click unlock to retrieve your encryption key from the network."
                )
            }
            else -> {
                // Show password section for USER/unknown
                passwordSection?.classList?.remove("hidden")
                updateDialogText(
                    title = "Unlock idOS",
                    subtitle = "Please enter your idOS password below:"
                )
            }
        }

        showForm()
    }

    private fun updateDialogText(title: String, subtitle: String) {
        val titleEl = document.getElementById("enclave-title") as? HTMLElement
        val subtitleEl = document.getElementById("enclave-subtitle") as? HTMLElement
        titleEl?.textContent = title
        subtitleEl?.textContent = subtitle
    }

    fun hideDialog() {
        val dialog = document.getElementById("enclave-dialog") as? HTMLElement
        dialog?.classList?.add("hidden")
        hideError()
    }

    private fun showForm() {
        val form = document.getElementById("enclave-form") as? HTMLElement
        val loading = document.getElementById("enclave-loading") as? HTMLElement
        form?.classList?.remove("hidden")
        loading?.classList?.add("hidden")
    }

    private fun showLoading() {
        val form = document.getElementById("enclave-form") as? HTMLElement
        val loading = document.getElementById("enclave-loading") as? HTMLElement
        form?.classList?.add("hidden")
        loading?.classList?.remove("hidden")
    }

    private fun showError(message: String) {
        val errorDiv = document.getElementById("enclave-error") as? HTMLElement
        errorDiv?.textContent = message
        errorDiv?.classList?.remove("hidden")
    }

    private fun hideError() {
        val errorDiv = document.getElementById("enclave-error") as? HTMLElement
        errorDiv?.classList?.add("hidden")
    }

    /**
     * Handle unlock form submission.
     * For USER type: uses password from form
     * For MPC type: no password needed
     */
    private fun handleUnlockSubmit() {
        val profile = ProfileManager.userProfile
        if (profile == null) {
            showError("User profile not available")
            return
        }

        // Get expiration from form (common for both types)
        val expirationRadio = document.querySelector("input[name='expiration']:checked") as? HTMLInputElement
        val expirationMillis = expirationRadio?.value?.toLongOrNull() ?: 3600000L

        // Get password only for USER type
        val password: String? = if (profile.enclaveKeyType == EnclaveKeyType.USER) {
            val passwordInput = document.getElementById("enclave-password") as? HTMLInputElement
            passwordInput?.value
        } else {
            null
        }

        console.log("Unlocking enclave type:", profile.enclaveKeyType?.value, "expiration:", expirationMillis)

        showLoading()
        hideError()

        AppState.scope.launch {
            try {
                val orchestrator = AppState.orchestrator
                    ?: throw Exception("Enclave orchestrator not initialized")

                val sessionConfig = EnclaveSessionConfig(
                    expirationType = ExpirationType.TIMED,
                    expirationMillis = expirationMillis
                )

                console.log("Calling orchestrator.unlock() with userId:", profile.id)
                orchestrator.unlock(profile.id, sessionConfig, password)
                console.log("orchestrator.unlock() completed")

                val state = orchestrator.state.value
                console.log("Enclave state after unlock:", state)

                when (state) {
                    is EnclaveState.Unlocked -> {
                        console.log("Enclave unlocked successfully")
                        hideDialog()

                        AppState.selectedCredentialId?.let { credentialId ->
                            CredentialManager.decryptCredential(credentialId)
                        }
                    }
                    is EnclaveState.Locked -> {
                        val errorMsg = if (profile.enclaveKeyType == EnclaveKeyType.USER) {
                            "Unlock failed - wrong password or key generation error."
                        } else {
                            "MPC unlock failed - could not retrieve secret from network."
                        }
                        throw Exception(errorMsg)
                    }
                    else -> {
                        throw Exception("Unexpected enclave state after unlock: $state")
                    }
                }
            } catch (e: Exception) {
                console.error("Failed to unlock enclave:", e)
                showForm()
                showError(e.message ?: "Failed to unlock enclave")
            }
        }
    }
}
