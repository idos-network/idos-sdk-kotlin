package ui

import kotlinx.browser.document
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.idos.kwil.domain.generated.view.GetCredentialsResponse
import org.idos.kwil.domain.generated.view.GetWalletsResponse
import org.idos.kwil.types.UuidString
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTableRowElement
import org.w3c.dom.HTMLTableSectionElement
import org.w3c.dom.asList

/**
 * Public notes structure parsed from credential JSON.
 * Matches Android's PublicNotes model.
 */
@Serializable
internal data class PublicNotes(
    @SerialName("id") val id: UuidString,
    @SerialName("type") val type: String,
    @SerialName("level") val level: String,
    @SerialName("status") val status: String,
    @SerialName("issuer") val issuer: String,
)

/**
 * UI renderer for the Profile page.
 *
 * This class handles DOM manipulation for displaying profile data.
 * When migrating to Compose, this will be replaced with @Composable functions.
 */
object ProfileView {

    private var credentialClickCallback: ((String) -> Unit)? = null

    /**
     * Set the callback for credential clicks.
     */
    fun setCredentialClickListener(callback: (String) -> Unit) {
        credentialClickCallback = callback
    }

    private fun onCredentialClick(credentialId: String) {
        console.log("Credential clicked:", credentialId)
        credentialClickCallback?.invoke(credentialId)
    }

    /**
     * Render the list of credentials in the table.
     * Parses publicNotes JSON to display type and issuer, matching Android behavior.
     */
    fun renderCredentials(credentials: List<GetCredentialsResponse>) {
        val tbody = document.getElementById("credentials-list") as? HTMLTableSectionElement
            ?: return

        if (credentials.isEmpty()) {
            tbody.innerHTML = """
                <tr class="loading-row">
                    <td colspan="5">No credentials found</td>
                </tr>
            """.trimIndent()
            return
        }

        tbody.innerHTML = credentials.joinToString("") { credential ->
            // Parse publicNotes JSON to extract structured data
            val notes = try {
                Json.decodeFromString<PublicNotes>(credential.publicNotes)
            } catch (e: Exception) {
                console.warn("Failed to parse publicNotes for credential ${credential.id}", e)
                null
            }

            val level = notes?.level ?: "unknown"
            val type = notes?.type ?: "Unknown"
            val status = notes?.status ?: "unknown"
            val issuer = notes?.issuer ?: credential.issuerAuthPublicKey.take(10) + "..."

            """
            <tr class="credential-row" data-credential-id="${credential.id}" style="cursor: pointer;">
                <td>$level</td>
                <td>$type</td>
                <td>$status</td>
                <td>$issuer</td>
                <td>0</td>
            </tr>
            """.trimIndent()
        }

        // Add click handlers to credential rows
        val rows = tbody.querySelectorAll(".credential-row").asList()
        for (i in 0 until rows.size) {
            val row = rows[i] as? HTMLTableRowElement ?: continue
            row.onclick = {
                val credentialId = row.getAttribute("data-credential-id")
                credentialId?.let { onCredentialClick(it) }
                null
            }
        }
    }

    /**
     * Render the list of wallets in the table.
     */
    fun renderWallets(wallets: List<GetWalletsResponse>) {
        val tbody = document.getElementById("wallets-list") as? HTMLTableSectionElement
            ?: return

        if (wallets.isEmpty()) {
            tbody.innerHTML = """
                <tr class="loading-row">
                    <td colspan="2">No wallets found</td>
                </tr>
            """.trimIndent()
            return
        }

        tbody.innerHTML = wallets.joinToString("") { wallet ->
            """
            <tr>
                <td style="font-family: monospace;">${wallet.address}</td>
                <td>${wallet.walletType.uppercase()}</td>
            </tr>
            """.trimIndent()
        }
    }

    /**
     * Update the connected address display in the header.
     */
    fun updateConnectedAddress(address: String) {
        val shortAddress = "0x" + address.removePrefix("0x").take(4) + "..." + address.takeLast(4)
        val addressElement = document.getElementById("connected-address")
        addressElement?.textContent = shortAddress
    }

    /**
     * Show loading state.
     */
    fun showLoading() {
        val credentialsTable = document.getElementById("credentials-list") as? HTMLTableSectionElement
        credentialsTable?.innerHTML = """
            <tr class="loading-row">
                <td colspan="5">Loading credentials...</td>
            </tr>
        """.trimIndent()

        val walletsTable = document.getElementById("wallets-list") as? HTMLTableSectionElement
        walletsTable?.innerHTML = """
            <tr class="loading-row">
                <td colspan="2">Loading wallets...</td>
            </tr>
        """.trimIndent()
    }

    /**
     * Show error state.
     */
    fun showError(message: String) {
        val credentialsTable = document.getElementById("credentials-list") as? HTMLTableSectionElement
        credentialsTable?.innerHTML = """
            <tr class="loading-row">
                <td colspan="5" style="color: #EF4444;">Error: $message</td>
            </tr>
        """.trimIndent()

        val walletsTable = document.getElementById("wallets-list") as? HTMLTableSectionElement
        walletsTable?.innerHTML = """
            <tr class="loading-row">
                <td colspan="2" style="color: #EF4444;">Error: $message</td>
            </tr>
        """.trimIndent()
    }

    /**
     * Get the currently connected wallet address from localStorage.
     */
    private fun getCurrentWalletAddress(): String? {
        return kotlinx.browser.window.localStorage.getItem("connectedAddress")
    }
}
