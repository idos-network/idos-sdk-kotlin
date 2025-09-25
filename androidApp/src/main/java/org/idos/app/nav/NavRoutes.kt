package org.idos.app.nav

sealed class NavRoute(val route: String, val title: String) {
    data object Credentials : NavRoute("credentials", "Credentials")
    data object Wallets : NavRoute("wallets", "Wallets")
    data object Settings : NavRoute("settings", "Settings")
    data object Mnemonic : NavRoute("mnemonic", "Import Mnemonic")

    companion object {
        val all = lazy { listOf(Credentials, Wallets, Settings, Mnemonic) }
    }
}
