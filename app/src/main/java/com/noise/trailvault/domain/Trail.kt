package com.noise.trailvault.domain

import java.util.UUID

enum class ActivityType { WALKING, HIKING, CYCLING }
enum class RecordingState { IDLE, RECORDING, PAUSED }
data class RoutePoint(val latitude: Double, val longitude: Double, val timestamp: Long,
    val altitude: Double? = null, val accuracy: Float? = null, val speed: Float? = null,
    val segment: Int = 0, val verticalAccuracy: Float? = null)
data class RouteSegment(val index: Int, val points: List<RoutePoint>)
data class RouteStatistics(val distanceMeters: Double = 0.0, val durationMillis: Long = 0,
    val averageSpeedKmh: Double = 0.0, val movingMillis: Long? = null,
    val elevationGainMeters: Double? = null)
data class Trail(val id: String = UUID.randomUUID().toString(), val name: String,
    val createdAt: Long = System.currentTimeMillis(), val startTime: Long = createdAt,
    val endTime: Long? = null, val activeMillis: Long = 0,
    val activity: ActivityType = ActivityType.WALKING, val notes: String = "",
    val tags: String = "", val favorite: Boolean = false,
    val state: RecordingState = RecordingState.IDLE)
data class TrackSummary(val trail: Trail, val statistics: RouteStatistics, val pointCount: Int)
data class RecordingSnapshot(val trail: Trail? = null, val points: List<RoutePoint> = emptyList(),
    val statistics: RouteStatistics = RouteStatistics(), val message: String? = null)
