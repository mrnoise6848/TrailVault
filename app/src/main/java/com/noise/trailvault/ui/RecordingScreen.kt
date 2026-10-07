package com.noise.trailvault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noise.trailvault.domain.*
import java.util.Locale

fun durationLabel(millis: Long): String {
    val seconds = millis.coerceAtLeast(0) / 1000
    return "%d:%02d:%02d".format(Locale.getDefault(), seconds / 3600, seconds / 60 % 60, seconds % 60)
}
@Composable
fun StatisticsView(statistics: RouteStatistics) {
    Text("%.2f km".format(Locale.getDefault(), statistics.distanceMeters / 1000), style = MaterialTheme.typography.headlineLarge)
    Text("Active duration  ${durationLabel(statistics.durationMillis)}")
    Text("Average speed  " + if (statistics.durationMillis > 0) "%.1f km/h".format(Locale.getDefault(), statistics.averageSpeedKmh) else "Unavailable")
    Text("Moving time  ${statistics.movingMillis?.let(::durationLabel) ?: "Unavailable"}")
    Text("Elevation gain  ${statistics.elevationGainMeters?.let { "≈ %.0f m".format(it) } ?: "Unavailable"}")
}
@Composable
fun RecordingScreen(snapshot: RecordingSnapshot, onStart: (ActivityType) -> Unit,
    onPause: () -> Unit, onResume: () -> Unit, onFinish: () -> Unit, onHistory: () -> Unit) {
    var activity by rememberSaveable { mutableStateOf(ActivityType.WALKING) }
    var follow by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("TrailVault", style = MaterialTheme.typography.headlineMedium)
        Text("Record your route. Keep it yours.")
        if (snapshot.trail == null) {
            Text("Choose an activity")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActivityType.entries.forEach { type ->
                    FilterChip(selected = type == activity, onClick = { activity = type }, label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }
            Button(onClick = { onStart(activity) }, modifier = Modifier.fillMaxWidth()) { Text("Start Route") }
        } else {
            Text(if (snapshot.trail.state == RecordingState.PAUSED) "Paused" else "Recording", style = MaterialTheme.typography.titleLarge)
            StatisticsView(snapshot.statistics)
            snapshot.points.lastOrNull()?.accuracy?.let { Text("GPS accuracy  ±%.0f m".format(it)) }
            snapshot.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (snapshot.points.isEmpty()) Text("Waiting for GPS. Move outdoors for a better signal.")
            MapPanel(snapshot.points, follow)
            FilterChip(selected = follow, onClick = { follow = !follow }, label = { Text("Follow position") })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = if (snapshot.trail.state == RecordingState.PAUSED) onResume else onPause) {
                    Text(if (snapshot.trail.state == RecordingState.PAUSED) "Resume" else "Pause")
                }
                OutlinedButton(onClick = onFinish) { Text("Finish & Save") }
            }
        }
        OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("My Routes") }
    }
}
