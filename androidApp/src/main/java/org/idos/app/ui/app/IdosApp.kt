package org.idos.app.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import org.idos.app.navigation.NavRoute
import org.idos.app.navigation.NavigationManager
import org.idos.app.ui.screens.credentials.CredentialDetailScreen
import org.idos.app.ui.screens.credentials.CredentialDetailViewModel
import org.idos.app.ui.screens.credentials.CredentialsScreen
import org.idos.app.ui.screens.mnemonic.MnemonicScreen
import org.idos.app.ui.screens.settings.SettingsScreen
import org.idos.app.ui.screens.wallets.WalletsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun LoadingScreen() {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading wallet...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdosApp() {
    val viewModel: IdosAppViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.isLoading) {
        return LoadingScreen()
    }

    val navController = rememberNavController()
    val navigationManager: NavigationManager = koinInject()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    navigationManager.SetupNavigation(navController)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val startRoute = if (state.isConnected) NavRoute.DrawerRoute.Credentials else NavRoute.DrawerRoute.Mnemonic
    val currentRoute = navBackStackEntry?.destination?.route ?: startRoute.route

    // Update title based on current route
    val title =
        remember(currentRoute) {
            NavRoute.all
                .find { it.route == currentRoute }
                ?.title ?: startRoute.title
        }

    // Filter out Mnemonic route if wallet is connected
    val drawerRoutes =
        remember(state.ethAddress) {
            if (state.isConnected) {
                NavRoute.DrawerRoute.connected
            } else {
                NavRoute.DrawerRoute.login
            }
        }

    LaunchedEffect(state.isConnected) {
        val targetRoute =
            if (state.isConnected) {
                NavRoute.DrawerRoute.Credentials.route
            } else {
                NavRoute.DrawerRoute.Mnemonic.route
            }

        // Only navigate if we're not already on the target route
        if (navController.currentDestination?.route != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(0) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        DrawerContent(
            currentRoute = currentRoute,
            ethAddress = state.ethAddress,
            onDisconnect = {
                scope.launch {
                    drawerState.close()
                    viewModel.disconnectWallet()
                }
            },
            onNavigate = { route ->
                if (route != currentRoute) {
                    scope.launch {
                        drawerState.close()
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            },
            routes = drawerRoutes,
        )
    }) {
        Scaffold(topBar = {
            TopAppBar(title = {
                Text(text = title)
            }, navigationIcon = {
                IconButton(
                    onClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                    )
                }
            })
        }) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startRoute.route,
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
            ) {
                composable(NavRoute.DrawerRoute.Credentials.route) { CredentialsScreen() }
                composable(NavRoute.DrawerRoute.Wallets.route) { WalletsScreen() }
                composable(NavRoute.DrawerRoute.Settings.route) { SettingsScreen() }
                composable(NavRoute.DrawerRoute.Mnemonic.route) { MnemonicScreen() }
                composable(
                    route = NavRoute.CredentialDetail.route,
                    arguments =
                        listOf(
                            navArgument(NavRoute.CredentialDetail.CREDENTIAL_ID_ARG) {
                                type = NavType.StringType
                                nullable = false
                            },
                        ),
                ) { backStackEntry ->
                    val vm =
                        koinViewModel<CredentialDetailViewModel>(
                            viewModelStoreOwner = backStackEntry,
                            parameters = { parametersOf(backStackEntry.savedStateHandle) },
                        )
                    CredentialDetailScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    currentRoute: String,
    ethAddress: String,
    onDisconnect: () -> Unit,
    onNavigate: (String) -> Unit,
    routes: List<NavRoute.DrawerRoute>,
) {
    val items =
        routes.map { route ->
            val icon =
                when (route) {
                    NavRoute.DrawerRoute.Credentials -> Icons.Default.CreditCard
                    NavRoute.DrawerRoute.Wallets -> Icons.Default.VpnKey
                    NavRoute.DrawerRoute.Settings -> Icons.Default.Settings
                    NavRoute.DrawerRoute.Mnemonic -> Icons.Default.VpnKey
                }
            Triple(route, icon, null as String?)
        }

    val isConnected = ethAddress.isNotBlank()
    val buttonHeight = 40.dp
    val sectionHeight = 160.dp // Fixed height for the header section

    ModalDrawerSheet {
        // Wallet connection status section with fixed height
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(sectionHeight)
                    .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = if (isConnected) "Connected Wallet" else "No Wallet Connected",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isConnected) {
                    Text(
                        text = "${ethAddress.take(6)}...${ethAddress.takeLast(4)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = "Connect a wallet to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            OutlinedButton(
                onClick = onDisconnect,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                enabled = isConnected,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    ),
            ) {
                Text(if (isConnected) "Disconnect Wallet" else "No Wallet Connected")
            }
        }

        // Divider between header and navigation items
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        // Navigation items
        items.forEach { (route, icon, _) ->
            NavigationDrawerItem(
                label = { Text(route.title) },
                selected = currentRoute == route.route,
                onClick = { onNavigate(route.route) },
                icon = { Icon(icon, contentDescription = route.title) },
                colors =
                    NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unselectedContainerColor = MaterialTheme.colorScheme.surface,
                        selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
