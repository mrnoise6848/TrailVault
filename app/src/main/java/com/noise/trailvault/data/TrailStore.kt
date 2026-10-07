package com.noise.trailvault.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.noise.trailvault.domain.*

/** Access only from the repository IO dispatcher. */
class TrailStore(context: Context) : SQLiteOpenHelper(context, "trails.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE trails(id TEXT PRIMARY KEY, name TEXT NOT NULL, created INTEGER NOT NULL, start INTEGER NOT NULL, end INTEGER, active INTEGER NOT NULL, activity TEXT NOT NULL, notes TEXT NOT NULL, tags TEXT NOT NULL, favorite INTEGER NOT NULL, state TEXT NOT NULL)")
        db.execSQL("CREATE TABLE points(id INTEGER PRIMARY KEY, trail TEXT NOT NULL REFERENCES trails(id) ON DELETE CASCADE, lat REAL NOT NULL, lon REAL NOT NULL, time INTEGER NOT NULL, altitude REAL, accuracy REAL, speed REAL, segment INTEGER NOT NULL, vertical REAL)")
        db.execSQL("CREATE INDEX point_route ON points(trail,id)")
        createStatistics(db)
    }
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true); db.enableWriteAheadLogging() }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val hasVertical = db.rawQuery("PRAGMA table_info(points)", null).use { c ->
                var found = false
                while (c.moveToNext()) if (c.getString(1) == "vertical") found = true
                found
            }
            if (!hasVertical) db.execSQL("ALTER TABLE points ADD COLUMN vertical REAL")
            createStatistics(db)
        }
    }
    private fun createStatistics(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS statistics(trail TEXT PRIMARY KEY REFERENCES trails(id) ON DELETE CASCADE, distance REAL NOT NULL, duration INTEGER NOT NULL, average REAL NOT NULL, moving INTEGER, elevation REAL, count INTEGER NOT NULL)")
    }
    fun complete(trail: Trail, points: List<RoutePoint>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            save(trail)
            val stats = Statistics.calculate(points, trail.activeMillis)
            db.insertWithOnConflict("statistics", null, ContentValues().apply {
                put("trail", trail.id); put("distance", stats.distanceMeters); put("duration", stats.durationMillis)
                put("average", stats.averageSpeedKmh); put("moving", stats.movingMillis)
                put("elevation", stats.elevationGainMeters); put("count", points.size)
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun trail(id: String): Trail? = readableDatabase.query("trails", null, "id=?", arrayOf(id), null, null, null).use {
        if (it.moveToFirst()) readTrail(it) else null
    }
    fun history(): List<TrackSummary> = readableDatabase.rawQuery(
        "SELECT t.*, s.distance, s.duration, s.average, s.moving, s.elevation, s.count FROM trails t LEFT JOIN statistics s ON t.id=s.trail WHERE t.state='IDLE' ORDER BY t.start DESC", null).use { c ->
        buildList { while (c.moveToNext()) {
            val trail = readTrail(c)
            if (c.isNull(c.getColumnIndexOrThrow("distance"))) {
                val points = points(trail.id)
                add(TrackSummary(trail, Statistics.calculate(points, trail.activeMillis), points.size))
            } else add(TrackSummary(trail, RouteStatistics(c.double("distance"), c.long("duration"), c.double("average"),
                c.optional("moving")?.toLong(), c.optional("elevation")), c.int("count")))
        } }
    }
    fun delete(id: String) { writableDatabase.delete("trails", "id=? AND state='IDLE'", arrayOf(id)) }
    fun importRoute(trail: Trail, points: List<RoutePoint>) {
        val db = writableDatabase
        db.beginTransaction()
        try { save(trail); points.forEach { append(trail.id, it) }; complete(trail, points); db.setTransactionSuccessful() }
        finally { db.endTransaction() }
    }
    fun save(trail: Trail) {
        val values = ContentValues().apply {
            put("id", trail.id); put("name", trail.name); put("created", trail.createdAt)
            put("start", trail.startTime); put("end", trail.endTime); put("active", trail.activeMillis)
            put("activity", trail.activity.name); put("notes", trail.notes); put("tags", trail.tags)
            put("favorite", if (trail.favorite) 1 else 0); put("state", trail.state.name)
        }
        if (writableDatabase.update("trails", values, "id=?", arrayOf(trail.id)) == 0)
            writableDatabase.insertOrThrow("trails", null, values)
    }
    fun append(id: String, point: RoutePoint) {
        writableDatabase.insertOrThrow("points", null, ContentValues().apply {
            put("trail", id); put("lat", point.latitude); put("lon", point.longitude)
            put("time", point.timestamp); put("altitude", point.altitude); put("accuracy", point.accuracy)
            put("speed", point.speed); put("segment", point.segment); put("vertical", point.verticalAccuracy)
        })
    }
    fun active(): Trail? = readableDatabase.rawQuery("SELECT * FROM trails WHERE state != 'IDLE' LIMIT 1", null).use {
        if (it.moveToFirst()) readTrail(it) else null
    }
    fun points(id: String): List<RoutePoint> = readableDatabase.query("points", null, "trail=?", arrayOf(id), null, null, "id").use { c ->
        buildList { while (c.moveToNext()) add(RoutePoint(c.double("lat"), c.double("lon"), c.long("time"),
            c.optional("altitude"), c.optional("accuracy")?.toFloat(), c.optional("speed")?.toFloat(), c.int("segment"), c.optional("vertical")?.toFloat())) }
    }
    private fun readTrail(c: Cursor) = Trail(c.string("id"), c.string("name"), c.long("created"), c.long("start"),
        c.optional("end")?.toLong(), c.long("active"), ActivityType.valueOf(c.string("activity")),
        c.string("notes"), c.string("tags"), c.int("favorite") != 0, RecordingState.valueOf(c.string("state")))
    private fun Cursor.string(key: String) = getString(getColumnIndexOrThrow(key))
    private fun Cursor.long(key: String) = getLong(getColumnIndexOrThrow(key))
    private fun Cursor.int(key: String) = getInt(getColumnIndexOrThrow(key))
    private fun Cursor.double(key: String) = getDouble(getColumnIndexOrThrow(key))
    private fun Cursor.optional(key: String): Double? = getColumnIndexOrThrow(key).let { if (isNull(it)) null else getDouble(it) }
}
