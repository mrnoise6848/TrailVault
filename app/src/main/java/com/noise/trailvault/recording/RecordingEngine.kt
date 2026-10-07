package com.noise.trailvault.recording

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import com.noise.trailvault.data.TrailStore
import com.noise.trailvault.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecordingEngine(private val context: Context, private val store: TrailStore,
    private val scope: CoroutineScope) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(RecordingSnapshot())
    val snapshot = mutable.asStateFlow()
    private var startedElapsed = 0L
    private var segment = 0
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) { scope.launch { accept(location) } }
        override fun onProviderDisabled(provider: String) { mutable.value = mutable.value.copy(message = "GPS is unavailable. Enable location services; your route is retained.") }
        override fun onProviderEnabled(provider: String) { mutable.value = mutable.value.copy(message = null) }
    }
    suspend fun recover() = mutex.withLock {
        if (mutable.value.trail != null) return@withLock
        val recovered = withContext(Dispatchers.IO) { store.active() } ?: return@withLock
        val paused = recovered.copy(state = RecordingState.PAUSED)
        val points = withContext(Dispatchers.IO) { store.save(paused); store.points(paused.id) }
        segment = (points.maxOfOrNull { it.segment } ?: 0) + 1
        mutable.value = RecordingSnapshot(paused, points, Statistics.calculate(points, paused.activeMillis), message = "Recovered route is paused. Resume when ready.")
    }
    suspend fun checkpoint() = mutex.withLock {
        val trail = mutable.value.trail ?: return@withLock
        val updated = trail.copy(activeMillis = elapsed(trail))
        withContext(Dispatchers.IO) { store.save(updated) }
        startedElapsed = SystemClock.elapsedRealtime()
        mutable.value = mutable.value.copy(trail = updated,
            statistics = Statistics.calculate(mutable.value.points, updated.activeMillis))
    }
    suspend fun start(activity: ActivityType = ActivityType.WALKING) = mutex.withLock {
        if (mutable.value.trail != null) return@withLock
        check(LocationPermission.granted(context)) { "Precise location access is required." }
        val trail = Trail(name = "Route ${java.time.LocalDate.now()}", activity = activity, state = RecordingState.RECORDING)
        withContext(Dispatchers.IO) { check(store.active() == null) { "Recover the existing route first." }; store.save(trail) }
        segment = 0
        mutable.value = RecordingSnapshot(trail)
        startedElapsed = SystemClock.elapsedRealtime()
        subscribe()
    }
    suspend fun pause() = mutex.withLock {
        val trail = mutable.value.trail ?: return@withLock
        if (trail.state != RecordingState.RECORDING) return@withLock
        manager.removeUpdates(listener)
        val paused = trail.copy(state = RecordingState.PAUSED, activeMillis = elapsed(trail))
        withContext(Dispatchers.IO) { store.save(paused) }
        mutable.value = mutable.value.copy(trail = paused)
    }
    suspend fun resume() = mutex.withLock {
        val trail = mutable.value.trail ?: return@withLock
        if (trail.state != RecordingState.PAUSED) return@withLock
        check(LocationPermission.granted(context)) { "Restore precise location permission before resuming." }
        val resumed = trail.copy(state = RecordingState.RECORDING)
        withContext(Dispatchers.IO) { store.save(resumed) }
        segment++
        startedElapsed = SystemClock.elapsedRealtime()
        mutable.value = mutable.value.copy(trail = resumed, message = null)
        subscribe()
    }
    suspend fun finish() = mutex.withLock {
        val trail = mutable.value.trail ?: return@withLock
        manager.removeUpdates(listener)
        withContext(Dispatchers.IO) { store.save(trail.copy(state = RecordingState.IDLE,
            activeMillis = elapsed(trail), endTime = System.currentTimeMillis())) }
        mutable.value = RecordingSnapshot()
    }
    private suspend fun accept(location: Location) = mutex.withLock {
        val trail = mutable.value.trail ?: return@withLock
        if (trail.state != RecordingState.RECORDING) return@withLock
        val previous = mutable.value.points.lastOrNull()?.takeIf { it.segment == segment }
        val rejection = LocationQuality.rejection(location, previous)
        if (rejection != null) {
            mutable.value = mutable.value.copy(message = rejection)
            return@withLock
        }
        if (previous != null && location.time - previous.timestamp > 60_000) segment++
        val point = RoutePoint(location.latitude, location.longitude, location.time,
            if (location.hasAltitude() && location.altitude.isFinite()) location.altitude else null,
            if (location.hasAccuracy()) location.accuracy else null,
            if (location.hasSpeed() && location.speed.isFinite() && location.speed >= 0) location.speed else null, segment,
            if (location.hasVerticalAccuracy() && location.verticalAccuracyMeters.isFinite() && location.verticalAccuracyMeters >= 0) location.verticalAccuracyMeters else null)
        withContext(Dispatchers.IO) { store.append(trail.id, point) }
        val points = mutable.value.points + point
        mutable.value = mutable.value.copy(points = points,
            statistics = Statistics.calculate(points, elapsed(trail)), message = null)
    }
    private fun elapsed(trail: Trail) = trail.activeMillis +
        if (trail.state == RecordingState.RECORDING) (SystemClock.elapsedRealtime() - startedElapsed).coerceAtLeast(0) else 0L
    @SuppressLint("MissingPermission")
    private fun subscribe() {
        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 3f, listener, Looper.getMainLooper())
        } catch (error: Exception) {
            manager.removeUpdates(listener)
            mutable.value = mutable.value.copy(message = "Location unavailable: ${error.javaClass.simpleName}. Pause or enable GPS.")
        }
    }
    fun close() { manager.removeUpdates(listener) }
}
