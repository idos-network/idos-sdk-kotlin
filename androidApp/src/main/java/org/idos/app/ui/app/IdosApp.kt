package org.idos.app.ui.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.idos.app.nav.NavRoute
import org.idos.app.ui.screens.credentials.CredentialsScreen
import org.idos.app.ui.screens.mnemonic.MnemonicScreen
import org.idos.app.ui.screens.settings.SettingsScreen
import org.idos.app.ui.screens.wallets.WalletsScreen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdosApp(
    viewModel: IdosAppViewModel = koinViewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val route = if (uiState.isConnected) NavRoute.Credentials else NavRoute.Mnemonic
    val currentRoute = navBackStackEntry?.destination?.route ?: route.route

    // Update title based on current route
    val title = remember(currentRoute) {
        NavRoute.all.value.find { it.route == currentRoute }?.title ?: route.title
    }

    // Filter out Mnemonic route if wallet is connected
    val filteredRoutes = remember(uiState.ethAddress) {
        if (uiState.isConnected) {
            NavRoute.all.value.filter { it != NavRoute.Mnemonic }
        } else {
            NavRoute.all.value.filter { it !in listOf(NavRoute.Credentials, NavRoute.Wallets) }
        }
    }

    LaunchedEffect(uiState.isConnected) {
        val targetRoute = if (uiState.isConnected) {
            NavRoute.Credentials.route
        } else {
            NavRoute.Mnemonic.route
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

    ModalNavigationDrawer(
        drawerState = drawerState, drawerContent = {
            DrawerContent(
                currentRoute = currentRoute, ethAddress = uiState.ethAddress, onDisconnect = {
                    scope.launch { drawerState.close() }
                    viewModel.disconnectWallet()
                }, onNavigate = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    }
                }, routes = filteredRoutes
            )
        }) {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Text(
                        text = title, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }, navigationIcon = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }, modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu, contentDescription = "Menu"
                        )
                    }
                })
            }) { innerPadding ->
            NavHost(
                navController = navController, startDestination = currentRoute, modifier = Modifier.padding(innerPadding)
            ) {
                composable(NavRoute.Credentials.route) { CredentialsScreen() }
                composable(NavRoute.Wallets.route) { WalletsScreen() }
                composable(NavRoute.Settings.route) { SettingsScreen() }
                composable(NavRoute.Mnemonic.route) { MnemonicScreen() }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    currentRoute: String, ethAddress: String, onDisconnect: () -> Unit, onNavigate: (String) -> Unit, routes: List<NavRoute>
) {
    val items = routes.map { route ->
        val icon = when (route) {
            NavRoute.Credentials -> Icons.Default.CreditCard
            NavRoute.Wallets -> Icons.Default.VpnKey
            NavRoute.Settings -> Icons.Default.Settings
            NavRoute.Mnemonic -> Icons.Default.VpnKey
        }
        Triple(route, icon, null as String?)
    }

    val isConnected = ethAddress.isNotBlank()
    val buttonHeight = 40.dp
    val sectionHeight = 160.dp // Fixed height for the header section

    ModalDrawerSheet {
        // Wallet connection status section with fixed height
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sectionHeight)
                .padding(16.dp), verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isConnected) "Connected Wallet" else "No Wallet Connected",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isConnected) {
                    Text(
                        text = "${ethAddress.take(6)}...${ethAddress.takeLast(4)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Connect a wallet to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                enabled = isConnected,
                colors = ButtonDefaults.outlinedButtonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Text(if (isConnected) "Disconnect Wallet" else "No Wallet Connected")
            }
        }

        // Divider between header and navigation items
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Navigation items
        items.forEach { (route, icon, _) ->
            NavigationDrawerItem(
                label = { Text(route.title) },
                selected = currentRoute == route.route,
                onClick = { onNavigate(route.route) },
                icon = { Icon(icon, contentDescription = route.title) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedContainerColor = MaterialTheme.colorScheme.surface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
