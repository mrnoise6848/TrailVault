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
    private val scope: CoroutineScope, private val reportError: (String) -> Unit) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(RecordingSnapshot())
    val snapshot = mutable.asStateFlow()
    private var startedElapsed = 0L
    private var fixStartNanos = 0L
    private var segment = 0
    private var accumulator = StatisticsAccumulator()
    private val listener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) { scope.launch {
            try { accept(location) } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { interrupt("Recording paused: unable to store a GPS point. Check available storage and permissions, then retry.") }
        } }
        override fun onProviderDisabled(provider: String) { mutable.value = mutable.value.copy(message = "GPS is unavailable. Enable location services; your route is retained.") }
        override fun onProviderEnabled(provider: String) { mutable.value = mutable.value.copy(message = null) }
    }
    suspend fun recover() = mutex.withLock {
        if (mutable.value.trail != null) return@withLock
        val recovered = withContext(Dispatchers.IO) { store.active() } ?: return@withLock
        val paused = recovered.copy(state = RecordingState.PAUSED)
        val points = withContext(Dispatchers.IO) { store.save(paused); accumulator = store.accumulator(paused.id); store.recent(paused.id) }
        segment = (points.maxOfOrNull { it.segment } ?: 0) + 1
        mutable.value = RecordingSnapshot(paused, points, accumulator.snapshot(paused.activeMillis), message = "Recovered route is paused. Resume when ready.")
    }
    suspend fun interrupt(message: String) = mutex.withLock {
        manager.removeUpdates(listener)
        val trail = mutable.value.trail
        if (trail != null) {
            val paused = trail.copy(state = RecordingState.PAUSED, activeMillis = elapsed(trail))
            mutable.value = mutable.value.copy(trail = paused, statistics = accumulator.snapshot(paused.activeMillis), message = message)
            try { withContext(Dispatchers.IO) { store.save(paused) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* The prior durable checkpoint and accepted points remain available. */ }
        }
        reportError(message)
    }
    suspend fun checkpoint() = mutex.withLock {
        val trail = mutable.value.trail ?: return@withLock
        val checkpointElapsed = SystemClock.elapsedRealtime()
        val updated = trail.copy(activeMillis = elapsed(trail, checkpointElapsed))
        withContext(Dispatchers.IO) { store.save(updated) }
        startedElapsed = checkpointElapsed
        mutable.value = mutable.value.copy(trail = updated,
            statistics = accumulator.snapshot(updated.activeMillis))
    }
    suspend fun start(activity: ActivityType = ActivityType.WALKING) = mutex.withLock {
        if (mutable.value.trail != null) return@withLock
        check(LocationPermission.granted(context)) { "Precise location access is required." }
        val trail = Trail(name = "Route ${java.time.LocalDate.now()}", activity = activity, state = RecordingState.RECORDING)
        withContext(Dispatchers.IO) { check(store.active() == null) { "Recover the existing route first." }; store.save(trail) }
        segment = 0
        accumulator = StatisticsAccumulator()
        mutable.value = RecordingSnapshot(trail)
        startedElapsed = SystemClock.elapsedRealtime()
        subscribe()
    }
    suspend fun pause() = mutex.withLock {
        val trail = mutable.value.trail ?: return@withLock
        if (trail.state != RecordingState.RECORDING) return@withLock
        manager.removeUpdates(listener)
        val paused = trail.copy(state = RecordingState.PAUSED, activeMillis = elapsed(trail))
        mutable.value = mutable.value.copy(trail = paused, statistics = accumulator.snapshot(paused.activeMillis))
        withContext(Dispatchers.IO) { store.save(paused) }
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
        val paused = trail.copy(state = RecordingState.PAUSED, activeMillis = elapsed(trail))
        mutable.value = mutable.value.copy(trail = paused, statistics = accumulator.snapshot(paused.activeMillis))
        withContext(Dispatchers.IO) { store.complete(paused.copy(state = RecordingState.IDLE,
            endTime = System.currentTimeMillis())) }
        mutable.value = RecordingSnapshot()
    }
    private suspend fun accept(location: Location) = mutex.withLock {
        val trail = mutable.value.trail ?: return@withLock
        if (trail.state != RecordingState.RECORDING || location.elapsedRealtimeNanos < fixStartNanos) return@withLock
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
        accumulator.add(point)
        val points = (mutable.value.points + point).takeLast(6000)
        mutable.value = mutable.value.copy(points = points,
            statistics = accumulator.snapshot(elapsed(trail)), message = null)
    }
    private fun elapsed(trail: Trail, now: Long = SystemClock.elapsedRealtime()) = trail.activeMillis +
        if (trail.state == RecordingState.RECORDING) (now - startedElapsed).coerceAtLeast(0) else 0L
    @SuppressLint("MissingPermission")
    private fun subscribe() {
        fixStartNanos = SystemClock.elapsedRealtimeNanos()
        check(LocationPermission.granted(context)) { "Location permission revoked" }
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            mutable.value = mutable.value.copy(message = "GPS is disabled. Enable location services to receive fixes.")
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 3f, listener, Looper.getMainLooper())
    }
    fun close() { manager.removeUpdates(listener) }
}
