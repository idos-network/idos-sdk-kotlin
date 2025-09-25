package org.idos.app.ui.screens.mnemonic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.idos.app.ui.screens.base.BaseScreen
import org.idos.app.ui.screens.base.spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MnemonicScreen(
    viewModel: MnemonicViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Show success dialog when wallet is created
    if (state.isSuccess) {
        Dialog(onDismissRequest = { viewModel.onEvent(MnemonicEvent.ResetSuccess) }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Mnemonic Generated", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your mnemonic has been processed.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.onEvent(MnemonicEvent.ResetSuccess) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }

    BaseScreen {
        Column(Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(
                text = "Import BIP39 Mnemonic",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text("Word Count", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Button(
                    onClick = { viewModel.onEvent(MnemonicEvent.UpdateWordCount(12)) },
                    enabled = state.wordCount != 12
                ) {
                    Text("12 words")
                }
                Button(
                    onClick = { viewModel.onEvent(MnemonicEvent.UpdateWordCount(24)) },
                    enabled = state.wordCount != 24
                ) {
                    Text("24 words")
                }
            }

            Text("Mnemonic Words", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(state.wordCount) { index ->
                    OutlinedTextField(
                        value = state.words.getOrElse(index) { "" },
                        onValueChange = { viewModel.onEvent(MnemonicEvent.UpdateWord(index, it)) },
                        label = { Text("#${index + 1}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.passphrase,
                onValueChange = { viewModel.onEvent(MnemonicEvent.UpdatePassphrase(it)) },
                label = { Text("Passphrase (optional)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.onEvent(MnemonicEvent.GenerateWallet) },
                enabled = state.isGenerateButtonEnabled && !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    Text("Processing...")
                } else {
                    Text("Generate Wallet")
                }
            }

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
