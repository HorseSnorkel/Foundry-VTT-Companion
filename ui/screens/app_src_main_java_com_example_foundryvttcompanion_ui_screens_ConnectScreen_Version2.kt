package com.example.foundryvttcompanion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.foundryvttcompanion.viewmodel.ConnectionMode
import com.example.foundryvttcompanion.viewmodel.SessionState
import com.example.foundryvttcompanion.viewmodel.UserConnectionInput

@Composable
fun ConnectScreen(
    state: SessionState,
    onConnect: (UserConnectionInput) -> Unit
) {
    val serverUrl = rememberSaveable { androidx.compose.runtime.mutableStateOf(state.serverUrl ?: "") }
    val world = rememberSaveable { androidx.compose.runtime.mutableStateOf(state.world ?: "") }
    val username = rememberSaveable { androidx.compose.runtime.mutableStateOf(state.username ?: "") }
    val password = rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val token = rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val modeIndex = rememberSaveable { mutableIntStateOf(0) } // 0 Hybrid, 1 WebView, 2 Native

    val scroll = rememberScrollState()

    ElevatedCard(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(scroll)
            .fillMaxWidth()
            .widthIn(max = 720.dp),
    ) {
        androidx.compose.foundation.layout.Column(
            Modifier
                .padding(24.dp)
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Connect to Foundry VTT", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(16.dp))
            ModeSelector(modeIndex.intValue) { modeIndex.intValue = it }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                label = { Text("Server URL") },
                value = serverUrl.value,
                onValueChange = { serverUrl.value = it },
                singleLine = true,
                placeholder = { Text("https://your-foundry.example.com") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                label = { Text("World (optional)") },
                value = world.value,
                onValueChange = { world.value = it },
                singleLine = true,
                placeholder = { Text("world-id or leave blank") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            if (modeIndex.intValue == 2) {
                OutlinedTextField(
                    label = { Text("Username") },
                    value = username.value,
                    onValueChange = { username.value = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    label = { Text("Password") },
                    value = password.value,
                    onValueChange = { password.value = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    label = { Text("API Token (Optional)") },
                    value = token.value,
                    onValueChange = { token.value = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    if (modeIndex.intValue == 0)
                        "Hybrid mode: Loads Foundry in a WebView for sheets while chat runs natively via a JavaScript bridge. Optionally uses a server companion module if available."
                    else
                        "WebView mode: Loads Foundry entirely in a WebView.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val mode = when (modeIndex.intValue) {
                        0 -> ConnectionMode.Hybrid
                        1 -> ConnectionMode.WebView
                        else -> ConnectionMode.Native
                    }
                    onConnect(
                        UserConnectionInput(
                            serverUrl = serverUrl.value.trim(),
                            world = world.value.trim(),
                            username = username.value.trim().ifEmpty { null },
                            password = password.value.takeIf { it.isNotEmpty() },
                            token = token.value.takeIf { it.isNotEmpty() },
                            mode = mode
                        )
                    )
                },
                enabled = state.connecting.not()
            ) { Text("Connect") }

            if (state.connecting) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator()
            }

            if (state.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(selectedIndex: Int, onSelected: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = selectedIndex == 0,
            onClick = { onSelected(0) },
            shape = SegmentedButtonDefaults.itemShape(0, 3)
        ) { Text("Hybrid (Recommended)") }
        SegmentedButton(
            selected = selectedIndex == 1,
            onClick = { onSelected(1) },
            shape = SegmentedButtonDefaults.itemShape(1, 3)
        ) { Text("WebView") }
        SegmentedButton(
            selected = selectedIndex == 2,
            onClick = { onSelected(2) },
            shape = SegmentedButtonDefaults.itemShape(2, 3)
        ) { Text("Native (Experimental)") }
    }
}