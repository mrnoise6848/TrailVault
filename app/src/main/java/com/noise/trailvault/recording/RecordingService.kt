package com.noise.trailvault.recording

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.noise.trailvault.TrailApplication
import com.noise.trailvault.MainActivity
import com.noise.trailvault.domain.*
import kotlinx.coroutines.*

class RecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val app get() = application as TrailApplication
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("recording", "Route recording", NotificationManager.IMPORTANCE_LOW))
        scope.launch {
            while (isActive) {
                delay(5000)
                if (app.engine.snapshot.value.trail != null) {
                    if (!LocationPermission.granted(this@RecordingService)) {
                        app.engine.interrupt("Location permission was revoked. Route paused; restore access in app settings.")
                        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); break
                    }
                    if (!app.perform { app.engine.checkpoint() }) {
                        app.engine.interrupt("Recording paused because the checkpoint could not be saved. Free storage and retry.")
                        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); break
                    }
                    getSystemService(NotificationManager::class.java).notify(7, notification())
                }
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!LocationPermission.granted(this)) {
            app.scope.launch {
                app.perform {
                    app.engine.recover()
                    app.engine.pause()
                    if (intent?.action == "FINISH") { app.engine.finish(); app.repository.refresh() }
                }
                app.error.value = "Precise location permission is unavailable. Your route is retained. Restore access in app settings to resume."
            }
            stopSelf(); return START_NOT_STICKY
        }
        try { startForeground(7, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) }
        catch (error: Exception) {
            app.scope.launch { app.engine.interrupt("Unable to start background recording. Reopen TrailVault and restore location access.") }
            stopSelf(); return START_NOT_STICKY
        }
        scope.launch {
            val ok = app.perform {
                app.engine.recover()
                when (intent?.action) {
                    "START" -> app.engine.start(runCatching { ActivityType.valueOf(intent.getStringExtra("activity") ?: "WALKING") }.getOrDefault(ActivityType.WALKING))
                    "PAUSE" -> app.engine.pause()
                    "RESUME" -> app.engine.resume()
                    "FINISH" -> { app.engine.finish(); app.repository.refresh() }
                }
            }
            if (!ok) app.engine.interrupt("Recording paused after an operation failed. Your route is retained; check storage and permissions.")
            if (!ok || app.engine.snapshot.value.trail == null) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
        }
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val snapshot = app.engine.snapshot.value
        val paused = snapshot.trail?.state == RecordingState.PAUSED
        val pause = PendingIntent.getService(this, 1, Intent(this, RecordingService::class.java).setAction("PAUSE"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, "recording").setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("TrailVault").setContentText(
                "${if (paused) "Paused" else "Recording"} · %.2f km".format(snapshot.statistics.distanceMeters / 1000))
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, if (paused) "Open to resume" else "Pause", if (paused) open else pause).build())
            .setContentIntent(open).setOngoing(true).build()
    }
    override fun onDestroy() {
        app.engine.close()
        scope.cancel()
        if (app.engine.snapshot.value.trail?.state == RecordingState.RECORDING)
            app.scope.launch { app.perform { app.engine.pause() } }
        super.onDestroy()
    }
}
