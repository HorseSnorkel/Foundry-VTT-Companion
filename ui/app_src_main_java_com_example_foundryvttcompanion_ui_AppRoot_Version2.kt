package com.example.foundryvttcompanion.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.foundryvttcompanion.ui.navigation.AppNavHost
import com.example.foundryvttcompanion.ui.theme.AppTheme
import com.example.foundryvttcompanion.viewmodel.SessionViewModel

@Composable
fun AppRoot(
    sessionViewModel: SessionViewModel = viewModel()
) {
    val navController = rememberNavController()
    val sessionState by sessionViewModel.uiState.collectAsState()

    AppTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavHost(
                navController = navController,
                sessionState = sessionState,
                onConnect = { sessionViewModel.connect(it) },
                onDisconnect = { sessionViewModel.disconnect() },
                onSendChat = { message -> sessionViewModel.sendChat(message) },
                onSelectActor = { actorId -> sessionViewModel.selectActor(actorId) },
                sessionViewModel = sessionViewModel
            )
        }
    }
}