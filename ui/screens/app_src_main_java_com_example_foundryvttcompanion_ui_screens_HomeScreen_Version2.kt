package com.example.foundryvttcompanion.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foundryvttcompanion.ui.widgets.ActorSheetPane
import com.example.foundryvttcompanion.ui.widgets.ChatPane
import com.example.foundryvttcompanion.ui.widgets.SettingsPane
import com.example.foundryvttcompanion.util.rememberWindowSizeClass
import com.example.foundryvttcompanion.viewmodel.ConnectionMode
import com.example.foundryvttcompanion.viewmodel.SessionState
import com.example.foundryvttcompanion.viewmodel.SessionViewModel

@Composable
fun HomeScreen(
    state: SessionState,
    onDisconnect: () -> Unit,
    onSendChat: (String) -> Unit,
    onSelectActor: (String) -> Unit,
    viewModel: SessionViewModel? = null
) {
    val windowSize = rememberWindowSizeClass()
    val isWide = windowSize.widthSizeClass >= androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium

    val selectedTab = remember { mutableIntStateOf(0) }

    // Controller to talk to WebView (used in Hybrid/WebView modes)
    val webController = remember { FoundryWebViewController() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FoundryVTT Companion") },
                actions = { OutlinedButton(onClick = onDisconnect) { Text("Disconnect") } },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            if (!isWide) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab.intValue == 0,
                        onClick = { selectedTab.intValue = 0 },
                        icon = { Icon(Icons.Rounded.Description, contentDescription = "Sheet") },
                        label = { Text("Sheet") }
                    )
                    NavigationBarItem(
                        selected = selectedTab.intValue == 1,
                        onClick = { selectedTab.intValue = 1 },
                        icon = { Icon(Icons.Rounded.Chat, contentDescription = "Chat") },
                        label = { Text("Chat") }
                    )
                    NavigationBarItem(
                        selected = selectedTab.intValue == 2,
                        onClick = { selectedTab.intValue = 2 },
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        val sheetContent: @Composable () -> Unit = {
            when (state.mode) {
                ConnectionMode.Hybrid, ConnectionMode.WebView -> {
                    val url = state.worldUrl ?: ""
                    FoundryWebView(
                        url = url,
                        controller = webController
                    ) { author, content, ts ->
                        // Bridge incoming chat to ViewModel
                        viewModel?.receiveChatFromBridge(author, content, ts)
                    }
                }
                ConnectionMode.Native -> {
                    ActorSheetPane(
                        state = state,
                        onSelectActor = onSelectActor
                    )
                }
            }
        }

        if (isWide) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                NavigationRail {
                    NavigationRailItem(
                        selected = selectedTab.intValue == 0,
                        onClick = { selectedTab.intValue = 0 },
                        icon = { Icon(Icons.Rounded.Description, contentDescription = "Sheet") },
                        label = { Text("Sheet") }
                    )
                    NavigationRailItem(
                        selected = selectedTab.intValue == 1,
                        onClick = { selectedTab.intValue = 1 },
                        icon = { Icon(Icons.Rounded.Chat, contentDescription = "Chat") },
                        label = { Text("Chat") }
                    )
                    NavigationRailItem(
                        selected = selectedTab.intValue == 2,
                        onClick = { selectedTab.intValue = 2 },
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                when (selectedTab.intValue) {
                    0 -> Surface(Modifier.weight(1f)) { sheetContent() }
                    1 -> Surface(Modifier.weight(1f)) {
                        ChatPane(
                            state = state,
                            onSend = { text ->
                                onSendChat(text)
                                if (state.mode != ConnectionMode.Native) {
                                    webController.sendChat(text)
                                }
                            }
                        )
                    }
                    2 -> Surface(Modifier.weight(1f)) { SettingsPane(state = state) }
                }
            }
        } else {
            when (selectedTab.intValue) {
                0 -> Surface(Modifier.padding(padding)) { sheetContent() }
                1 -> ChatPane(
                    state = state,
                    onSend = { text ->
                        onSendChat(text)
                        if (state.mode != ConnectionMode.Native) {
                            webController.sendChat(text)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
                2 -> SettingsPane(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}