package com.noise.trailvault.data

import android.util.Xml
import com.noise.trailvault.domain.*
import java.io.OutputStream
import java.io.InputStream
import java.io.FilterInputStream
import org.xmlpull.v1.XmlPullParser
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Gpx {
    private const val NS = "http://www.topografix.com/GPX/1/1"
    private const val TV = "https://trailvault.app/gpx/1"
    fun filename(trail: Trail): String {
        val name = trail.name.replace(Regex("[^\\p{L}\\p{N}._-]+"), "_").trim('_', '.').take(80).ifBlank { "Route" }
        val date = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(trail.startTime))
        return "${name}_${date}.gpx"
    }
    fun write(output: OutputStream, trail: Trail, points: List<RoutePoint>) {
        require(points.isNotEmpty()) { "Route contains no GPS points" }
        val xml = Xml.newSerializer()
        xml.setOutput(output, "UTF-8")
        xml.startDocument("UTF-8", true)
        xml.setPrefix("", NS)
        xml.setPrefix("tv", TV)
        fun element(name: String, text: String) { xml.startTag(NS, name); xml.text(text); xml.endTag(NS, name) }
        xml.startTag(NS, "gpx").attribute(null, "version", "1.1").attribute(null, "creator", "TrailVault")
        xml.startTag(NS, "metadata")
        element("name", trail.name)
        element("time", Instant.ofEpochMilli(trail.createdAt).toString())
        xml.endTag(NS, "metadata")
        xml.startTag(NS, "trk")
        element("name", trail.name)
        if (trail.notes.isNotBlank()) element("desc", trail.notes)
        element("type", trail.activity.name)
        xml.startTag(NS, "extensions")
        xml.startTag(TV, "activeMillis").text(trail.activeMillis.toString()).endTag(TV, "activeMillis")
        if (trail.tags.isNotBlank()) xml.startTag(TV, "tags").text(trail.tags).endTag(TV, "tags")
        xml.endTag(NS, "extensions")
        var segment: Int? = null
        points.forEach { point ->
            if (segment != point.segment) {
                if (segment != null) xml.endTag(NS, "trkseg")
                xml.startTag(NS, "trkseg")
                segment = point.segment
            }
            xml.startTag(NS, "trkpt").attribute(null, "lat", point.latitude.toString()).attribute(null, "lon", point.longitude.toString())
            point.altitude?.let { element("ele", it.toString()) }
            if (point.timestamp > 0) element("time", Instant.ofEpochMilli(point.timestamp).toString())
            xml.endTag(NS, "trkpt")
        }
        if (segment != null) xml.endTag(NS, "trkseg")
        xml.endTag(NS, "trk"); xml.endTag(NS, "gpx"); xml.endDocument(); xml.flush()
    }
    data class Imported(val trail: Trail, val points: List<RoutePoint>)
    private class LimitedInput(input: InputStream) : FilterInputStream(input) {
        private var count = 0L
        private fun add(size: Int) { if (size > 0) count += size; require(count <= 16L * 1024 * 1024) { "GPX exceeds 16 MiB" } }
        override fun read(): Int = `in`.read().also { if (it >= 0) add(1) }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = `in`.read(buffer, offset, length).also(::add)
    }
    fun read(input: InputStream): Imported {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(LimitedInput(input), null)
        val stack = java.util.ArrayDeque<String>()
        val text = StringBuilder()
        val points = ArrayList<RoutePoint>()
        var rootSeen = false
        var segment = -1
        var pointDepth = -1
        var latitude = 0.0
        var longitude = 0.0
        var altitude: Double? = null
        var timestamp = 0L
        var name = "Imported route"
        var notes = ""
        var tags = ""
        var activity = ActivityType.WALKING
        var active: Long? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.DOCDECL -> error("Document declarations are not supported")
                XmlPullParser.START_TAG -> {
                    if (!rootSeen) {
                        require(parser.name == "gpx" && parser.namespace in listOf("", NS, "http://www.topografix.com/GPX/1/0")) { "Not a GPX document" }
                        rootSeen = true
                    }
                    require(parser.depth <= 32) { "GPX nesting is too deep" }
                    stack.addLast(parser.name); text.setLength(0)
                    if (parser.name == "trkseg" || parser.name == "rte") segment++
                    if (parser.name == "trkpt" || parser.name == "rtept") {
                        require(pointDepth == -1) { "Nested route point" }
                        latitude = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: error("Missing latitude")
                        longitude = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: error("Missing longitude")
                        require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0) { "Invalid coordinates" }
                        if (segment < 0) segment = 0
                        altitude = null; timestamp = 0; pointDepth = parser.depth
                    }
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> {
                    val value = parser.text ?: error("Unsupported XML entity")
                    require(text.length + value.length <= 8192) { "GPX text field is too large" }
                    text.append(value)
                }
                XmlPullParser.END_TAG -> {
                    val value = text.toString().trim()
                    val parent = stack.toList().dropLast(1).lastOrNull()
                    if (pointDepth > 0 && parser.depth == pointDepth + 1) {
                        if (parser.name == "ele") {
                            altitude = value.toDoubleOrNull() ?: error("Invalid altitude")
                            require(altitude!!.isFinite()) { "Invalid altitude" }
                        }
                        if (parser.name == "time") {
                            timestamp = Instant.parse(value).toEpochMilli()
                            require(timestamp > 0) { "Unsupported point timestamp" }
                        }
                    }
                    if (parser.name in listOf("trkpt", "rtept") && parser.depth == pointDepth) {
                        require(points.size < 100_000) { "GPX exceeds 100,000 points" }
                        points.add(RoutePoint(latitude, longitude, timestamp, altitude, segment = segment))
                        pointDepth = -1
                    } else if (pointDepth == -1) {
                        if (parser.name == "name" && parent in listOf("trk", "rte", "metadata") && value.isNotBlank() && name == "Imported route") name = value.take(160)
                        if (parser.name == "desc" && parent in listOf("trk", "rte")) notes = value.take(4000)
                        if (parser.name == "type") activity = ActivityType.entries.firstOrNull { it.name.equals(value, true) } ?: activity
                        if (parser.namespace == TV && parser.name == "tags") tags = value.take(500)
                        if (parser.namespace == TV && parser.name == "activeMillis") active = value.toLongOrNull()?.takeIf { it >= 0 }
                    }
                    stack.removeLast(); text.setLength(0)
                }
            }
            event = parser.nextToken()
        }
        require(rootSeen && points.isNotEmpty()) { "GPX contains no track or route points" }
        var duration = 0L
        points.zipWithNext().forEach { (a, b) ->
            if (a.segment == b.segment && a.timestamp > 0 && b.timestamp > a.timestamp)
                duration += b.timestamp - a.timestamp
        }
        val first = points.filter { it.timestamp > 0 }.minOfOrNull { it.timestamp }
        val last = points.filter { it.timestamp > 0 }.maxOfOrNull { it.timestamp }
        val imported = Trail(name = name, startTime = first ?: System.currentTimeMillis(), endTime = last,
            activeMillis = active?.coerceAtMost(duration) ?: duration, activity = activity, notes = notes, tags = tags)
        return Imported(imported, points)
    }

}
