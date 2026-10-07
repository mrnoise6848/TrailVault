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
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!LocationPermission.granted(this)) { stopSelf(); return START_NOT_STICKY }
        startForeground(7, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        scope.launch {
            val ok = app.perform {
                app.engine.recover()
                when (intent?.action) {
                    "START" -> app.engine.start(runCatching { ActivityType.valueOf(intent.getStringExtra("activity") ?: "WALKING") }.getOrDefault(ActivityType.WALKING))
                    "PAUSE" -> app.engine.pause()
                    "RESUME" -> app.engine.resume()
                    "FINISH" -> app.engine.finish()
                }
            }
            if (!ok || app.engine.snapshot.value.trail == null) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
        }
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, "recording").setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("TrailVault").setContentText("Route recording active")
            .setContentIntent(open).setOngoing(true).build()
    }
    override fun onDestroy() { app.engine.close(); scope.cancel(); super.onDestroy() }
}
