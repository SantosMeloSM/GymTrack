package com.gymtrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.gymtrack.app.AppContainer
import com.gymtrack.app.data.db.SessionSummary
import com.gymtrack.app.data.repo.AppSettings
import com.gymtrack.app.data.repo.SessionDetail
import com.gymtrack.app.data.repo.toLogged
import com.gymtrack.app.ui.Routes
import com.gymtrack.app.ui.components.BigButton
import com.gymtrack.app.ui.components.ConfirmDialog
import com.gymtrack.app.ui.components.EmptyState
import com.gymtrack.app.ui.components.GtCard
import com.gymtrack.app.ui.components.GtScaffold
import com.gymtrack.app.ui.components.IconBadge
import com.gymtrack.app.ui.components.MonthCalendar
import com.gymtrack.app.ui.components.SectionHeader
import com.gymtrack.app.ui.components.StatRow
import com.gymtrack.app.ui.components.StatTile
import com.gymtrack.app.ui.components.appViewModel
import com.gymtrack.app.ui.components.fmtDate
import com.gymtrack.app.ui.components.fmtWeekday
import com.gymtrack.app.ui.theme.Gt
import com.gymtrack.domain.Consistency
import com.gymtrack.domain.ExercisePerformance
import com.gymtrack.domain.Format
import com.gymtrack.domain.LoggedSet
import com.gymtrack.domain.OneRepMax
import com.gymtrack.domain.RecordBreak
import com.gymtrack.domain.RecordType
import com.gymtrack.domain.Records
import com.gymtrack.domain.WorkoutStats
import java.time.YearMonth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun com.gymtrack.app.data.db.SetEntity.logged() = LoggedSet(weight ?: 0.0, reps ?: 0, isWarmup, completed, rpe, rir)

// ------------------------------------------------------------------ history

