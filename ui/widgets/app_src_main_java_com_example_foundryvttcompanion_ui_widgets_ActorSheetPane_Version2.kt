package com.example.foundryvttcompanion.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foundryvttcompanion.viewmodel.SessionState

@Composable
fun ActorSheetPane(
    state: SessionState,
    onSelectActor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Actor Sheet (Native)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        if (state.actors.isEmpty()) {
            Text("No actors available.")
            return
        }
        androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            state.actors.forEach { actor ->
                AssistChip(
                    onClick = { onSelectActor(actor.id) },
                    label = { Text(actor.name) },
                    selected = state.selectedActor?.id == actor.id
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        val actor = state.selectedActor ?: state.actors.first()
        ElevatedCard {
            LazyColumn(Modifier.padding(16.dp)) {
                item {
                    Text(actor.name, style = MaterialTheme.typography.headlineSmall)
                    Text(actor.type, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                }
                item {
                    Text("Attributes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }
                items(actor.attributes.size) { idx ->
                    val a = actor.attributes[idx]
                    AttributeRow(a.label, a.value, a.max)
                }
                item { Spacer(Modifier.height(12.dp)); Text("Resources", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp)) }
                items(actor.resources.size) { idx ->
                    val r = actor.resources[idx]
                    ResourceSlider(r.label, r.current, r.max)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AttributeRow(label: String, value: Int, max: Int?) {
    OutlinedCard(Modifier.padding(vertical = 4.dp)) {
        androidx.compose.foundation.layout.Row(Modifier.padding(12.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(if (max != null) "$value / $max" else "$value", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ResourceSlider(label: String, current: Int, max: Int) {
    val sliderState = remember { mutableFloatStateOf(current.toFloat()) }
    androidx.compose.material3.Card(Modifier.padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("$label: ${sliderState.floatValue.toInt()} / $max")
            Slider(
                value = sliderState.floatValue,
                onValueChange = { sliderState.floatValue = it },
                valueRange = 0f..max.toFloat()
            )
        }
    }
}