package org.idos.app.ui.screens.credentials

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.idos.app.data.model.CredentialDetail
import org.idos.app.ui.screens.base.BaseScreen
import org.idos.app.ui.screens.base.KeyGenerationDialog
import org.idos.app.ui.screens.base.spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CredentialDetailScreen(viewModel: CredentialDetailViewModel) {
    val state by viewModel.state.collectAsState()
    val enclaveUiState by viewModel.enclaveUiState.collectAsState()

    BaseScreen {
        when (val it = state) {
            is CredentialDetailState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is CredentialDetailState.Loaded -> {
                CredentialDetailContent(
                    credential = it.credential,
                    decryptedContent = it.decryptedContent,
                    isEnabled = it.enabled,
                    isEncrypted = it.decryptedContent == null,
                    onItem = { viewModel.onEvent(CredentialDetailEvent.DecryptCredential) },
                )
            }

            is CredentialDetailState.Error -> {
                ErrorState(
                    message = it.message,
                    onRetry = { viewModel.onEvent(CredentialDetailEvent.Retry) },
                    onReset = { viewModel.onEvent(CredentialDetailEvent.LockEnclave) },
                )
            }
        }
    }

    // Enclave UI overlay (password dialog, errors)
    EnclaveUiOverlay(
        enclaveUiState = enclaveUiState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun EnclaveUiOverlay(
    enclaveUiState: EnclaveUiState,
    onEvent: (CredentialDetailEvent) -> Unit,
) {
    // Show single dialog, update values instead of recreating
    if (enclaveUiState != EnclaveUiState.Hidden) {
        val isGenerating = enclaveUiState is EnclaveUiState.Unlocking
        val error = (enclaveUiState as? EnclaveUiState.UnlockError)?.message
        val canRetry = (enclaveUiState as? EnclaveUiState.UnlockError)?.canRetry ?: true

        // Get enclave type from the sealed class base property, default to USER if null
        val enclaveType = enclaveUiState.type ?: org.idos.enclave.EnclaveKeyType.USER

        KeyGenerationDialog(
            enclaveType = enclaveType,
            onGenerateKey = { password, expiration ->
                val config =
                    org.idos.enclave.EnclaveSessionConfig(
                        expirationType = org.idos.enclave.ExpirationType.TIMED,
                        expirationMillis = expiration.inWholeMilliseconds,
                    )
                onEvent(CredentialDetailEvent.UnlockEnclave(password, config))
            },
            onDismiss = {
                onEvent(CredentialDetailEvent.DismissEnclave)
            },
            isGenerating = isGenerating,
            error = error,
            canRetry = canRetry,
        )
    }
}

@Composable
private fun CredentialDetailContent(
    credential: CredentialDetail,
    decryptedContent: JsonElement?,
    isEnabled: Boolean,
    isEncrypted: Boolean,
    onItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.medium)
                .verticalScroll(scrollState),
    ) {
        // Credential metadata
        CredentialInfoItem("ID", credential.id)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // JSON content section
        Text(
            text = "Credential Data",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Display encrypted or decrypted content
        if (isEncrypted || decryptedContent == null) {
            EncryptedContentPlaceholder(onClick = onItem, isEnabled)
        } else {
            JsonElementDisplay(jsonElement = decryptedContent)
        }
    }
}

@Composable
private fun EncryptedContentPlaceholder(
    onClick: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
        enabled = isEnabled,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "\uD83D\uDD12 Content Encrypted",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = if (isEnabled) "Enter your password to decrypt" else "Enclave not available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun JsonElementDisplay(
    jsonElement: JsonElement,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium),
        ) {
            JsonElementContent(jsonElement = jsonElement)
        }
    }
}

@Composable
private fun JsonElementContent(
    jsonElement: JsonElement,
    nestingLevel: Int = 0,
) {
    when (jsonElement) {
        is JsonObject -> {
            JsonObjectContent(jsonObject = jsonElement, nestingLevel = nestingLevel)
        }

        is JsonArray -> {
            JsonArrayContent(jsonArray = jsonElement, nestingLevel = nestingLevel)
        }

        is JsonPrimitive -> {
            JsonPrimitiveContent(jsonPrimitive = jsonElement)
        }

        is JsonNull -> {
            Text(
                text = "null",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun JsonObjectContent(
    jsonObject: JsonObject,
    nestingLevel: Int = 0,
) {
    val entries = jsonObject.entries.toList()

    entries.forEachIndexed { index, (key, value) ->
        JsonKeyValueItem(
            key = key,
            value = value,
            nestingLevel = nestingLevel,
        )

        if (index < entries.size - 1) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        }
    }
}

@Composable
private fun JsonKeyValueItem(
    key: String,
    value: JsonElement,
    nestingLevel: Int = 0,
) {
    val indentationPadding = (nestingLevel * 16).dp

    Column(
        modifier = Modifier.padding(start = indentationPadding),
    ) {
        // Key label
        Text(
            text = key,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        // Value content
        when (value) {
            is JsonObject -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(MaterialTheme.spacing.small),
                    ) {
                        JsonObjectContent(
                            jsonObject = value,
                            nestingLevel = nestingLevel + 1,
                        )
                    }
                }
            }

            is JsonArray -> {
                JsonArrayContent(
                    jsonArray = value,
                    nestingLevel = nestingLevel + 1,
                )
            }

            is JsonPrimitive -> {
                JsonPrimitiveContent(jsonPrimitive = value)
            }

            is JsonNull -> {
                Text(
                    text = "null",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun JsonArrayContent(
    jsonArray: JsonArray,
    nestingLevel: Int = 0,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.small),
        ) {
            jsonArray.forEachIndexed { index, item ->
                JsonKeyValueItem(
                    key = "[$index]",
                    value = item,
                    nestingLevel = nestingLevel,
                )

                if (index < jsonArray.size - 1) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                }
            }
        }
    }
}

@Composable
private fun JsonPrimitiveContent(jsonPrimitive: JsonPrimitive) {
    val displayText =
        when {
            jsonPrimitive.isString -> "\"${jsonPrimitive.content}\""
            else -> jsonPrimitive.content
        }

    val textColor =
        when {
            jsonPrimitive.booleanOrNull != null -> MaterialTheme.colorScheme.tertiary
            jsonPrimitive.doubleOrNull != null || jsonPrimitive.longOrNull != null -> MaterialTheme.colorScheme.secondary
            jsonPrimitive.isString -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurface
        }

    Text(
        text = displayText,
        style = MaterialTheme.typography.bodyMedium,
        color = textColor,
    )
}

@Composable
private fun CredentialInfoItem(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Button(onClick = onRetry) {
            Text("Retry")
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Button(onClick = onReset) {
            Text("Reset Key")
        }
    }
}
