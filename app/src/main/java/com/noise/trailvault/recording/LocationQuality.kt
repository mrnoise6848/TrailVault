package com.noise.trailvault.recording

import android.location.Location
import android.os.SystemClock
import com.noise.trailvault.domain.RoutePoint

object LocationQuality {
    fun rejection(location: Location, previous: RoutePoint?): String? {
        if (!location.latitude.isFinite() || !location.longitude.isFinite() ||
            location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) return "Invalid GPS coordinates"
        val age = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        if (age < -1_000_000_000L || age > 30_000_000_000L) return "Waiting for a fresh GPS fix"
        if (!location.hasAccuracy() || !location.accuracy.isFinite() || location.accuracy < 0 || location.accuracy > 75f)
            return "Waiting for a more accurate GPS fix"
        if (previous != null) {
            val delta = location.time - previous.timestamp
            if (delta <= 0) return "Waiting for a newer GPS fix"
            val meters = FloatArray(1)
            Location.distanceBetween(previous.latitude, previous.longitude, location.latitude, location.longitude, meters)
            if (delta < 60_000 && meters[0] / (delta / 1000.0) > 70) return "Ignoring an implausible GPS jump"
        }
        return null
    }
}
