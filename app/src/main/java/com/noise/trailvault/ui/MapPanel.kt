package com.noise.trailvault.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.noise.trailvault.domain.RoutePoint

@Composable
fun MapPanel(points: List<RoutePoint>, follow: Boolean) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("map_privacy", Context.MODE_PRIVATE) }
    var online by remember { mutableStateOf(preferences.getBoolean("online", false)) }
    var explain by remember { mutableStateOf(false) }
    val manager = remember { context.getSystemService(ConnectivityManager::class.java) }
    var connected by remember { mutableStateOf(manager.activeNetwork != null) }
    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { connected = true }
            override fun onLost(network: Network) { connected = manager.activeNetwork != null }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { manager.unregisterNetworkCallback(callback) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Online map tiles", modifier = Modifier.padding(top = 12.dp))
        Switch(checked = online, onCheckedChange = { enabled ->
            if (enabled) explain = true
            else { online = false; preferences.edit().putBoolean("online", false).apply() }
        })
    }
    Text("Route data is stored offline. ${if (!online || !connected) "Only cached map tiles are available; missing tiles appear blank." else "Map tiles need internet and may be unavailable."}",
        style = MaterialTheme.typography.bodySmall)
    RouteMap(points, Modifier.fillMaxWidth().height(300.dp), follow, online && connected)
    if (explain) AlertDialog(onDismissRequest = { explain = false },
        title = { Text("Enable online maps?") },
        text = { Text("OpenStreetMap receives your IP address and the map areas you view when tiles are requested. Your recorded track is kept on this device. Cached tiles may work offline; complete offline maps are not provided.") },
        confirmButton = { TextButton(onClick = {
            online = true; explain = false; preferences.edit().putBoolean("online", true).apply()
        }) { Text("Enable") } },
        dismissButton = { TextButton(onClick = { explain = false }) { Text("Cancel") } })
}
