package org.idos.app.ui.screens.base

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyGenerationDialog(
    onGenerateKey: (password: String, expiration: Duration) -> Unit,
    onDismiss: () -> Unit,
    isGenerating: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("") }
    var selectedExpiration by remember { mutableStateOf(KeyGenerationOptions.DEFAULT_OPTIONS.first()) }
    var passwordVisible by remember { mutableStateOf(false) }

//    val isPasswordValid = password.length >= 8
    val isPasswordValid = true
    val canGenerate = isPasswordValid && !isGenerating

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        modifier = modifier,
        title = {
            Text("Generate Encryption Key")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                Text(
                    text = "Create a secure password to generate your encryption key.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            )
                        }
                    },
                    isError = password.isNotEmpty() && !isPasswordValid,
                    supportingText = {
                        if (password.isNotEmpty() && !isPasswordValid) {
                            Text(
                                text = "Password must be at least 8 characters",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    enabled = !isGenerating,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Expiration selection
                Text(
                    text = "Key Expiration",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Column {
                    KeyGenerationOptions.DEFAULT_OPTIONS.forEach { option ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selectedExpiration == option,
                                        onClick = { selectedExpiration = option },
                                        role = Role.RadioButton,
                                    ).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedExpiration == option,
                                onClick = null, // handled by selectable
                                enabled = !isGenerating,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                // Error message
                if (error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(MaterialTheme.spacing.medium),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerateKey(password, selectedExpiration.duration) },
                enabled = canGenerate,
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Generate Key")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating,
            ) {
                Text("Cancel")
            }
        },
    )
}
