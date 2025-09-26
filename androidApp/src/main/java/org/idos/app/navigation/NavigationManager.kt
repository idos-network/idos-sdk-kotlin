package org.idos.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import org.koin.core.component.KoinComponent
import timber.log.Timber

@Stable
class NavigationManager : KoinComponent {
    private val _navigationCommands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 10)
    val navigationCommands = _navigationCommands.asSharedFlow()

    suspend fun navigate(command: NavigationCommand) {
        _navigationCommands.emit(command)
    }

    @Composable
    fun SetupNavigation(navController: NavHostController) {
        LaunchedEffect(navController) {
            navigationCommands.collect { command ->
                Timber.d("Navigation command: $command")
                when (command) {
                    is NavigationCommand.NavigateToRoute -> {
                        navController.navigate(command.route, command.builder)
                    }
                    is NavigationCommand.NavigateUp -> {
                        navController.navigateUp()
                    }
                    is NavigationCommand.NavigateUpToRoute -> {
                        navController.navigateUp()
                        navController.popBackStack(
                            command.route,
                            command.inclusive,
                            command.saveState,
                        )
                    }
                    is NavigationCommand.PopUpToRoute -> {
                        navController.popBackStack(
                            command.route,
                            command.inclusive,
                            command.saveState,
                        )
                    }
                }
            }
        }
    }
}

sealed class NavigationCommand {
    data class NavigateToRoute(
        val route: String,
        val builder: NavOptionsBuilder.() -> Unit = {},
    ) : NavigationCommand()

    object NavigateUp : NavigationCommand()

    data class NavigateUpToRoute(
        val route: String,
        val inclusive: Boolean = false,
        val saveState: Boolean = false,
    ) : NavigationCommand()

    data class PopUpToRoute(
        val route: String,
        val inclusive: Boolean = false,
        val saveState: Boolean = false,
    ) : NavigationCommand()
}
