package com.gymtrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.gymtrack.app.AppContainer
import com.gymtrack.app.data.db.RoutineSummary
import com.gymtrack.app.data.db.SessionSummary
import com.gymtrack.app.data.repo.AppSettings
import com.gymtrack.app.ui.Routes
import com.gymtrack.app.ui.components.BigButton
import com.gymtrack.app.ui.components.GtCard
import com.gymtrack.app.ui.components.GtScaffold
import com.gymtrack.app.ui.components.IconBadge
import com.gymtrack.app.ui.components.SectionHeader
import com.gymtrack.app.ui.components.StatRow
import com.gymtrack.app.ui.components.StatTile
import com.gymtrack.app.ui.components.appViewModel
import com.gymtrack.app.ui.components.fmtDayMonth
import com.gymtrack.app.ui.components.fmtWeekday
import com.gymtrack.domain.Consistency
import com.gymtrack.domain.Format
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(c: AppContainer) : ViewModel() {
    val active = c.workouts.observeActiveSession().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val routines = c.workouts.observeRoutines().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val sessions = c.workouts.observeFinishedSessions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
}

/** Suggests the routine of the current program that was performed least recently. */
fun suggestNext(routines: List<RoutineSummary>, sessions: List<SessionSummary>): RoutineSummary? {
    if (routines.isEmpty()) return null
    val lastRoutineId = sessions.firstOrNull { it.routineId != null }?.routineId
    val program = routines.firstOrNull { it.id == lastRoutineId }?.program
    val pool = routines.filter { program == null || it.program == program }.ifEmpty { routines }
    return pool.minWithOrNull(compareBy<RoutineSummary> { it.lastPerformed ?: 0L }.thenBy { it.position })
}

@Composable
fun HomeScreen(nav: NavController) {
    val vm = appViewModel { HomeViewModel(it) }
    val active by vm.active.collectAsState()
    val routines by vm.routines.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val settings by vm.settings.collectAsState()
    val start = rememberStartWorkout(nav)
    val now by com.gymtrack.app.ui.components.rememberNow(1000, enabled = active != null)

    val today = LocalDate.now()
    val consistency = remember { Consistency() }
    val dates = sessions.map { it.startedAt }
    val weekCount = consistency.weeklyCounts(dates, today, 1).first().second
    val streak = consistency.weekStreak(dates, today, settings.weeklyTarget)
    val weekStart = consistency.weekStart(today)
    val trainedThisWeek = dates.map { consistency.toDate(it) }.filter { !it.isBefore(weekStart) }.map { it.dayOfWeek.value }.toSet()
    val suggestion = routines?.let { suggestNext(it, sessions) }

    GtScaffold(title = "GymTrack", isTab = true, actions = {
        IconButton(onClick = { nav.navigate(Routes.HISTORY) }) { Icon(Icons.Outlined.History, "Histórico") }
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(fmtWeekday(System.currentTimeMillis()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            active?.let { s ->
                item {
                    GtCard(onClick = { nav.navigate(Routes.WORKOUT) }, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.testTag("resume_card")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("TREINO EM ANDAMENTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(s.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(if (s.pausedAt != null) "Pausado · ${Format.duration(s.elapsedMs(now))}" else Format.duration(s.elapsedMs(now)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            IconBadge(Icons.Filled.PlayArrow, tint = MaterialTheme.colorScheme.onPrimary, bg = MaterialTheme.colorScheme.primary, size = 52)
                        }
                    }
                }
            }

            if (active == null) item {
                GtCard {
                    if (suggestion != null) {
                        Text("PRÓXIMO TREINO SUGERIDO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(suggestion.name, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${suggestion.exerciseCount} exercícios · ${suggestion.totalSets} séries" + (suggestion.lastPerformed?.let { " · último em ${fmtDayMonth(it)}" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(14.dp))
                        BigButton("Iniciar treino", { start(suggestion.id) }, Modifier.fillMaxWidth().testTag("start_suggested"), icon = Icons.Filled.PlayArrow)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { start(null) }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Treino livre") }
                    } else {
                        Text("Comece agora", style = MaterialTheme.typography.headlineMedium)
                        Text("Crie sua ficha de treino ou importe um modelo pronto (PPL, ABC, Upper/Lower…). Você também pode iniciar um treino livre.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(14.dp))
                        BigButton("Criar ficha de treino", { nav.navigate(Routes.ROUTINES) }, Modifier.fillMaxWidth().testTag("go_routines"))
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { start(null) }, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("start_free")) { Text("Treino livre") }
                    }
                }
            }

            item {
                GtCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ESTA SEMANA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$weekCount de ${settings.weeklyTarget} treinos", style = MaterialTheme.typography.titleLarge)
                        }
                        if (streak > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Bolt, null, tint = MaterialTheme.colorScheme.tertiary)
                            Text("$streak sem.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("S", "T", "Q", "Q", "S", "S", "D").forEachIndexed { i, l ->
                            val day = i + 1
                            val trained = day in trainedThisWeek
                            val isToday = today.dayOfWeek.value == day
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.size(34.dp).clip(CircleShape).background(if (trained) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center,
                                ) { Text(l, style = MaterialTheme.typography.labelMedium, color = if (trained) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isToday) FontWeight.Black else FontWeight.Medium) }
                                if (isToday) Box(Modifier.padding(top = 4.dp).size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface))
                            }
                        }
                    }
                }
            }

            if (sessions.isNotEmpty()) {
                item {
                    val last30 = sessions.filter { it.startedAt >= System.currentTimeMillis() - 30L * 86_400_000 }
                    StatRow {
                        StatTile("Treinos (30 dias)", "${last30.size}", Modifier.weight(1f))
                        StatTile("Volume (30 dias)", Format.volume(last30.sumOf { it.volume }), Modifier.weight(1f))
                    }
                }
                item { SectionHeader("Últimos treinos", action = "Ver tudo", onAction = { nav.navigate(Routes.HISTORY) }) }
                items(sessions.take(3), key = { it.id }) { s -> SessionCard(s) { nav.navigate(Routes.session(s.id)) } }
            }

            val r = routines.orEmpty()
            if (active == null && r.size > 1) {
                item { SectionHeader("Iniciar rapidamente", action = "Fichas", onAction = { nav.navigate(Routes.ROUTINES) }) }
                items(r.take(6), key = { "r${it.id}" }) { routine ->
                    GtCard(onClick = { start(routine.id) }, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                                Text("${routine.exerciseCount} exercícios" + (routine.program.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Filled.PlayArrow, "Iniciar ${routine.name}", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            if (sessions.isEmpty() && active == null) item {
                GtCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.EmojiEvents)
                        Spacer(Modifier.width(12.dp))
                        Text("Seus recordes, gráficos e histórico aparecem aqui depois do primeiro treino concluído.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(s: SessionSummary, onClick: () -> Unit) {
    GtCard(onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium)
                Text(fmtWeekday(s.startedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${Format.durationShort((s.endedAt ?: s.startedAt) - s.startedAt - s.pausedAccumMs)} · ${s.setCount} séries · ${Format.volume(s.volume)}",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
