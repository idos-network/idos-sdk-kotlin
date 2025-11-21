import kotlinx.browser.window
import ui.ProfileView

/**
 * Main entry point for the idOS web application.
 */
fun main() {
    window.addEventListener("DOMContentLoaded", {
        setupApplication()
    })
}

/**
 * Initialize the application and wire up all components.
 */
private fun setupApplication() {
    console.log("idOS Web App initializing...")

    // Configure fetch for Kwil API credentials
    setupFetchCredentials()

    // Initialize wallet connection (Reown AppKit)
    WalletManager.initializeReownAppKit()
    WalletManager.setupConnectButton()
    WalletManager.setupDisconnectButton()

    // Initialize enclave unlock dialog
    EnclaveManager.setup()

    // Setup credential click handler
    ProfileView.setCredentialClickListener { credentialId ->
        console.log("Opening enclave dialog for credential:", credentialId)
        AppState.selectedCredentialId = credentialId
        CredentialManager.checkEnclaveAndDecrypt(credentialId)
    }

    // Check for existing wallet session
    WalletManager.checkPreviousConnection()
}

/**
 * Configure global fetch to include credentials for Kwil API requests.
 *
 * Required for KWIL Gateway authentication cookies to work in browsers.
 * Only adds credentials to the configured idOS base URL.
 */
private fun setupFetchCredentials() {
    val baseUrl = Config.IDOS_BASE_URL

    js("""
        (function(baseUrl) {
            if (!window.__idosFetchInstalled) {
                window.__originalFetch = window.fetch;
                window.fetch = function(resource, init) {
                    var url = typeof resource === 'string' ? resource : resource.url;

                    if (url.startsWith(baseUrl)) {
                        var newInit = Object.assign({}, init || {}, { credentials: 'include' });
                        return window.__originalFetch(resource, newInit);
                    }

                    return window.__originalFetch(resource, init);
                };
                window.__idosFetchInstalled = true;
                console.log('[idOS] Fetch credentials configured for:', baseUrl);
            }
        })
    """)(baseUrl)
}
