import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * Navigation between app pages.
 */
object Navigation {
    fun showConnectPage() {
        val connectPage = document.getElementById("connect-page") as? HTMLElement
        val profilePage = document.getElementById("profile-page") as? HTMLElement

        connectPage?.classList?.remove("hidden")
        profilePage?.classList?.add("hidden")
    }

    fun showProfilePage() {
        val connectPage = document.getElementById("connect-page") as? HTMLElement
        val profilePage = document.getElementById("profile-page") as? HTMLElement

        connectPage?.classList?.add("hidden")
        profilePage?.classList?.remove("hidden")
    }
}
