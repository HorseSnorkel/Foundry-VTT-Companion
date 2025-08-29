package com.example.foundryvttcompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foundryvttcompanion.viewmodel.models.Actor
import com.example.foundryvttcompanion.viewmodel.models.ActorAttribute
import com.example.foundryvttcompanion.viewmodel.models.ActorResource
import com.example.foundryvttcompanion.viewmodel.models.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ConnectionMode {
    Hybrid,  // WebView for sheet, native chat (via JS bridge) and optional server API
    WebView, // Full Foundry in WebView
    Native   // Experimental: pure native via companion API (if available)
}

data class UserConnectionInput(
    val serverUrl: String,
    val world: String,
    val username: String?,
    val password: String?,
    val token: String?,
    val mode: ConnectionMode
)

data class SessionState(
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val errorMessage: String? = null,
    val serverUrl: String? = null,
    val world: String? = null,
    val username: String? = null,
    val mode: ConnectionMode = ConnectionMode.Hybrid,
    val actors: List<Actor> = emptyList(),
    val selectedActor: Actor? = null,
    val chatMessages: List<ChatMessage> = emptyList()
) {
    val worldUrl: String?
        get() {
            val base = serverUrl?.trim()?.trimEnd('/')
            val w = world?.trim()
            if (base.isNullOrEmpty()) return null
            if (w.isNullOrEmpty()) return base
            // Foundry typically routes via /join or direct world routes depending on setup.
            // We keep it simple: load base and let user pick world, or accept full URL in serverUrl.
            return base
        }
}

class SessionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SessionState())
    val uiState = _uiState.asStateFlow()

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    fun connect(input: UserConnectionInput) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connecting = true,
                errorMessage = null,
                serverUrl = input.serverUrl,
                world = input.world,
                username = input.username,
                mode = input.mode
            )

            // TODO: Implement actual Foundry connection and/or companion-module auth if available.
            delay(500)

            // For Hybrid/WebView we consider "connected" once we have the URL.
            val connected = true

            // Demo data for Native mode or for initial UI while WebView loads.
            val demoActors = listOf(
                Actor(
                    id = "a1",
                    name = "Althea Stormborn",
                    type = "PC",
                    attributes = listOf(
                        ActorAttribute("Strength", 14, null),
                        ActorAttribute("Dexterity", 12, null),
                        ActorAttribute("Constitution", 13, null),
                        ActorAttribute("Intelligence", 10, null),
                        ActorAttribute("Wisdom", 8, null),
                        ActorAttribute("Charisma", 16, null)
                    ),
                    resources = listOf(
                        ActorResource("HP", 28, 34),
                        ActorResource("Spell Slots (Lv1)", 1, 4)
                    )
                ),
                Actor(
                    id = "a2",
                    name = "Kael Nightwind",
                    type = "PC",
                    attributes = listOf(
                        ActorAttribute("Strength", 10, null),
                        ActorAttribute("Dexterity", 16, null),
                        ActorAttribute("Constitution", 12, null),
                        ActorAttribute("Intelligence", 14, null),
                        ActorAttribute("Wisdom", 12, null),
                        ActorAttribute("Charisma", 11, null)
                    ),
                    resources = listOf(
                        ActorResource("HP", 21, 26),
                        ActorResource("Ki Points", 2, 3)
                    )
                )
            )

            val demoChat = listOf(
                ChatMessage(id = "m1", authorName = "GM", content = "Welcome to the session!", timestamp = Instant.now().minusSeconds(600), fmt = timeFmt),
                ChatMessage(id = "m2", authorName = "Althea", content = "Ready when you are.", timestamp = Instant.now().minusSeconds(420), fmt = timeFmt),
                ChatMessage(id = "m3", authorName = "Kael", content = "Let's do this.", timestamp = Instant.now().minusSeconds(60), fmt = timeFmt)
            )

            _uiState.value = _uiState.value.copy(
                connecting = false,
                connected = connected,
                actors = demoActors,
                selectedActor = demoActors.first(),
                chatMessages = demoChat
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            _uiState.value = SessionState()
        }
    }

    fun sendChat(message: String) {
        viewModelScope.launch {
            val now = Instant.now()
            val newMessage = ChatMessage(
                id = "local-${now.toEpochMilli()}",
                authorName = _uiState.value.username ?: "You",
                content = message,
                timestamp = now,
                fmt = timeFmt
            )
            _uiState.value = _uiState.value.copy(
                chatMessages = _uiState.value.chatMessages + newMessage
            )
            // If using companion API or JS bridge, the Screen layer should also forward to the source of truth.
        }
    }

    fun receiveChatFromBridge(author: String, content: String, tsMillis: Long) {
        viewModelScope.launch {
            val msg = ChatMessage(
                id = "bridge-$tsMillis",
                authorName = author,
                content = content,
                timestamp = Instant.ofEpochMilli(tsMillis),
                fmt = timeFmt
            )
            _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + msg)
        }
    }

    fun selectActor(actorId: String) {
        val actor = _uiState.value.actors.find { it.id == actorId } ?: return
        _uiState.value = _uiState.value.copy(selectedActor = actor)
    }
}