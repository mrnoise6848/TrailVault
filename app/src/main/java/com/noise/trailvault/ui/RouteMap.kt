package com.noise.trailvault.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noise.trailvault.domain.RoutePoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.CopyrightOverlay

@Composable
fun RouteMap(points: List<RoutePoint>, modifier: Modifier = Modifier, follow: Boolean = true,
    online: Boolean = false) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val map = remember(context) {
        Configuration.getInstance().apply {
            userAgentValue = "TrailVault/1.0 (${context.packageName})"
            osmdroidBasePath = java.io.File(context.cacheDir, "maps")
            osmdroidTileCache = java.io.File(osmdroidBasePath, "tiles")
            tileFileSystemCacheMaxBytes = 64L * 1024 * 1024
            tileFileSystemCacheTrimBytes = 48L * 1024 * 1024
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            setUseDataConnection(false)
        }
    }
    DisposableEffect(map, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) map.onResume()
            if (event == Lifecycle.Event.ON_PAUSE) map.onPause()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) map.onResume()
        onDispose { lifecycle.removeObserver(observer); map.onPause(); map.onDetach() }
    }
    LaunchedEffect(points, follow, online) {
        map.setUseDataConnection(online)
        map.overlays.clear()
        map.overlays.add(CopyrightOverlay(context).apply { setCopyrightNotice("© OpenStreetMap contributors") })
        points.groupBy { it.segment }.values.forEach { segment ->
            if (segment.size > 1) map.overlays.add(Polyline(map).apply {
                setPoints(segment.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.color = android.graphics.Color.rgb(18, 116, 88)
                outlinePaint.strokeWidth = 8f
            })
        }
        points.lastOrNull()?.let { last ->
            map.overlays.add(Marker(map).apply {
                position = GeoPoint(last.latitude, last.longitude)
                title = "Last recorded position"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
            if (follow) map.controller.animateTo(GeoPoint(last.latitude, last.longitude))
            else if (points.size > 1) map.post {
                if (map.width > 0) map.zoomToBoundingBox(BoundingBox.fromGeoPoints(points.map { GeoPoint(it.latitude, it.longitude) }), false, 48)
            }
        }
        map.invalidate()
    }
    AndroidView(factory = { map }, modifier = modifier)
}
