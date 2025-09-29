package org.idos.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.idos.app.data.ConnectedUser
import org.idos.app.data.repository.UserRepository
import org.idos.app.ui.dashboard.DashboardScreen
import org.idos.app.ui.theme.AppTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val userRepository: UserRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val userState by userRepository.userState.collectAsState()

                // Redirect to login if user is not authenticated
                LaunchedEffect(userState) {
                    if (userState !is ConnectedUser) {
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                }

                // Only show main app if user is authenticated
                if (userState is ConnectedUser) {
                    DashboardScreen()
                }
            }
        }
    }
}
