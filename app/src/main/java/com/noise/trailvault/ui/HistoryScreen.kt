package com.noise.trailvault.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noise.trailvault.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun dateLabel(time: Long): String = DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm")
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(time))

@Composable
fun HistoryScreen(routes: List<TrackSummary>, onBack: () -> Unit, onOpen: (String) -> Unit,
    onUpdate: (Trail) -> Unit, onDelete: (String) -> Unit, onImport: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var activity by rememberSaveable { mutableStateOf<ActivityType?>(null) }
    var favorites by rememberSaveable { mutableStateOf(false) }
    var recent by rememberSaveable { mutableStateOf(false) }
    val filtered = remember(routes, query, activity, favorites, recent) {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        routes.filter { summary ->
            val trail = summary.trail
            (query.isBlank() || listOf(trail.name, trail.tags, trail.activity.name).any { it.contains(query.trim(), ignoreCase = true) }) &&
                (activity == null || trail.activity == activity) && (!favorites || trail.favorite) && (!recent || trail.startTime >= cutoff)
        }
    }
    var rename by remember { mutableStateOf<Trail?>(null) }
    var deleting by remember { mutableStateOf<Trail?>(null) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Recording") }
        Text("My Routes", style = MaterialTheme.typography.headlineLarge)
        OutlinedButton(onClick = onImport) { Text("Import GPX") }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(query, { query = it.take(160) }, label = { Text("Search name, activity or tags") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = favorites, onClick = { favorites = !favorites }, label = { Text("Favorites") })
            FilterChip(selected = recent, onClick = { recent = !recent }, label = { Text("Last 30 days") })
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActivityType.entries.forEach { type -> FilterChip(selected = activity == type,
                onClick = { activity = if (activity == type) null else type }, label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
        }
        if (routes.isNotEmpty() && filtered.isEmpty()) Text("No routes match your filters.")
        if (routes.isEmpty()) Text("Your routes belong here. Record your first route to get started.")
            }
            }
            items(filtered, key = { it.trail.id }) { summary ->
                val trail = summary.trail
                Column(Modifier.fillMaxWidth().clickable { onOpen(trail.id) }.padding(vertical = 12.dp)) {
                    Text((if (trail.favorite) "★ " else "") + trail.name, style = MaterialTheme.typography.titleLarge)
                    Text("%.2f km · %s".format(summary.statistics.distanceMeters / 1000, durationLabel(summary.statistics.durationMillis)))
                    Text(dateLabel(trail.startTime), style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        TextButton(onClick = { rename = trail }) { Text("Rename") }
                        TextButton(onClick = { onUpdate(trail.copy(favorite = !trail.favorite)) }) { Text(if (trail.favorite) "Unfavorite" else "Favorite") }
                        TextButton(onClick = { deleting = trail }) { Text("Delete") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
    rename?.let { trail -> RenameDialog(trail, { rename = null }) { onUpdate(it); rename = null } }
    deleting?.let { trail -> DeleteDialog(trail, { deleting = null }) { onDelete(trail.id); deleting = null } }
}

@Composable
fun RenameDialog(trail: Trail, onDismiss: () -> Unit, onSave: (Trail) -> Unit) {
    var name by remember(trail.id) { mutableStateOf(trail.name) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Rename route") },
        text = { OutlinedTextField(name, { name = it.take(160) }, label = { Text("Route name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(trail.copy(name = name.trim())) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
@Composable
fun DeleteDialog(trail: Trail, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Delete route?") },
        text = { Text("${trail.name} and its recorded points will be permanently removed from this device.") },
        confirmButton = { TextButton(onClick = onDelete) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
