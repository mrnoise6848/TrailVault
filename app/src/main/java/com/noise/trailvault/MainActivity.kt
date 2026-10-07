package com.noise.trailvault

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
    @Composable
    private fun TrailRoot() {
        val app = application as TrailApplication
        val snapshot by app.engine.snapshot.collectAsState()
        val history by app.repository.history.collectAsState()
        var page by rememberSaveable { mutableStateOf("recording") }
        var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
        val error by app.error.collectAsState()
        var explain by remember { mutableStateOf(false) }
        var requestedActivity by remember { mutableStateOf(ActivityType.WALKING) }
        val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        fun command(action: String) {
            try {
                val intent = Intent(this@MainActivity, RecordingService::class.java).setAction(action)
                    .putExtra("activity", requestedActivity.name)
                startForegroundService(intent)
            } catch (exception: Exception) { app.error.value = "Unable to start recording. Check location permission and try again." }
        }
        val location = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (LocationPermission.granted(this@MainActivity)) command("START")
            else app.error.value = "Precise location is required to record a route. You can enable it in app settings."
        }
        Scaffold { padding ->
            Box(Modifier.padding(padding)) {
                if (page == "history") HistoryScreen(history, onBack = { page = "recording" },
                    onOpen = { selectedId = it; page = "detail" },
                    onUpdate = { app.scope.launch { app.perform { app.repository.update(it) } } },
                    onDelete = { app.scope.launch { app.perform { app.repository.delete(it) } } })
                else RecordingScreen(snapshot,
                    onStart = { requestedActivity = it; explain = true },
                    onPause = { command("PAUSE") }, onResume = { command("RESUME") },
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
                        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        command("START")
                    }
                    LocationPermission.settingsRequired(this@MainActivity) -> LocationPermission.openSettings(this@MainActivity)
                    else -> { LocationPermission.markRequested(this@MainActivity); location.launch(LocationPermission.permissions) }
                }
            }) { Text(if (LocationPermission.settingsRequired(this@MainActivity)) "Open Settings" else "Continue") } },
            dismissButton = { TextButton(onClick = { explain = false }) { Text("Cancel") } })
        error?.let { message -> AlertDialog(onDismissRequest = { app.error.value = null },
            title = { Text("TrailVault") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { app.error.value = null }) { Text("OK") } }) }
    }
}
