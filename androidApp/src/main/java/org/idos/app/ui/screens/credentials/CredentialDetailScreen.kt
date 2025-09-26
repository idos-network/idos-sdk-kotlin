package org.idos.app.ui.screens.credentials

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.idos.app.data.model.CredentialDetail
import org.idos.app.ui.screens.base.BaseScreen
import org.idos.app.ui.screens.base.spacing
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialDetailScreen(viewModel: CredentialDetailViewModel) {
    val state by viewModel.state.collectAsState()

    BaseScreen {
        when (val s = state) {
            is CredentialDetailState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is CredentialDetailState.Success -> {
                CredentialDetailContent(
                    credential = s.credential,
                    decryptedContent = s.decryptedContent,
                )
            }

            is CredentialDetailState.Error -> {
                ErrorState(
                    message = s.message,
                    onRetry = { viewModel.onEvent(CredentialDetailEvent.LoadCredential) },
                )
            }
        }
    }
}

@Composable
private fun CredentialDetailContent(
    credential: CredentialDetail,
    decryptedContent: String,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(spacing.medium)
                .verticalScroll(scrollState),
    ) {
        // Credential metadata
        CredentialInfoItem("ID", credential.id.value)
        Spacer(modifier = Modifier.height(spacing.small))
//        CredentialInfoItem("Type", credential.type)
//        Spacer(modifier = Modifier.height(spacing.medium))

        // Decrypted content section
        Text(
            text = "Content",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(spacing.small))

        // Display the decrypted content with monospace font
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.small),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = decryptedContent,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.padding(spacing.medium),
            )
        }
    }
}

@Composable
private fun CredentialInfoItem(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(spacing.small))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(spacing.medium))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
