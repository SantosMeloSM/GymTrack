package com.gymtrack.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import com.gymtrack.app.data.repo.ThemeMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.gymtrack.app.data.repo.AppSettings
import com.gymtrack.app.notify.AppVisibility
import com.gymtrack.app.notify.WorkoutNotification
import com.gymtrack.app.ui.GymTrackRoot
import com.gymtrack.app.ui.theme.GymTrackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GymTrack)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GymTrackApp).container
        setContent {
            val settings by container.settings.settings.collectAsState(initial = AppSettings())
            val dark = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // System bars follow the app theme (not only the system setting).
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            GymTrackTheme(settings.themeMode) { GymTrackRoot(settings) }
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.foreground = true
        WorkoutNotification.cancel(this)
    }

    override fun onStop() {
        super.onStop()
        AppVisibility.foreground = false
        val app = applicationContext
        lifecycleScope.launch { WorkoutNotification.refresh(app) }
    }
}
