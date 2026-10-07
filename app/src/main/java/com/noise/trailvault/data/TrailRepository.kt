package com.noise.trailvault.data

import com.noise.trailvault.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class TrailRepository(private val store: TrailStore) {
    private val mutable = MutableStateFlow<List<TrackSummary>>(emptyList())
    val history = mutable.asStateFlow()
    suspend fun refresh() { mutable.value = withContext(Dispatchers.IO) { store.history() } }
    suspend fun detail(id: String): Pair<Trail, List<RoutePoint>> = withContext(Dispatchers.IO) {
        val trail = requireNotNull(store.trail(id)) { "Route no longer exists" }
        trail to store.preview(id)
    }
    suspend fun export(id: String, output: java.io.OutputStream) = withContext(Dispatchers.IO) {
        val trail = requireNotNull(store.trail(id))
        store.withPoints(id) { Gpx.write(output, trail, it) }
    }
    suspend fun update(trail: Trail) {
        require(trail.name.isNotBlank() && trail.name.length <= 160)
        require(trail.notes.length <= 4000 && trail.tags.length <= 500)
        withContext(Dispatchers.IO) { store.save(trail.copy(name = trail.name.trim())) }
        refresh()
    }
    suspend fun delete(id: String) { withContext(Dispatchers.IO) { store.delete(id) }; refresh() }
    suspend fun importRoute(trail: Trail, points: List<RoutePoint>) {
        withContext(Dispatchers.IO) { store.importRoute(trail, points) }; refresh()
    }
}
