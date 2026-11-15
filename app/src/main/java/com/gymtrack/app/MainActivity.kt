package com.gymtrack.app

import android.os.Bundle
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
