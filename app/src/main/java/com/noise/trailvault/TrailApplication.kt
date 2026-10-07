package com.noise.trailvault

import android.app.Application
import com.noise.trailvault.data.TrailStore
import com.noise.trailvault.data.TrailRepository
import com.noise.trailvault.recording.RecordingEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class TrailApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val error = MutableStateFlow<String?>(null)
    lateinit var store: TrailStore
        private set
    lateinit var repository: TrailRepository
        private set
    lateinit var engine: RecordingEngine
        private set
    override fun onCreate() {
        super.onCreate()
        store = TrailStore(this)
        repository = TrailRepository(store)
        engine = RecordingEngine(this, store, scope) { error.value = it }
        scope.launch { perform { engine.recover(); repository.refresh() } }
    }
    suspend fun perform(action: suspend () -> Unit): Boolean = try {
        action(); true
    } catch (cancelled: CancellationException) { throw cancelled
    } catch (exception: Exception) {
        error.value = when (exception) {
            is android.database.sqlite.SQLiteFullException -> "Storage is full. Free space and retry; the prior saved route is retained."
            is SecurityException -> "Access was denied. Restore location permission or choose an accessible document, then retry."
            is IllegalArgumentException, is org.xmlpull.v1.XmlPullParserException, is java.time.format.DateTimeParseException -> "Invalid or unsupported GPX/data. Check the selected file and try again."
            is java.io.IOException -> "Unable to read or write the document. Check the destination and available storage, then retry."
            else -> "Operation failed (${exception.javaClass.simpleName}). Your saved data is retained. Check permissions and available storage, then retry."
        }
        false
    }
}
