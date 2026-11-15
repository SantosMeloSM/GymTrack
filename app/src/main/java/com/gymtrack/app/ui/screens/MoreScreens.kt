package com.gymtrack.app.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.gymtrack.app.AppContainer
import com.gymtrack.app.BuildConfig
import com.gymtrack.app.data.db.GoalEntity
import com.gymtrack.app.data.db.ReminderEntity
import com.gymtrack.app.data.repo.Analytics
import com.gymtrack.app.data.repo.AppSettings
import com.gymtrack.app.data.repo.BackupInfo
import com.gymtrack.app.data.repo.ThemeMode
import com.gymtrack.app.notify.ReminderScheduler
import com.gymtrack.app.notify.canPostNotifications
import com.gymtrack.app.ui.Routes
import com.gymtrack.app.ui.components.ConfirmDialog
import com.gymtrack.app.ui.components.EmptyState
import com.gymtrack.app.ui.components.EnumDropdown
import com.gymtrack.app.ui.components.ExercisePickerDialog
import com.gymtrack.app.ui.components.GtCard
import com.gymtrack.app.ui.components.GtScaffold
import com.gymtrack.app.ui.components.ListRow
import com.gymtrack.app.ui.components.ProgressBar
import com.gymtrack.app.ui.components.SectionHeader
import com.gymtrack.app.ui.components.appContainer
import com.gymtrack.app.ui.components.appViewModel
import com.gymtrack.app.ui.components.fmtDate
import com.gymtrack.domain.Format
import com.gymtrack.domain.OneRepMaxFormula
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(nav: NavController) {
    GtScaffold("Mais", isTab = true) { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                GtCard(contentPadding = PaddingValues(0.dp)) {
                    ListRow("Evolução corporal", subtitle = "Peso, medidas e fotos", icon = Icons.Outlined.MonitorWeight, onClick = { nav.navigate(Routes.BODY) }, trailing = { Chevron() })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListRow("Metas de desempenho", subtitle = "Cargas e repetições alvo", icon = Icons.Outlined.Flag, onClick = { nav.navigate(Routes.GOALS) }, trailing = { Chevron() })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListRow("Histórico de treinos", subtitle = "Sessões, calendário e comparações", icon = Icons.Outlined.History, onClick = { nav.navigate(Routes.HISTORY) }, trailing = { Chevron() })
                }
            }
            item {
                GtCard(contentPadding = PaddingValues(0.dp)) {
                    ListRow("Lembretes de treino", subtitle = "Notificações por dia da semana", icon = Icons.Outlined.Alarm, onClick = { nav.navigate(Routes.REMINDERS) }, trailing = { Chevron() })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListRow("Backup e exportação", subtitle = "Backup local, importação, PDF e CSV", icon = Icons.Outlined.Backup, onClick = { nav.navigate(Routes.BACKUP) }, trailing = { Chevron() })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListRow("Configurações", subtitle = "Tema, descanso, vibração, 1RM", icon = Icons.Outlined.Settings, onClick = { nav.navigate(Routes.SETTINGS) }, trailing = { Chevron() })
                }
            }
            item {
                GtCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("GymTrack ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                            Text("Funciona 100% offline. Seus dados ficam somente neste aparelho, sem login e sem assinatura.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Chevron() = Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)

// ---------------------------------------------------------------- settings

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) =
    ListRow(title, subtitle = subtitle, onClick = { onChange(!checked) }, trailing = { Switch(checked, onChange) })

@Composable
fun SettingsScreen(nav: NavController) {
    val c = appContainer()
    val s by c.settings.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var restDialog by remember { mutableStateOf(false) }
    GtScaffold("Configurações", onBack = { nav.popBackStack() }) { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionHeader("Aparência") }
            item {
                GtCard {
                    Text("Tema", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { ThemeMode.entries.forEach { t -> FilterChip(selected = s.themeMode == t, onClick = { scope.launch { c.settings.setTheme(t) } }, label = { Text(t.label) }, modifier = Modifier.testTag("theme_${t.name}")) } }
                }
            }
            item { SectionHeader("Treino") }
            item {
                GtCard(contentPadding = PaddingValues(0.dp)) {
                    ListRow("Descanso padrão", subtitle = "${s.defaultRestSec}s para exercícios novos", onClick = { restDialog = true })
                    SwitchRow("Iniciar descanso automaticamente", "Ao concluir uma série", s.autoStartRest) { v -> scope.launch { c.settings.setAutoStartRest(v) } }
                    SwitchRow("Vibrar ao fim do descanso", null, s.vibrate) { v -> scope.launch { c.settings.setVibrate(v) } }
                    SwitchRow("Notificação de fim do descanso", "Quando o app está em segundo plano", s.restNotifications) { v -> scope.launch { c.settings.setRestNotifications(v) } }
                    SwitchRow("Mostrar coluna RPE/RIR", "Registro opcional de esforço", s.showRpe) { v -> scope.launch { c.settings.setShowRpe(v) } }
                    SwitchRow("Manter tela ligada no treino", null, s.keepScreenOn) { v -> scope.launch { c.settings.setKeepScreenOn(v) } }
                }
            }
            item {
                GtCard {
                    Text("Meta semanal de treinos", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { (1..7).forEach { n -> FilterChip(selected = s.weeklyTarget == n, onClick = { scope.launch { c.settings.setWeeklyTarget(n) } }, label = { Text("$n") }) } }
                    Spacer(Modifier.height(10.dp))
                    Text("Fórmula do 1RM estimado", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { OneRepMaxFormula.entries.forEach { f -> FilterChip(selected = s.oneRmFormula == f, onClick = { scope.launch { c.settings.setFormula(f) } }, label = { Text(f.label) }) } }
                    Text("Epley: carga × (1 + reps/30). Brzycki: carga × 36/(37 − reps). Calculado apenas para séries de até 12 repetições.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { SectionHeader("Sistema") }
            item {
                GtCard(contentPadding = PaddingValues(0.dp)) {
                    SwitchRow("Backup automático local", "Após cada treino concluído (mantém os 7 mais recentes)", s.autoBackup) { v -> scope.launch { c.settings.setAutoBackup(v) } }
                    ListRow("Permissões de notificação", subtitle = if (context.canPostNotifications()) "Permitidas" else "Desativadas — toque para abrir", onClick = {
                        val i = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) else Intent(Settings.ACTION_SETTINGS)
                        runCatching { context.startActivity(i) }
                    })
                }
            }
        }
    }
    if (restDialog) RestPickerDialog(s.defaultRestSec, { restDialog = false }) { v -> scope.launch { c.settings.setDefaultRest(v.coerceAtLeast(10)) }; restDialog = false }
}

// --------------------------------------------------------------- reminders

private val dayNames = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RemindersScreen(nav: NavController) {
    val c = appContainer()
    val list by c.reminders.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var adding by remember { mutableStateOf(false) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun save(r: ReminderEntity) = scope.launch { val id = c.reminders.save(r); ReminderScheduler.schedule(context, r.copy(id = if (r.id == 0L) id else r.id)) }
    GtScaffold("Lembretes", onBack = { nav.popBackStack() }, fab = { ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Novo lembrete") }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!context.canPostNotifications() && Build.VERSION.SDK_INT >= 33) item {
                GtCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("As notificações estão desativadas. Permita para receber os lembretes.", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    TextButton(onClick = { perm.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) { Text("Permitir notificações") }
                }
            }
            if (list.isEmpty()) item { EmptyState(Icons.Outlined.Alarm, "Nenhum lembrete", "Crie lembretes para os dias em que você costuma treinar.") }
            items(list, key = { it.id }) { r ->
                GtCard(contentPadding = PaddingValues(0.dp)) {
                    ListRow("${dayNames[r.dayOfWeek - 1]} · %02d:%02d".format(r.hour, r.minute), subtitle = r.label.ifBlank { null }, trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(r.enabled, { v -> save(r.copy(enabled = v)) })
                            IconButton(onClick = { scope.launch { ReminderScheduler.cancel(context, r.id); c.reminders.delete(r.id) } }) { Icon(Icons.Outlined.Delete, "Excluir lembrete") }
                        }
                    })
                }
            }
            item { Text("Lembretes usam alarmes que economizam bateria e podem atrasar alguns minutos quando o aparelho está em modo de economia.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    if (adding) {
        val days = remember { mutableStateOf(setOf<Int>()) }
        var hour by remember { mutableStateOf(18) }; var minute by remember { mutableStateOf(0) }
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Novo lembrete") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dias", style = MaterialTheme.typography.labelMedium)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        dayNames.forEachIndexed { i, d -> FilterChip(selected = (i + 1) in days.value, onClick = { days.value = if ((i + 1) in days.value) days.value - (i + 1) else days.value + (i + 1) }, label = { Text(d.take(3)) }) }
                    }
                    OutlinedButton(onClick = { TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show() }) { Text("Horário: %02d:%02d".format(hour, minute)) }
                    OutlinedTextField(label, { label = it }, label = { Text("Mensagem (opcional)") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(enabled = days.value.isNotEmpty(), onClick = {
                days.value.forEach { d -> save(ReminderEntity(dayOfWeek = d, hour = hour, minute = minute, label = label.trim())) }
                if (Build.VERSION.SDK_INT >= 33 && !context.canPostNotifications()) perm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                adding = false
            }) { Text("Salvar", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancelar") } },
        )
    }
}

// ------------------------------------------------------------------- goals

data class GoalProgress(val goal: GoalEntity, val exerciseName: String, val bestWeightAtReps: Double?, val fraction: Float, val achieved: Boolean)

class GoalsViewModel(private val c: AppContainer) : ViewModel() {
    val exercises = c.workouts.observeExercises().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val goals = c.goals.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val rows = c.workouts.observeAllCompletedRows().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun save(g: GoalEntity) = viewModelScope.launch { c.goals.save(g) }
    fun delete(id: Long) = viewModelScope.launch { c.goals.delete(id) }

    /** Progress = heaviest working set done with at least the target reps, relative to the target weight. */
    fun progress(goals: List<GoalEntity>, rows: List<com.gymtrack.app.data.db.SetRow>, names: Map<Long, String>): List<GoalProgress> = goals.map { g ->
        val qualifying = rows.filter { it.exerciseId == g.exerciseId && !it.isWarmup && (it.reps ?: 0) >= g.targetReps && it.startedAt >= g.createdAt - 365L * 86_400_000 }
        val best = qualifying.mapNotNull { it.weight }.maxOrNull()
        val achieved = best != null && best >= g.targetWeight
        if (achieved && g.achievedAt == null) {
            val at = qualifying.filter { (it.weight ?: 0.0) >= g.targetWeight }.minOf { it.startedAt }
            save(g.copy(achievedAt = at))
        }
        GoalProgress(g, names[g.exerciseId] ?: "Exercício", best, if (g.targetWeight <= 0) 1f else ((best ?: 0.0) / g.targetWeight).toFloat(), achieved)
    }
}

@Composable
fun GoalsScreen(nav: NavController) {
    val vm = appViewModel { GoalsViewModel(it) }
    val exercises by vm.exercises.collectAsState()
    val goals by vm.goals.collectAsState()
    val rows by vm.rows.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var newFor by remember { mutableStateOf<Long?>(null) }
    val names = exercises.associate { it.id to it.name }
    val progress = remember(goals, rows, names) { vm.progress(goals, rows, names) }
    GtScaffold("Metas", onBack = { nav.popBackStack() }, fab = { ExtendedFloatingActionButton(onClick = { picking = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Nova meta") }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (progress.isEmpty()) item { EmptyState(Icons.Outlined.Flag, "Nenhuma meta", "Defina metas como \"Supino 100 kg × 5\". O progresso é calculado a partir das suas séries registradas.") }
            items(progress, key = { it.goal.id }) { p ->
                GtCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.exerciseName, style = MaterialTheme.typography.titleMedium)
                            Text("Meta: ${Format.weight(p.goal.targetWeight)} kg × ${p.goal.targetReps}" + (p.goal.deadline?.let { " · até ${fmtDate(it)}" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { vm.delete(p.goal.id) }) { Icon(Icons.Outlined.Delete, "Excluir meta") }
                    }
                    Spacer(Modifier.height(8.dp))
                    ProgressBar(p.fraction, color = if (p.achieved) com.gymtrack.app.ui.theme.Gt.colors.success else MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            p.achieved -> "Meta atingida" + (p.goal.achievedAt?.let { " em ${fmtDate(it)}" } ?: "") + "!"
                            p.bestWeightAtReps != null -> "Melhor com ≥${p.goal.targetReps} reps: ${Format.weight(p.bestWeightAtReps)} kg (${(p.fraction * 100).toInt()}%)"
                            else -> "Ainda sem séries com ${p.goal.targetReps}+ repetições"
                        },
                        style = MaterialTheme.typography.labelMedium, color = if (p.achieved) com.gymtrack.app.ui.theme.Gt.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (picking) ExercisePickerDialog(exercises, multi = false, title = "Meta para qual exercício?", onDismiss = { picking = false }, onPicked = { newFor = it.first(); picking = false }, onCreate = { _, _, _, _ -> })
    newFor?.let { exId ->
        var w by remember { mutableStateOf("") }; var r by remember { mutableStateOf("5") }
        AlertDialog(
            onDismissRequest = { newFor = null },
            title = { Text(names[exId] ?: "Meta") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(w, { w = it.take(6) }, label = { Text("Carga (kg)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(r, { r = it.filter(Char::isDigit).take(3) }, label = { Text("Reps") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            },
            confirmButton = { TextButton(enabled = Format.parseDecimal(w) != null && (r.toIntOrNull() ?: 0) > 0, onClick = { vm.save(GoalEntity(exerciseId = exId, targetWeight = Format.parseDecimal(w)!!, targetReps = r.toInt(), createdAt = System.currentTimeMillis())); newFor = null }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { newFor = null }) { Text("Cancelar") } },
        )
    }
}

// ------------------------------------------------------------------ backup

class BackupViewModel(private val c: AppContainer) : ViewModel() {
    val messages = Channel<String>(Channel.BUFFERED)
    var autoBackups by mutableStateOf<List<BackupInfo>>(emptyList()); private set
    var busy by mutableStateOf(false); private set
    init { refresh() }
    fun refresh() { autoBackups = c.backup.listAutoBackups() }
    private fun run(okMsg: String, block: suspend () -> Unit) = viewModelScope.launch {
        busy = true
        runCatching { block() }.onSuccess { messages.trySend(okMsg) }.onFailure { messages.trySend(it.message ?: "Falha na operação") }
        busy = false; refresh()
    }
    fun export(uri: android.net.Uri) = run("Backup exportado com sucesso") { c.backup.exportToUri(uri) }
    fun import(uri: android.net.Uri) = run("Backup importado. Uma cópia de segurança dos dados anteriores foi criada.") { c.backup.importFromUri(uri); ReminderScheduler.rescheduleAll(c.appContext) }
    fun backupNow() = run("Backup local criado") { c.backup.writeAutoBackup(prefix = "manual") }
    fun restore(b: BackupInfo) = run("Backup restaurado") { c.backup.restoreAuto(b.file); ReminderScheduler.rescheduleAll(c.appContext) }
    fun csv(uri: android.net.Uri) = run("CSV exportado") { c.reports.exportCsv(uri, c.workouts.observeAllCompletedRows().first(), c.settings.current().oneRmFormula) }
    fun pdf(uri: android.net.Uri) = run("Relatório PDF exportado") {
        val s = c.settings.current()
        c.reports.exportPdf(uri, c.workouts.observeAllCompletedRows().first(), c.workouts.observeFinishedSessions().first(), c.body.observeMeasurements().first(), s.oneRmFormula, s.weeklyTarget)
    }
}

@Composable
fun BackupScreen(nav: NavController) {
    val vm = appViewModel { BackupViewModel(it) }
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    LaunchedEffect(Unit) { for (m in vm.messages) snackbar.showSnackbar(m) }
    val stamp = fmtDate(System.currentTimeMillis(), "yyyy-MM-dd")
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let(vm::export) }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingImport = it }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let(vm::csv) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { it?.let(vm::pdf) }
    var restoring by remember { mutableStateOf<BackupInfo?>(null) }

    GtScaffold("Backup e exportação", onBack = { nav.popBackStack() }, snackbar = snackbar) { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (vm.busy) item { androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth()) }
            item { SectionHeader("Backup completo") }
            item {
                GtCard(contentPadding = PaddingValues(0.dp)) {
                    ListRow("Exportar backup (.zip)", subtitle = "Fichas, treinos, medidas, fotos e configurações de metas/lembretes", icon = Icons.Outlined.Backup, onClick = { exportLauncher.launch("gymtrack-backup-$stamp.zip") })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListRow("Importar backup", subtitle = "Substitui os dados atuais (uma cópia de segurança é criada antes)", onClick = { importLauncher.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "*/*")) })
                }
            }
            item { SectionHeader("Relatórios") }
            item {
                GtCard(contentPadding = PaddingValues(0.dp)) {
                    ListRow("Relatório de progresso (PDF)", subtitle = "Resumo, recordes, sessões e medidas", onClick = { pdfLauncher.launch("gymtrack-relatorio-$stamp.pdf") })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListRow("Exportar séries (CSV)", subtitle = "Todas as séries concluídas, para planilhas", onClick = { csvLauncher.launch("gymtrack-series-$stamp.csv") })
                }
            }
            item { SectionHeader("Backups locais automáticos", action = "Criar agora", onAction = { vm.backupNow() }) }
            if (vm.autoBackups.isEmpty()) item { Text("Nenhum backup local ainda. Eles são criados automaticamente ao concluir treinos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(vm.autoBackups, key = { it.file.name }) { b ->
                GtCard(contentPadding = PaddingValues(0.dp)) {
                    ListRow(fmtDate(b.createdAt, "dd/MM/yyyy HH:mm"), subtitle = "${b.file.name.substringBefore('-').replaceFirstChar { it.uppercase() }} · ${b.sizeBytes / 1024} KB", trailing = { TextButton(onClick = { restoring = b }) { Text("Restaurar") } })
                }
            }
            item { Text("Backups locais ficam no armazenamento privado do app. Para proteção contra perda ou troca de aparelho, exporte o .zip e guarde-o em outro local.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    pendingImport?.let { uri -> ConfirmDialog("Importar backup?", "Todos os dados atuais serão substituídos pelo conteúdo do arquivo. Uma cópia de segurança local dos dados atuais será criada antes.", "Importar", { vm.import(uri) }, { pendingImport = null }, destructive = true) }
    restoring?.let { b -> ConfirmDialog("Restaurar backup?", "Os dados atuais serão substituídos pelos de ${fmtDate(b.createdAt, "dd/MM/yyyy HH:mm")}. Uma cópia dos dados atuais é criada antes.", "Restaurar", { vm.restore(b) }, { restoring = null }, destructive = true) }
}
