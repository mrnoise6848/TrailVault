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
        engine = RecordingEngine(this, store, scope)
        scope.launch { perform { engine.recover(); repository.refresh() } }
    }
    suspend fun perform(action: suspend () -> Unit): Boolean = try {
        action(); true
    } catch (cancelled: CancellationException) { throw cancelled
    } catch (exception: Exception) {
        error.value = "Operation failed (${exception.javaClass.simpleName}). Your saved data is retained. Retry after checking permissions and available storage."
        false
    }
}
