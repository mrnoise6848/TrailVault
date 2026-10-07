package com.noise.trailvault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noise.trailvault.domain.*

@Composable
fun MetadataDialog(trail: Trail, onDismiss: () -> Unit, onSave: (Trail) -> Unit) {
    var activity by remember(trail.id) { mutableStateOf(trail.activity) }
    var notes by remember(trail.id) { mutableStateOf(trail.notes) }
    var tags by remember(trail.id) { mutableStateOf(trail.tags) }
    var favorite by remember(trail.id) { mutableStateOf(trail.favorite) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Route information") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Activity")
                ActivityType.entries.forEach { type ->
                    FilterChip(selected = activity == type, onClick = { activity = type }, label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
                OutlinedTextField(tags, { tags = it.take(500) }, label = { Text("Tags, separated by commas") })
                OutlinedTextField(notes, { notes = it.take(4000) }, label = { Text("Notes") }, minLines = 3, maxLines = 6)
                Row { Checkbox(favorite, { favorite = it }); Text("Favorite", Modifier.padding(top = 12.dp)) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(trail.copy(activity = activity, notes = notes,
            tags = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(", "), favorite = favorite)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