class HistoryViewModel(c: AppContainer) : ViewModel() {
    val sessions = c.workouts.observeFinishedSessions().stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@Composable
fun HistoryScreen(nav: NavController) {
    val vm = appViewModel { HistoryViewModel(it) }
    val sessions by vm.sessions.collectAsState()
    var calendar by rememberSaveable { mutableStateOf(false) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var dayFilter by remember { mutableStateOf<java.time.LocalDate?>(null) }
    val consistency = remember { Consistency() }

    GtScaffold("Histórico", onBack = { nav.popBackStack() }, actions = {
        IconButton(onClick = { calendar = !calendar; dayFilter = null }) { Icon(if (calendar) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.CalendarMonth, if (calendar) "Ver lista" else "Ver calendário") }
    }) { pad ->
        val list = sessions ?: return@GtScaffold
        if (list.isEmpty()) { Column(Modifier.padding(pad)) { EmptyState(Icons.Outlined.History, "Nenhum treino concluído", "Seus treinos finalizados aparecem aqui.") }; return@GtScaffold }
        val shown = when {
            dayFilter != null -> list.filter { consistency.toDate(it.startedAt) == dayFilter }
            calendar -> list.filter { YearMonth.from(consistency.toDate(it.startedAt)) == month }
            else -> list
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (calendar) item {
                GtCard {
                    MonthCalendar(month, consistency.trainingDaysInMonth(list.map { it.startedAt }, month.year, month.monthValue), onPrev = { month = month.minusMonths(1); dayFilter = null }, onNext = { month = month.plusMonths(1); dayFilter = null }, onDayClick = { dayFilter = it })
                    Text("${shown.size} treino(s) ${if (dayFilter != null) "em ${dayFilter!!.dayOfMonth}/${dayFilter!!.monthValue}" else "neste mês"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(shown, key = { it.id }) { s -> SessionCard(s) { nav.navigate(Routes.session(s.id)) } }
        }
    }
}

// ------------------------------------------------------------ detail/summary

data class ExerciseResult(val name: String, val exerciseId: Long, val sets: List<LoggedSet>, val records: List<RecordBreak>, val e1rm: Double?)

class SessionDetailViewModel(private val c: AppContainer, val sessionId: Long) : ViewModel() {
    val detail = c.workouts.observeSessionDetail(sessionId).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val all = c.workouts.observeFinishedSessions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    var results by mutableStateOf<List<ExerciseResult>>(emptyList()); private set

    init {
        viewModelScope.launch {
            detail.collect { d -> if (d != null) results = computeResults(d) }
        }
    }

    /** Records are compared only with sessions that happened BEFORE this one. */
    private suspend fun computeResults(d: SessionDetail): List<ExerciseResult> {
        val formula = c.settings.current().oneRmFormula
        return d.exercises.map { ex ->
            val history = c.workouts.getExerciseRows(ex.exercise.id).filter { it.startedAt < d.session.startedAt && it.sessionId != d.session.id }
            val prev = history.groupBy { it.sessionId }.map { (sid, r) -> ExercisePerformance(sid, r.first().startedAt, r.map { it.toLogged() }) }
            val sets = ex.sets.filter { it.completed }.map { it.logged() }
            ExerciseResult(ex.exercise.name, ex.exercise.id, sets, Records.broken(prev, sets, formula), OneRepMax.best(sets, formula))
        }
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch { c.workouts.deleteSession(sessionId); onDone() }
    fun updateRoutine(onDone: (Int) -> Unit) = viewModelScope.launch { onDone(c.workouts.updateRoutineFromSession(sessionId)) }
}

@Composable
private fun SessionStats(d: SessionDetail) {
    val sets = d.exercises.flatMap { it.sets }.filter { it.completed }.map { it.logged() }
    StatRow {
        StatTile("Duração", Format.durationShort(d.session.elapsedMs(d.session.endedAt ?: System.currentTimeMillis())), Modifier.weight(1f))
        StatTile("Séries", "${WorkoutStats.workingSets(sets)}", Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    StatRow {
        StatTile("Volume", Format.volume(WorkoutStats.volume(sets)), Modifier.weight(1f), accent = true)
        StatTile("Repetições", "${WorkoutStats.totalReps(sets)}", Modifier.weight(1f))
    }
}

@Composable
private fun ResultCard(r: ExerciseResult, onClick: () -> Unit) {
    GtCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (r.records.isNotEmpty()) Icon(Icons.Outlined.EmojiEvents, "Recorde", tint = Gt.colors.record)
        }
        Spacer(Modifier.height(6.dp))
        var n = 0
        r.sets.forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(if (s.isWarmup) "A" else "${++n}", Modifier.width(28.dp), color = if (s.isWarmup) Gt.colors.warmup else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                Text("${Format.weight(s.weight)} kg × ${s.reps}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                val effort = listOfNotNull(s.rpe?.let { "RPE ${Format.weight(it)}" }, s.rir?.let { "RIR $it" }).joinToString(" · ")
                if (effort.isNotEmpty()) Text(effort, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        r.e1rm?.let { Text("1RM estimado: ${Format.weight(OneRepMax.round(it))} kg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
        r.records.forEach { rb ->
            val v = if (rb.type == RecordType.REPS) "${rb.value.toInt()} reps" else if (rb.type == RecordType.VOLUME) Format.volume(rb.value) else "${Format.weight(OneRepMax.round(rb.value))} kg"
            Text("🏆 ${rb.type.label}: $v", style = MaterialTheme.typography.labelMedium, color = Gt.colors.record, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun SessionDetailScreen(nav: NavController, id: Long) {
    val vm = appViewModel(key = "session_$id") { SessionDetailViewModel(it, id) }
    val d by vm.detail.collectAsState()
    val all by vm.all.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    var pickCompare by remember { mutableStateOf(false) }
    val detail = d ?: run { GtScaffold("Sessão", onBack = { nav.popBackStack() }) {}; return }
    GtScaffold(detail.session.name, onBack = { nav.popBackStack() }, actions = {
        IconButton(onClick = { pickCompare = true }) { Icon(Icons.Outlined.CompareArrows, "Comparar") }
        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Excluir sessão") }
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(fmtWeekday(detail.session.startedAt) + " · " + fmtDate(detail.session.startedAt, "HH:mm"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { SessionStats(detail) }
            if (detail.session.notes.isNotBlank()) item { GtCard { Text("Observações", style = MaterialTheme.typography.titleSmall); Text(detail.session.notes) } }
            items(vm.results, key = { it.exerciseId.toString() + it.name }) { r -> ResultCard(r) { nav.navigate(Routes.exercise(r.exerciseId)) } }
        }
    }
    if (confirmDelete) ConfirmDialog("Excluir sessão?", "Esta sessão e todas as séries serão apagadas do histórico.", "Excluir", { vm.delete { nav.popBackStack() } }, { confirmDelete = false }, destructive = true)
    if (pickCompare) ComparePicker(all.filter { it.id != id }, detail.session.routineId, onDismiss = { pickCompare = false }) { other -> pickCompare = false; nav.navigate(Routes.compare(other, id)) }
}

@Composable
private fun ComparePicker(sessions: List<SessionSummary>, routineId: Long?, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val sorted = sessions.sortedByDescending { if (routineId != null && it.routineId == routineId) 1 else 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comparar com…") },
        text = {
            if (sorted.isEmpty()) Text("Não há outras sessões para comparar.")
            else LazyColumn {
                items(sorted.take(50), key = { it.id }) { s ->
                    TextButton(onClick = { onPick(s.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(s.name + if (routineId != null && s.routineId == routineId) "  • mesma ficha" else "", style = MaterialTheme.typography.titleSmall)
                            Text(fmtDate(s.startedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
fun SessionSummaryScreen(nav: NavController, id: Long) {
    val vm = appViewModel(key = "summary_$id") { SessionDetailViewModel(it, id) }
    val d by vm.detail.collectAsState()
    var updated by remember { mutableStateOf<Int?>(null) }
    val detail = d ?: run { Box(Modifier.fillMaxSize()); return }
    val prCount = vm.results.sumOf { it.records.size }
    fun close() = nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
    androidx.activity.compose.BackHandler { close() }
    GtScaffold("Treino concluído", onBack = { close() }, bottomBar = {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                if (detail.session.routineId != null && updated == null) {
                    OutlinedButton(onClick = { vm.updateRoutine { updated = it } }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Atualizar cargas planejadas da ficha") }
                    Spacer(Modifier.height(8.dp))
                }
                BigButton("Concluir", { close() }, Modifier.fillMaxWidth().testTag("summary_done"))
            }
        }
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconBadge(Icons.Outlined.EmojiEvents, size = 72, tint = if (prCount > 0) Gt.colors.record else MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(detail.session.name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(if (prCount > 0) "$prCount recorde(s) pessoal(is) nesta sessão!" else "Bom trabalho! Tudo foi salvo no histórico.", color = if (prCount > 0) Gt.colors.record else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    updated?.let { Text(if (it > 0) "$it carga(s) da ficha atualizada(s)." else "As cargas da ficha já estavam atualizadas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
            item { SessionStats(detail) }
            item { SectionHeader("Exercícios") }
            items(vm.results, key = { it.exerciseId.toString() + it.name }) { r -> ResultCard(r) { nav.navigate(Routes.exercise(r.exerciseId)) } }
        }
    }
}

// ----------------------------------------------------------------- compare

class CompareViewModel(c: AppContainer, a: Long, b: Long) : ViewModel() {
    val first = c.workouts.observeSessionDetail(a).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val second = c.workouts.observeSessionDetail(b).stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@Composable
fun CompareScreen(nav: NavController, a: Long, b: Long) {
    val vm = appViewModel(key = "cmp_${a}_$b") { CompareViewModel(it, a, b) }
    val d1 by vm.first.collectAsState(); val d2 by vm.second.collectAsState()
    val x = d1; val y = d2
    GtScaffold("Comparar sessões", onBack = { nav.popBackStack() }) { pad ->
        if (x == null || y == null) return@GtScaffold
        // Oldest on the left.
        val (l, r) = if (x.session.startedAt <= y.session.startedAt) x to y else y to x
        fun sets(d: SessionDetail) = d.exercises.flatMap { it.sets }.filter { it.completed }.map { it.logged() }
        val ids = (l.exercises.map { it.exercise.id } + r.exercises.map { it.exercise.id }).distinct()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                GtCard {
                    Row { Text("", Modifier.weight(1f)); Text(fmtDate(l.session.startedAt), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Gt.colors.seriesB, textAlign = TextAlign.End); Text(fmtDate(r.session.startedAt), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Gt.colors.seriesA, textAlign = TextAlign.End) }
                    Text("${l.session.name}  →  ${r.session.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    CompareRow("Duração", Format.durationShort(l.session.elapsedMs(l.session.endedAt ?: 0)), Format.durationShort(r.session.elapsedMs(r.session.endedAt ?: 0)), null)
                    val v1 = WorkoutStats.volume(sets(l)); val v2 = WorkoutStats.volume(sets(r))
                    CompareRow("Volume", Format.volume(v1), Format.volume(v2), v2 - v1)
                    CompareRow("Séries", "${WorkoutStats.workingSets(sets(l))}", "${WorkoutStats.workingSets(sets(r))}", (WorkoutStats.workingSets(sets(r)) - WorkoutStats.workingSets(sets(l))).toDouble())
                    CompareRow("Repetições", "${WorkoutStats.totalReps(sets(l))}", "${WorkoutStats.totalReps(sets(r))}", (WorkoutStats.totalReps(sets(r)) - WorkoutStats.totalReps(sets(l))).toDouble())
                }
            }
            items(ids) { exId ->
                val e1 = l.exercises.firstOrNull { it.exercise.id == exId }; val e2 = r.exercises.firstOrNull { it.exercise.id == exId }
                val s1 = e1?.sets?.filter { it.completed }?.map { it.logged() }.orEmpty(); val s2 = e2?.sets?.filter { it.completed }?.map { it.logged() }.orEmpty()
                GtCard {
                    Text((e1 ?: e2)!!.exercise.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    val w1 = WorkoutStats.maxWeight(s1); val w2 = WorkoutStats.maxWeight(s2)
                    CompareRow("Maior carga", w1?.let { "${Format.weight(it)} kg" } ?: "–", w2?.let { "${Format.weight(it)} kg" } ?: "–", if (w1 != null && w2 != null) w2 - w1 else null)
                    val o1 = OneRepMax.best(s1); val o2 = OneRepMax.best(s2)
                    CompareRow("1RM est.", o1?.let { "${Format.weight(OneRepMax.round(it))} kg" } ?: "–", o2?.let { "${Format.weight(OneRepMax.round(it))} kg" } ?: "–", if (o1 != null && o2 != null) o2 - o1 else null)
                    CompareRow("Volume", Format.volume(WorkoutStats.volume(s1)), Format.volume(WorkoutStats.volume(s2)), if (s1.isNotEmpty() && s2.isNotEmpty()) WorkoutStats.volume(s2) - WorkoutStats.volume(s1) else null)
                    Text(
                        "Antes: " + (s1.filter { it.isWorking }.joinToString("  ") { "${Format.weight(it.weight)}×${it.reps}" }.ifEmpty { "—" }) +
                            "\nDepois: " + (s2.filter { it.isWorking }.joinToString("  ") { "${Format.weight(it.weight)}×${it.reps}" }.ifEmpty { "—" }),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareRow(label: String, a: String, b: String, delta: Double?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(a, Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(b, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (delta != null && delta != 0.0) Text((if (delta > 0) "▲ +" else "▼ ") + Format.weight(kotlin.math.abs(OneRepMax.round(delta))).let { if (delta < 0) "−$it" else it }, style = MaterialTheme.typography.labelSmall, color = if (delta > 0) Gt.colors.success else MaterialTheme.colorScheme.error)
        }
    }
}
