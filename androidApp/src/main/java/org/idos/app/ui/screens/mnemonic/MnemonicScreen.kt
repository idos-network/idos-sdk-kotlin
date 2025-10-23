package org.idos.app.ui.screens.mnemonic

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.idos.app.ui.screens.base.BaseScreen
import org.idos.app.ui.screens.base.spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.MnemonicScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: MnemonicViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Show dialog for loading, success, or error states
    if (state.isLoading || state.isSuccess || state.error != null) {
        Dialog(
            onDismissRequest = {
                // Only allow dismissal if showing error (not loading or success)
                if (!state.isLoading && !state.isSuccess) {
                    viewModel.onEvent(MnemonicEvent.ClearError)
                }
            },
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(0.9f),
            ) {
                Column(
                    modifier =
                        Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                            .height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    when {
                        state.isLoading && !state.isSuccess -> {
                            // Initial wallet import loading
                            Text("Processing Wallet", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Please wait while we import your wallet...",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        state.isSuccess -> {
                            Text("Wallet Imported", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))

                            if (state.isLoading) {
                                // Loading after clicking OK (fetching profile)
                                Text(
                                    text = "Loading profile...",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            } else {
                                Text(
                                    text = "Your wallet has been successfully imported.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { viewModel.onEvent(MnemonicEvent.ResetSuccess) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("OK")
                                }
                            }
                        }

                        else -> {
                            Text("Import Failed", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { viewModel.onEvent(MnemonicEvent.ClearError) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
    }

    BaseScreen {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(MaterialTheme.spacing.medium),
        ) {
            Text(
                text = "Import BIP39 Mnemonic",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text("Recovery Phrase", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Enter your 12 or 24 word recovery phrase",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = state.mnemonic,
                onValueChange = { viewModel.onEvent(MnemonicEvent.UpdateMnemonic(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("word1 word2 word3 ...") },
                maxLines = 5
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.derivationPath,
                onValueChange = { viewModel.onEvent(MnemonicEvent.UpdateDerivationPath(it)) },
                label = { Text("Derivation Path") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.onEvent(MnemonicEvent.GenerateWallet) },
                enabled = state.isGenerateButtonEnabled && !state.isLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "primaryButton"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
            ) {
                Text("Generate Wallet")
            }
        }
    }
}
