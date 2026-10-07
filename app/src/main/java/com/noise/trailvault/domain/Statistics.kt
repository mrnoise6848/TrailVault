package com.noise.trailvault.domain

import android.location.Location

/** Constant-memory accumulator shared by live recording and streamed database statistics. */
class StatisticsAccumulator {
    private var previous: RoutePoint? = null
    private var distance = 0.0
    private var moving = 0L
    private var hasSpeed = false
    private var gain = 0.0
    private var altitudeAnchor: Double? = null
    private var altitudeSegment = -1
    private var reliableAltitudeCount = 0
    private val result = FloatArray(1)
    var count = 0
        private set
    fun add(point: RoutePoint) {
        count++
        val last = previous
        if (last != null && last.segment == point.segment) {
            Location.distanceBetween(last.latitude, last.longitude, point.latitude, point.longitude, result)
            distance += result[0]
            val delta = point.timestamp - last.timestamp
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
        previous = point
    }
    fun snapshot(duration: Long) = RouteStatistics(distance, duration.coerceAtLeast(0),
        if (duration > 0) distance / duration * 3600 else 0.0,
        if (hasSpeed) moving.coerceAtMost(duration.coerceAtLeast(0)) else null,
        if (reliableAltitudeCount >= 2) gain else null)
}
object Statistics {
    fun calculate(points: List<RoutePoint>, durationMillis: Long): RouteStatistics =
        StatisticsAccumulator().apply { points.forEach(::add) }.snapshot(durationMillis)
}
