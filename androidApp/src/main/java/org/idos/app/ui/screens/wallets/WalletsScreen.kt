package org.idos.app.ui.screens.wallets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.idos.app.data.model.Wallet
import org.idos.app.ui.screens.base.BaseScreen
import org.idos.app.ui.screens.base.EmptyView
import org.idos.app.ui.screens.base.spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(viewModel: WalletsViewModel = koinViewModel()) {
    val state = viewModel.state.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle errors with Snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Short,
                )
                viewModel.onEvent(WalletsEvent.ClearError)
            }
        }
    }

    // Load wallets when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.onEvent(WalletsEvent.LoadWallets)
    }

    BaseScreen(
        snackbarHostState = snackbarHostState,
    ) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.onEvent(WalletsEvent.LoadWallets) },
        ) {
            if (state.wallets.isEmpty()) {
                EmptyView(
                    message = "No wallets found",
                    subMessage = "Create a new wallet to get started",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Wallet,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                ) {
                    items(state.wallets) { wallet ->
                        WalletCard(wallet = wallet)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletCard(wallet: Wallet) {
    Card(
        onClick = { /* Handle wallet selection */ },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Wallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                text = wallet.address,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
