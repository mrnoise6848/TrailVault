package com.noise.trailvault.data

import android.util.Xml
import com.noise.trailvault.domain.*
import java.io.OutputStream
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
            element("time", Instant.ofEpochMilli(point.timestamp).toString())
            xml.endTag(NS, "trkpt")
        }
        if (segment != null) xml.endTag(NS, "trkseg")
        xml.endTag(NS, "trk"); xml.endTag(NS, "gpx"); xml.endDocument(); xml.flush()
    }
}
