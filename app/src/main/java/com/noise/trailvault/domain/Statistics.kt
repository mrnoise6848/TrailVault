package com.noise.trailvault.domain

import android.location.Location

object Statistics {
    fun calculate(points: List<RoutePoint>, durationMillis: Long): RouteStatistics {
        var distance = 0.0
        var moving = 0L
        var hasSpeed = false
        var gain = 0.0
        var altitudeAnchor: Double? = null
        var altitudeSegment = -1
        var reliableAltitudeCount = 0
        val result = FloatArray(1)
        points.forEachIndexed { index, point ->
            val previous = points.getOrNull(index - 1)
            if (previous != null && previous.segment == point.segment) {
                Location.distanceBetween(previous.latitude, previous.longitude, point.latitude, point.longitude, result)
                distance += result[0]
                val delta = point.timestamp - previous.timestamp
                if (delta in 1..60_000 && point.speed != null) {
                    hasSpeed = true
                    if (point.speed >= 0.8f) moving += delta
                }
            }
            if (point.altitude != null && point.verticalAccuracy != null && point.verticalAccuracy <= 10f) {
                reliableAltitudeCount++
                if (altitudeSegment != point.segment) { altitudeAnchor = point.altitude; altitudeSegment = point.segment }
                val anchor = altitudeAnchor ?: point.altitude
                if (kotlin.math.abs(point.altitude - anchor) >= 5) {
                    if (point.altitude > anchor) gain += point.altitude - anchor
                    altitudeAnchor = point.altitude
                }
            } else { altitudeAnchor = null; altitudeSegment = -1 }
        }
        return RouteStatistics(distance, durationMillis.coerceAtLeast(0),
            if (durationMillis > 0) distance / durationMillis * 3600 else 0.0,
            if (hasSpeed) moving.coerceAtMost(durationMillis.coerceAtLeast(0)) else null,
            if (reliableAltitudeCount >= 2) gain else null)
    }
}
