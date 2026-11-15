package com.gymtrack.app.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.gymtrack.app.data.db.SessionEntity
import com.gymtrack.app.notify.RestTimerScheduler
import com.gymtrack.app.ui.Routes
import com.gymtrack.app.ui.components.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Returns a function that starts a workout (routine id or null for free workout), handling an already active session. */
@Composable
fun rememberStartWorkout(nav: NavController): (Long?) -> Unit {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    var conflict by remember { mutableStateOf<Pair<SessionEntity, Long?>?>(null) }
    conflict?.let { (active, wanted) ->
        AlertDialog(
            onDismissRequest = { conflict = null },
            title = { Text("Treino em andamento") },
            text = { Text("\"${active.name}\" ainda está ativo. Deseja continuar esse treino ou descartá-lo e iniciar outro?") },
            confirmButton = { TextButton(onClick = { conflict = null; nav.navigate(Routes.WORKOUT) }) { Text("Continuar atual") } },
            dismissButton = {
                TextButton(onClick = {
                    conflict = null
                    scope.launch(Dispatchers.Main) {
                        RestTimerScheduler.cancel(c.appContext)
                        c.workouts.deleteSession(active.id)
                        c.workouts.startSession(wanted)
                        nav.navigate(Routes.WORKOUT)
                    }
                }) { Text("Descartar e iniciar", color = MaterialTheme.colorScheme.error) }
            },
        )
    }
    return { routineId ->
        scope.launch(Dispatchers.Main) {
            val active = c.workouts.getActiveSession()
            if (active != null) {
                if (active.routineId == routineId && routineId != null) nav.navigate(Routes.WORKOUT) else conflict = active to routineId
            } else {
                c.workouts.startSession(routineId)
                nav.navigate(Routes.WORKOUT)
            }
        }
    }
}
