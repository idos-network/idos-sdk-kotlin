package org.idos.app.navigation

import androidx.navigation.NavBackStackEntry
import org.idos.kwil.rpc.UuidString

sealed class NavRoute(
    val route: String,
    val title: String,
) {
    // Routes that should be shown in the drawer
    sealed class DrawerRoute(
        route: String,
        title: String,
    ) : NavRoute(route, title) {
        data object Credentials : DrawerRoute("credentials", "Credentials")

        data object Wallets : DrawerRoute("wallets", "Wallets")

        data object Settings : DrawerRoute("settings", "Settings")

        data object Mnemonic : DrawerRoute("mnemonic", "Import Mnemonic")

        companion object {
            val login by lazy { listOf(Settings, Mnemonic) }
            val connected by lazy { listOf(Credentials, Wallets, Settings) }
            val all by lazy { listOf(Credentials, Wallets, Settings, Mnemonic) }
        }
    }

    // Detail routes (not in drawer)
    data object CredentialDetail : NavRoute("credential_detail/{credentialId}", "Credential Detail") {
        const val CREDENTIAL_ID_ARG = "credentialId"

        fun createRoute(credentialId: UuidString): String = "credential_detail/${credentialId.value}"

        fun fromNavArgs(navBackStackEntry: NavBackStackEntry): String = navBackStackEntry.arguments?.getString(CREDENTIAL_ID_ARG) ?: ""
    }

    companion object {
        val all by lazy { DrawerRoute.all + CredentialDetail }

        fun fromRoute(route: String?): NavRoute? = all.find { it.route == route }
    }
}
