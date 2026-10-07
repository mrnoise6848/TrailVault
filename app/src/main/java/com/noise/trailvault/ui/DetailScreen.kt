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
fun DetailScreen(trail: Trail, points: List<RoutePoint>, statistics: RouteStatistics,
    onBack: () -> Unit, onUpdate: (Trail) -> Unit, onDelete: () -> Unit,
    onExport: () -> Unit) {
    var rename by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← My Routes") }
        Text(trail.name, style = MaterialTheme.typography.headlineLarge)
        Text(trail.activity.name.lowercase().replaceFirstChar { it.uppercase() })
        StatisticsView(statistics)
        if (points.isEmpty()) Text("No GPS points were recorded for this route.")
        else MapPanel(points, follow = false)
        Text("Started  ${if (points.any { it.timestamp > 0 }) dateLabel(trail.startTime) else "Unavailable (imported)"}")
        Text("Finished  ${trail.endTime?.let(::dateLabel) ?: "Unavailable"}")
        Text("${points.size} recorded points")
        if (trail.tags.isNotBlank()) Text("Tags  ${trail.tags}")
        if (trail.notes.isNotBlank()) Text(trail.notes)
        Button(onClick = onExport, enabled = points.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Export GPX") }
        OutlinedButton(onClick = { editing = true }) { Text("Edit activity, tags & notes") }
        Row {
            TextButton(onClick = { rename = true }) { Text("Rename") }
            TextButton(onClick = { onUpdate(trail.copy(favorite = !trail.favorite)) }) { Text(if (trail.favorite) "★ Favorited" else "Favorite") }
            TextButton(onClick = { deleting = true }) { Text("Delete") }
        }
    }
    if (editing) MetadataDialog(trail, { editing = false }) { onUpdate(it); editing = false }
    if (rename) RenameDialog(trail, { rename = false }) { onUpdate(it); rename = false }
    if (deleting) DeleteDialog(trail, { deleting = false }) { deleting = false; onDelete() }
}
