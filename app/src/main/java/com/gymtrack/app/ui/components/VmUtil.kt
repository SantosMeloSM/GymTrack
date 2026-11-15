package com.gymtrack.app.ui.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.isActive
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymtrack.app.AppContainer
import com.gymtrack.app.GymTrackApp

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as GymTrackApp).container

@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val c = appContainer()
    return viewModel(key = key, factory = viewModelFactory { initializer { create(c) } })
}

/**
 * Wall-clock ticker for timers. Runs on a background dispatcher with real time, so it never
 * blocks UI idleness (and doesn't spin in tests' virtual time).
 */
@Composable
fun rememberNow(periodMs: Long = 500, enabled: Boolean = true): androidx.compose.runtime.State<Long> =
    androidx.compose.runtime.produceState(System.currentTimeMillis(), enabled) {
        if (!enabled) return@produceState
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            while (isActive) {
                value = System.currentTimeMillis()
                kotlinx.coroutines.delay(periodMs)
            }
        }
    }
