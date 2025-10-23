package org.idos.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.idos.app.navigation.NavigationManager
import org.idos.app.ui.screens.login.LoginScreen
import org.idos.app.ui.screens.login.LoginViewModel
import org.idos.app.ui.screens.mnemonic.MnemonicScreen
import org.idos.app.ui.theme.AppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalSharedTransitionApi::class)
class LoginActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash screen visible until we have definitive user state
        splashScreen.setKeepOnScreenCondition {
            val currentState = loginViewModel.state.value
            !currentState.showConnectButton && !currentState.isConnectedUser
        }

        setContent {
            AppTheme {
                val uiState by loginViewModel.state.collectAsState()
                val navController = rememberNavController()
                val navigationManager: NavigationManager = koinInject()

                // Set up navigation for this activity
                navigationManager.SetupNavigation(navController)

                // Navigate to main app when user is fully connected
                LaunchedEffect(uiState.isConnectedUser) {
                    if (uiState.isConnectedUser) {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                }

                Scaffold { paddingValues ->
                    SharedTransitionLayout {
                        NavHost(
                            navController = navController,
                            startDestination = "login",
                            modifier =
                                Modifier
                                    .padding(paddingValues)
                                    .consumeWindowInsets(paddingValues),
                        ) {
                            composable("login") {
                                LoginScreen(
                                    viewModel = loginViewModel,
                                    animatedVisibilityScope = this,
                                )
                            }
                            composable("mnemonic") {
                                MnemonicScreen(animatedVisibilityScope = this)
                            }
                        }
                    }
                }
            }
        }
    }
}
