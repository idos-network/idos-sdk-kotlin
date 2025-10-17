package org.idos.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.idos.app.ui.screens.base.BaseScreen
import org.idos.app.ui.screens.base.spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state = viewModel.state.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }

    // Check key status when screen is first shown
    LaunchedEffect(Unit) {
        viewModel.onEvent(SettingsEvent.CheckKeyStatus)
    }

    // Show error messages
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long,
            )
            viewModel.onEvent(SettingsEvent.ClearError)
        }
    }

    // Show snackbar messages
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
            )
            viewModel.onEvent(SettingsEvent.DismissSnackbar)
        }
    }

    BaseScreen(
        snackbarHostState = snackbarHostState,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(MaterialTheme.spacing.medium)
                    .fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.width(16.dp))
            // Encryption Key Section
            Text("Security", style = MaterialTheme.typography.titleMedium)

            // Encryption Key Status
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { viewModel.onEvent(SettingsEvent.OnEncryptionStatusClick) },
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (val status = state.enclaveStatus) {
                        is EnclaveUiStatus.Unlocked -> {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Unlocked",
                                tint = Color.Green,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enclave Unlocked", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    status.formattedExpiration,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        is EnclaveUiStatus.Locked -> {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enclave Locked", style = MaterialTheme.typography.titleMedium)
                                status.formattedExpiration?.let { expiration ->
                                    Text(
                                        expiration,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        EnclaveUiStatus.NotAvailable -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Not Available",
                                tint = Color.Red,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enclave Not Available", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        EnclaveUiStatus.Unlocking -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Enclave Unlocking...", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // Delete Key Button
            if (state.hasEncryptionKey) {
                Button(
                    onClick = { viewModel.onEvent(SettingsEvent.DeleteEncryptionKey) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    enabled = !state.isDeleting,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    } else {
                        Text("Delete Encryption Key")
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (state.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(SettingsEvent.CancelDelete) },
                title = { Text("Delete Encryption Key") },
                text = {
                    Text("Are you sure you want to delete your encryption key? You will need to generate a new key to decrypt credentials.")
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(SettingsEvent.ConfirmDelete) },
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { },
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}
