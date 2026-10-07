package com.noise.trailvault

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.noise.trailvault.data.Gpx
import androidx.compose.ui.Modifier
import com.noise.trailvault.domain.*
import com.noise.trailvault.recording.*
import com.noise.trailvault.ui.*
import com.noise.trailvault.ui.theme.TrailVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TrailVaultTheme { TrailRoot() } }
    }
    override fun onResume() {
        super.onResume()
        val app = application as TrailApplication
        if (!LocationPermission.granted(this) && app.engine.snapshot.value.trail != null)
            app.scope.launch { app.perform { app.engine.pause() } }
    }
    @Composable
    private fun TrailRoot() {
        val app = application as TrailApplication
        val snapshot by app.engine.snapshot.collectAsState()
        val history by app.repository.history.collectAsState()
        var page by rememberSaveable { mutableStateOf("recording") }
        var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
        val completedId by app.completedId.collectAsState()
        LaunchedEffect(completedId) {
            completedId?.let { selectedId = it; page = "detail"; app.completedId.value = null }
        }
        val detail by produceState<Pair<Trail, List<RoutePoint>>?>(null, selectedId, history) {
            value = null
            selectedId?.let { id -> app.perform { value = app.repository.detail(id) } }
        }
        val error by app.error.collectAsState()
        var explain by remember { mutableStateOf(false) }
        var pendingAction by rememberSaveable { mutableStateOf("START") }
        var busy by remember { mutableStateOf(false) }
        var notice by remember { mutableStateOf<String?>(null) }
        var requestedActivity by rememberSaveable { mutableStateOf(ActivityType.WALKING) }
        var exportId by rememberSaveable { mutableStateOf<String?>(null) }
        val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gpx+xml")) { uri ->
            val id = exportId
            exportId = null
            if (uri != null && id != null) app.scope.launch {
                busy = true
                val success = app.perform {
                    withContext(Dispatchers.IO) {
                        requireNotNull(app.contentResolver.openOutputStream(uri, "wt")) { "Cannot open destination" }.use {
                            app.repository.export(id, it)
                        }
                    }
                }
                busy = false
                if (success) notice = "GPX exported successfully."
            }
        }
        val importGpx = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) app.scope.launch {
                busy = true
                app.perform {
                    val imported = withContext(Dispatchers.IO) {
                        requireNotNull(app.contentResolver.openInputStream(uri)) { "Cannot open GPX" }.use(Gpx::read)
                    }
                    app.repository.importRoute(imported.trail, imported.points)
                    selectedId = imported.trail.id; page = "detail"
                }
                busy = false
            }
        }
        fun command(action: String) {
            try {
                val intent = Intent(this@MainActivity, RecordingService::class.java).setAction(action)
                    .putExtra("activity", requestedActivity.name)
                startForegroundService(intent)
            } catch (exception: Exception) { app.error.value = "Unable to start recording. Check location permission and try again." }
        }
        val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { command(pendingAction) }
        fun continueRecording() {
            val preferences = getPreferences(MODE_PRIVATE)
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED && !preferences.getBoolean("notification_requested", false)) {
                preferences.edit().putBoolean("notification_requested", true).apply()
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else command(pendingAction)
        }
        val location = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (LocationPermission.granted(this@MainActivity)) continueRecording()
            else app.error.value = "Precise location is required to record a route. You can enable it in app settings."
        }
        BackHandler(page != "recording") {
            if (page == "detail") { selectedId = null; page = "history" } else page = "recording"
        }
        Scaffold { padding ->
            Box(Modifier.padding(padding)) {
                if (page == "history") HistoryScreen(history, onImport = { importGpx.launch(arrayOf("application/gpx+xml", "application/xml", "text/xml", "application/octet-stream", "*/*")) }, onBack = { page = "recording" },
                    onOpen = { selectedId = it; page = "detail" },
                    onUpdate = { app.scope.launch { app.perform { app.repository.update(it) } } },
                    onDelete = { app.scope.launch { app.perform { app.repository.delete(it) } } })
                else if (page == "detail") {
                    val current = detail
                    if (current == null) Column {
                        TextButton(onClick = { page = "history" }) { Text("← My Routes") }
                        Text("Loading route…")
                    } else DetailScreen(current.first, current.second,
                        history.firstOrNull { it.trail.id == current.first.id }?.statistics ?: RouteStatistics(),
                        onBack = { selectedId = null; page = "history" },
                        onUpdate = { app.scope.launch { app.perform { app.repository.update(it) } } },
                        onDelete = { app.scope.launch { if (app.perform { app.repository.delete(current.first.id) }) { selectedId = null; page = "history" } } },
                        onExport = { exportId = current.first.id; export.launch(Gpx.filename(current.first)) })
                } else RecordingScreen(snapshot,
                    onStart = { pendingAction = "START"; requestedActivity = it; explain = true },
                    onPause = { command("PAUSE") }, onResume = { if (LocationPermission.granted(this@MainActivity)) command("RESUME") else { pendingAction = "RESUME"; explain = true } },
                    onFinish = { command("FINISH") }, onHistory = { page = "history"; app.scope.launch { app.perform { app.repository.refresh() } } })
            }
        }
        if (explain) AlertDialog(onDismissRequest = { explain = false },
            title = { Text("Record your route") },
            text = { Text("TrailVault needs precise location access to record your route, including while the screen is off. A recording notification keeps controls available.") },
            confirmButton = { TextButton(onClick = {
                explain = false
                when {
                    LocationPermission.granted(this@MainActivity) -> {
                        continueRecording()
                    }
                    LocationPermission.settingsRequired(this@MainActivity) -> LocationPermission.openSettings(this@MainActivity)
                    else -> { LocationPermission.markRequested(this@MainActivity); location.launch(LocationPermission.permissions) }
                }
            }) { Text(if (LocationPermission.settingsRequired(this@MainActivity)) "Open Settings" else "Continue") } },
            dismissButton = { TextButton(onClick = { explain = false }) { Text("Cancel") } })
        if (busy) AlertDialog(onDismissRequest = {}, title = { Text("Processing GPX…") },
            text = { LinearProgressIndicator(Modifier.fillMaxWidth()) }, confirmButton = {})
        notice?.let { message -> AlertDialog(onDismissRequest = { notice = null }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("OK") } }) }
        error?.let { message -> AlertDialog(onDismissRequest = { app.error.value = null },
            title = { Text("TrailVault") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { app.error.value = null }) { Text("OK") } },
            dismissButton = { if (!LocationPermission.granted(this@MainActivity)) TextButton(onClick = {
                app.error.value = null; LocationPermission.openSettings(this@MainActivity)
            }) { Text("App Settings") } }) }
    }
}
