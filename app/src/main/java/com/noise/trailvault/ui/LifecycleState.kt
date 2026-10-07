package com.noise.trailvault.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.StateFlow

/** Reuse runtime-ktx instead of introducing a new Compose lifecycle dependency. */
@Composable
fun <T> StateFlow<T>.collectWhileStarted(): State<T> {
    val lifecycle = (LocalContext.current as LifecycleOwner).lifecycle
    return produceState(value, this, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { collect { value = it } }
    }
}
