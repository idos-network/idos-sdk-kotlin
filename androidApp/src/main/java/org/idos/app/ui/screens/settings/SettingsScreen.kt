package org.idos.app.ui.screens.settings

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

    // Show success/error messages
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long,
            )
            viewModel.onEvent(SettingsEvent.ClearError)
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
                        .padding(vertical = 8.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Encryption Key",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Encryption Key")
                    Spacer(modifier = Modifier.weight(1f))
                    if (state.hasEncryptionKey) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Key exists",
                            tint = Color.Green,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "No key",
                            tint = Color.Red,
                        )
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
