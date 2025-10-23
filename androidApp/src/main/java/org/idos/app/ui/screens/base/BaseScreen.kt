package org.idos.app.ui.screens.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
        ) {
            content()
        }
    }
}

// Simple spacing tokens
val MaterialTheme.spacing: Spacing
    @Composable get() = Spacing()

class Spacing(
    val extraSmall: Dp = 8.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
)
