package com.example.foundryvttcompanion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.foundryvttcompanion.ui.screens.ConnectScreen
import com.example.foundryvttcompanion.ui.screens.HomeScreen
import com.example.foundryvttcompanion.viewmodel.SessionState
import com.example.foundryvttcompanion.viewmodel.SessionViewModel
import com.example.foundryvttcompanion.viewmodel.UserConnectionInput

object Routes {
    const val Connect = "connect"
    const val Home = "home"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    sessionState: SessionState,
    onConnect: (UserConnectionInput) -> Unit,
    onDisconnect: () -> Unit,
    onSendChat: (String) -> Unit,
    onSelectActor: (String) -> Unit,
    sessionViewModel: SessionViewModel
) {
    val startDestination = if (sessionState.connected) Routes.Home else Routes.Connect

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.Connect) {
            ConnectScreen(
                state = sessionState,
                onConnect = {
                    onConnect(it)
                    // Navigation will react when connected becomes true
                }
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                state = sessionState,
                onDisconnect = {
                    onDisconnect()
                    navController.navigate(Routes.Connect) {
                        popUpTo(Routes.Home) { inclusive = true }
                    }
                },
                onSendChat = onSendChat,
                onSelectActor = onSelectActor,
                viewModel = sessionViewModel
            )
        }
    }
}