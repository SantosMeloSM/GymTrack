package com.gymtrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.gymtrack.app.AppContainer
import com.gymtrack.app.data.repo.Analytics
import com.gymtrack.app.data.repo.AppSettings
import com.gymtrack.app.ui.Routes
import com.gymtrack.app.ui.components.BarChart
import com.gymtrack.app.ui.components.BarDatum
import com.gymtrack.app.ui.components.ChartPoint
import com.gymtrack.app.ui.components.EmptyState
import com.gymtrack.app.ui.components.GtCard
import com.gymtrack.app.ui.components.GtScaffold
import com.gymtrack.app.ui.components.HorizontalBars
import com.gymtrack.app.ui.components.LineChart
import com.gymtrack.app.ui.components.MonthCalendar
import com.gymtrack.app.ui.components.SectionHeader
import com.gymtrack.app.ui.components.StatRow
import com.gymtrack.app.ui.components.StatTile
import com.gymtrack.app.ui.components.appViewModel
import com.gymtrack.app.ui.components.fmtDayMonth
import com.gymtrack.domain.Consistency
import com.gymtrack.domain.Format
import com.gymtrack.domain.OneRepMax
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class ProgressViewModel(c: AppContainer) : ViewModel() {
    val rows = c.workouts.observeAllCompletedRows().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val sessions = c.workouts.observeFinishedSessions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
}

private enum class Period(val label: String, val days: Int) { W4("4 sem.", 28), W12("12 sem.", 84), Y1("1 ano", 365), ALL("Tudo", 36500) }

@Composable
fun ProgressScreen(nav: NavController) {
    val vm = appViewModel { ProgressViewModel(it) }
    val rowsN by vm.rows.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val settings by vm.settings.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var period by rememberSaveable { mutableStateOf(Period.W4) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    val c = remember { Consistency() }
    val today = LocalDate.now()

    GtScaffold("Progresso", isTab = true) { pad ->
        val rows = rowsN ?: return@GtScaffold
        if (sessions.isEmpty()) { Column(Modifier.padding(pad)) { EmptyState(Icons.Outlined.Insights, "Sem dados ainda", "Conclua seu primeiro treino para ver gráficos de evolução, recordes e consistência.") }; return@GtScaffold }
        val dates = sessions.map { it.startedAt }
        val from = today.minusDays(period.days.toLong() - 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val inPeriod = sessions.filter { it.startedAt >= from }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Visão geral", "Exercícios").forEachIndexed { i, l -> SegmentedButton(selected = tab == i, onClick = { tab = i }, shape = SegmentedButtonDefaults.itemShape(i, 2), label = { Text(l) }, modifier = Modifier.testTag("progress_tab_$i")) }
                }
            }
            if (tab == 0) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Period.entries.forEach { p -> FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) }) } }
                }
                item {
                    StatRow {
                        StatTile("Treinos", "${inPeriod.size}", Modifier.weight(1f))
                        StatTile("Volume", Format.volume(inPeriod.sumOf { it.volume }), Modifier.weight(1f), accent = true)
                        StatTile("Tempo", Format.durationShort(inPeriod.sumOf { ((it.endedAt ?: it.startedAt) - it.startedAt - it.pausedAccumMs).coerceAtLeast(0) }), Modifier.weight(1f))
                    }
                }
                item {
                    val target = settings.weeklyTarget
                    GtCard {
                        Text("Consistência", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        StatRow {
                            StatTile("Sequência", "${c.weekStreak(dates, today, target)} sem.", Modifier.weight(1f), sub = "meta $target/sem")
                            StatTile("Aderência", "${c.adherence(dates, today, target, 8)}%", Modifier.weight(1f), sub = "8 semanas")
                            StatTile("Dias ativos", "${c.activeDays(dates, today, 30)}", Modifier.weight(1f), sub = "30 dias")
                        }
                    }
                }
                item {
                    val weeks = c.weeklyCounts(dates, today, 12)
                    GtCard {
                        Text("Frequência semanal", style = MaterialTheme.typography.titleMedium)
                        Text("Treinos por semana · linha tracejada = meta", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        BarChart(weeks.map { (ws, n) -> BarDatum(n.toFloat(), "${ws.dayOfMonth}/${ws.monthValue}", "Semana de ${ws.dayOfMonth}/${ws.monthValue}: $n treino(s)") }, target = settings.weeklyTarget.toFloat(), description = "Treinos por semana")
                    }
                }
                item {
                    val weeks = c.weeklyCounts(emptyList(), today, 12).map { it.first }
                    val volByWeek = sessions.groupBy { c.weekStart(c.toDate(it.startedAt)) }.mapValues { (_, l) -> l.sumOf { it.volume } }
                    GtCard {
                        Text("Volume semanal", style = MaterialTheme.typography.titleMedium)
                        Text("Soma de carga × repetições das séries de trabalho", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        BarChart(weeks.map { ws -> val v = volByWeek[ws] ?: 0.0; BarDatum(v.toFloat(), "${ws.dayOfMonth}/${ws.monthValue}", "Semana de ${ws.dayOfMonth}/${ws.monthValue}: ${Format.volume(v)}") }, description = "Volume por semana")
                    }
                }
                item {
                    val vols = Analytics.sessionVolumes(rows.filter { it.startedAt >= from })
                    if (vols.size >= 2) GtCard {
                        Text("Volume por sessão", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        LineChart(vols.map { (d, v) -> ChartPoint(v.toFloat(), fmtDayMonth(d), "${fmtDayMonth(d)} · ${Format.volume(v)}") }, description = "Volume por sessão")
                    }
                }
                item {
                    val breakdown = Analytics.muscleBreakdown(rows, from, Long.MAX_VALUE)
                    GtCard {
                        Text("Séries por grupo muscular", style = MaterialTheme.typography.titleMedium)
                        Text("Séries de trabalho, pelo músculo principal do exercício (${period.label})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        if (breakdown.isEmpty()) Text("Sem séries no período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else HorizontalBars(breakdown.map { (m, n, v) -> Triple(m.label, n.toFloat(), "$n · ${Format.volume(v)}") })
                    }
                }
                item {
                    GtCard {
                        Text("Calendário", style = MaterialTheme.typography.titleMedium)
                        MonthCalendar(month, c.trainingDaysInMonth(dates, month.year, month.monthValue), onPrev = { month = month.minusMonths(1) }, onNext = { month = month.plusMonths(1) }, onDayClick = { nav.navigate(Routes.HISTORY) })
                    }
                }
            } else {
                val summaries = Analytics.exerciseSummaries(rows, settings.oneRmFormula)
                item { SectionHeader("Recordes e melhor desempenho (${summaries.size})") }
                items(summaries, key = { it.exerciseId }) { s ->
                    GtCard(onClick = { nav.navigate(Routes.exercise(s.exerciseId)) }, contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.name, style = MaterialTheme.typography.titleMedium)
                                Text("${s.muscle?.label ?: ""} · ${s.sessions} sessões · último ${fmtDayMonth(s.lastDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    listOfNotNull(
                                        s.records.maxWeight?.let { "Máx ${Format.weight(it)} kg × ${s.records.maxWeightReps}" },
                                        s.records.bestEstimated1Rm?.let { "1RM est. ${Format.weight(OneRepMax.round(it))} kg" },
                                    ).joinToString("  ·  "),
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
