package com.gymtrack.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gymtrack.app.data.db.SessionExerciseEntity
import com.gymtrack.app.data.db.SetEntity
import com.gymtrack.app.data.repo.AppSettings
import com.gymtrack.app.data.repo.SessionExerciseDetail
import com.gymtrack.app.notify.canPostNotifications
import com.gymtrack.app.ui.Routes
import com.gymtrack.app.ui.components.ConfirmDialog
import com.gymtrack.app.ui.components.ExercisePickerDialog
import com.gymtrack.app.ui.components.MusclePill
import com.gymtrack.app.ui.components.Pill
import com.gymtrack.app.ui.components.ProgressBar
import com.gymtrack.app.ui.components.appViewModel
import com.gymtrack.app.ui.theme.Gt
import com.gymtrack.domain.Format
import com.gymtrack.domain.MuscleGroup
import com.gymtrack.domain.WorkoutStats
import com.gymtrack.domain.LoggedSet
import kotlinx.coroutines.delay

private fun fmtNum(v: Double?): String = v?.let { Format.weight(it) } ?: ""

@Composable
fun WorkoutScreen(nav: NavController, settings: AppSettings) {
    val vm = appViewModel { WorkoutViewModel(it) }
    val detail by vm.detail.collectAsState()
    val hasActive by vm.hasActive.collectAsState()
    val previous by vm.previous.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val now by com.gymtrack.app.ui.components.rememberNow(250)
    LaunchedEffect(Unit) { for (m in vm.messages) snackbar.showSnackbar(m) }

    // Ask for notification permission once (Android 13+), needed for the rest alert.
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !context.canPostNotifications()) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(hasActive) { if (hasActive == false) nav.popBackStack() }

    var showPicker by remember { mutableStateOf(false) }
    var replaceFor by remember { mutableStateOf<Long?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var emptyFinish by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }

    BackHandler { nav.popBackStack() } // session stays active; can be resumed from Home

    val d = detail ?: run {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val s = d.session
    val paused = s.pausedAt != null
    val restRemaining = s.restEndsAt?.let { ((it - now) / 1000).toInt() + if ((it - now) % 1000 > 0) 1 else 0 }?.takeIf { it > 0 && !paused }

    val allSets = d.exercises.flatMap { it.sets }
    val done = allSets.count { it.completed && !it.isWarmup }
    val total = allSets.count { !it.isWarmup }
    val volume = WorkoutStats.volume(allSets.map { LoggedSet(it.weight ?: 0.0, it.reps ?: 0, it.isWarmup, it.completed) })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.statusBarsPadding().padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.KeyboardArrowDown, "Minimizar treino") }
                        Column(Modifier.weight(1f).clickable { editName = true }) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                Format.duration(s.elapsedMs(now)),
                                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("session_timer"),
                            )
                        }
                        IconButton(onClick = { if (paused) vm.resume() else vm.pause() }, modifier = Modifier.testTag("pause_button")) {
                            Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (paused) "Continuar" else "Pausar")
                        }
                        Button(onClick = { confirmFinish = true }, modifier = Modifier.testTag("finish_button")) { Text("Concluir", fontWeight = FontWeight.Bold) }
                    }
                    Row(Modifier.padding(start = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$done/$total séries", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("  ·  ${Format.volume(volume)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(if (total == 0) 0f else done / total.toFloat(), Modifier.padding(horizontal = 12.dp))
                }
            }
        },
        bottomBar = {
            Column {
                AnimatedVisibility(paused, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Treino pausado", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Button(onClick = { vm.resume() }) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Continuar") }
                        }
                    }
                }
                AnimatedVisibility(restRemaining != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                    RestBar(restRemaining ?: 0, s.restTotalSec, s.restExerciseName, onMinus = { vm.adjustRest(-15) }, onPlus = { vm.adjustRest(15) }, onSkip = { vm.skipRest() })
                }
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().imePadding().testTag("workout_list"),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = pad.calculateTopPadding() + 12.dp, bottom = pad.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (d.exercises.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Treino livre", style = MaterialTheme.typography.titleLarge)
                    Text("Adicione o primeiro exercício para começar a registrar.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            itemsIndexed(d.exercises, key = { _, e -> e.entry.id }) { idx, ex ->
                ExerciseBlock(
                    ex = ex, index = idx, count = d.exercises.size, previous = previous[ex.exercise.id].orEmpty(), showRpe = settings.showRpe,
                    vm = vm, onReplace = { replaceFor = ex.entry.id }, onOpenInfo = { nav.navigate(Routes.exercise(ex.exercise.id)) },
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("add_exercise"), shape = MaterialTheme.shapes.medium) {
                        Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar exercício", style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = { confirmDiscard = true }, modifier = Modifier.fillMaxWidth()) { Text("Descartar treino", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (showPicker) ExercisePickerDialog(exercises, multi = true, onDismiss = { showPicker = false }, onPicked = { vm.addExercises(it); showPicker = false }, onCreate = vm::createExercise)
    replaceFor?.let { seId ->
        ExercisePickerDialog(exercises, multi = false, title = "Trocar exercício", onDismiss = { replaceFor = null }, onPicked = { vm.replaceExercise(seId, it.first()); replaceFor = null }, onCreate = vm::createExercise)
    }
    if (confirmFinish) {
        val pending = allSets.count { !it.completed }
        ConfirmDialog(
            "Concluir treino?",
            if (pending > 0) "$pending série(s) não marcada(s) como concluída(s) serão descartadas. As séries concluídas ficam salvas no histórico." else "O treino será salvo no histórico.",
            "Concluir",
            onConfirm = { vm.finish { id -> if (id == null) emptyFinish = true else nav.navigate(Routes.summary(id)) { popUpTo(Routes.WORKOUT) { inclusive = true } } } },
            onDismiss = { confirmFinish = false },
        )
    }
    if (emptyFinish) AlertDialog(
        onDismissRequest = { emptyFinish = false },
        title = { Text("Nenhuma série concluída") },
        text = { Text("Marque ao menos uma série como concluída para salvar, ou descarte o treino.") },
        confirmButton = { TextButton(onClick = { emptyFinish = false }) { Text("Continuar treinando") } },
        dismissButton = { TextButton(onClick = { emptyFinish = false; vm.discard { } }) { Text("Descartar", color = MaterialTheme.colorScheme.error) } },
    )
    if (confirmDiscard) ConfirmDialog("Descartar treino?", "Todas as séries desta sessão serão apagadas. Esta ação não pode ser desfeita.", "Descartar", onConfirm = { vm.discard { } }, onDismiss = { confirmDiscard = false }, destructive = true)
    if (editName) {
        var name by remember { mutableStateOf(s.name) }
        var notes by remember { mutableStateOf(s.notes) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Sessão") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(notes, { notes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            },
            confirmButton = { TextButton(onClick = { vm.rename(name, notes); editName = false }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun RestBar(remaining: Int, total: Int, name: String?, onMinus: () -> Unit, onPlus: () -> Unit, onSkip: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp).testTag("rest_bar")) {
            ProgressBar(if (total <= 0) 0f else remaining / total.toFloat())
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DESCANSO" + (name?.let { " · $it" } ?: ""), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(Format.duration(remaining * 1000L), style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("rest_time"))
                }
                OutlinedButton(onClick = onMinus, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("−15s") }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(onClick = onPlus, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("+15s") }
                Spacer(Modifier.width(6.dp))
                Button(onClick = onSkip, modifier = Modifier.testTag("skip_rest")) { Text("Pular") }
            }
        }
    }
}

@Composable
private fun ExerciseBlock(
    ex: SessionExerciseDetail, index: Int, count: Int, previous: List<SetEntity>, showRpe: Boolean,
    vm: WorkoutViewModel, onReplace: () -> Unit, onOpenInfo: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var restDialog by remember { mutableStateOf(false) }
    var notesDialog by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val prevWarm = previous.filter { it.isWarmup }
    val prevWork = previous.filter { !it.isWarmup }

    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.testTag("exercise_block_${ex.exercise.name}")) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable(onClick = onOpenInfo)) {
                    Text(ex.exercise.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        MusclePill(MuscleGroup.fromName(ex.exercise.primaryMuscle))
                        if (ex.entry.repsMin != null) Pill("Meta ${ex.entry.repsMin}–${ex.entry.repsMax} reps")
                    }
                }
                Row(Modifier.clip(RoundedCornerShape(50)).clickable { restDialog = true }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, "Descanso", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(" ${ex.entry.restSec}s", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opções do exercício") }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Trocar exercício") }, onClick = { menu = false; onReplace() })
                        DropdownMenuItem(text = { Text("Iniciar descanso agora") }, onClick = { menu = false; vm.startRest(ex.entry.restSec, ex.exercise.name) })
                        DropdownMenuItem(text = { Text("Observação") }, onClick = { menu = false; notesDialog = true })
                        if (index > 0) DropdownMenuItem(text = { Text("Mover para cima") }, onClick = { menu = false; vm.moveExercise(index, index - 1) })
                        if (index < count - 1) DropdownMenuItem(text = { Text("Mover para baixo") }, onClick = { menu = false; vm.moveExercise(index, index + 1) })
                        DropdownMenuItem(text = { Text("Remover exercício", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; confirmRemove = true })
                    }
                }
            }
            if (ex.entry.notes.isNotBlank()) Text(ex.entry.notes, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            // Column headers
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderCell("SÉRIE", Modifier.width(44.dp))
                HeaderCell("ANTERIOR", Modifier.weight(1f))
                HeaderCell("KG", Modifier.width(72.dp))
                Spacer(Modifier.width(6.dp))
                HeaderCell("REPS", Modifier.width(60.dp))
                if (showRpe) { Spacer(Modifier.width(6.dp)); HeaderCell("RPE", Modifier.width(44.dp)) }
                Spacer(Modifier.width(8.dp))
                HeaderCell("", Modifier.width(48.dp))
            }
            var workIdx = 0; var warmIdx = 0
            ex.sets.forEach { set ->
                val prev = if (set.isWarmup) prevWarm.getOrNull(warmIdx) else prevWork.getOrNull(workIdx)
                val label = if (set.isWarmup) "A" else "${workIdx + 1}"
                if (set.isWarmup) warmIdx++ else workIdx++
                androidx.compose.runtime.key(set.id) {
                    SetRowUi(set, label, prev, ex.entry, showRpe, vm, ex.exercise.name)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { vm.addSet(ex.entry.id, false) }, modifier = Modifier.weight(1f).testTag("add_set_${ex.exercise.name}")) { Icon(Icons.Outlined.Add, null); Text(" Série") }
                TextButton(onClick = { vm.addSet(ex.entry.id, true) }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Add, null); Text(" Aquecimento") }
            }
        }
    }

    if (restDialog) RestPickerDialog(ex.entry.restSec, onDismiss = { restDialog = false }) { vm.updateEntry(ex.entry.copy(restSec = it)); restDialog = false }
    if (notesDialog) {
        var t by remember { mutableStateOf(ex.entry.notes) }
        AlertDialog(
            onDismissRequest = { notesDialog = false }, title = { Text("Observação") },
            text = { OutlinedTextField(t, { t = it }, modifier = Modifier.fillMaxWidth(), minLines = 3) },
            confirmButton = { TextButton(onClick = { vm.updateEntry(ex.entry.copy(notes = t)); notesDialog = false }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { notesDialog = false }) { Text("Cancelar") } },
        )
    }
    if (confirmRemove) ConfirmDialog("Remover ${ex.exercise.name}?", "As séries deste exercício nesta sessão serão apagadas.", "Remover", { vm.removeExercise(ex.entry.id) }, { confirmRemove = false }, destructive = true)
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) =
    Text(text, modifier, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

@Composable
private fun SetRowUi(set: SetEntity, label: String, prev: SetEntity?, entry: SessionExerciseEntity, showRpe: Boolean, vm: WorkoutViewModel, exerciseName: String) {
    val focus = LocalFocusManager.current
    var menu by remember { mutableStateOf(false) }
    var effortDialog by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (set.completed) Gt.colors.completedRow else Color.Transparent, label = "rowbg")
    var weightText by remember(set.id) { mutableStateOf(fmtNum(set.weight)) }
    var repsText by remember(set.id) { mutableStateOf(set.reps?.toString() ?: "") }
    LaunchedEffect(set.weight) { if (Format.parseDecimal(weightText) != set.weight) weightText = fmtNum(set.weight) }
    LaunchedEffect(set.reps) { if (repsText.toIntOrNull() != set.reps) repsText = set.reps?.toString() ?: "" }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp).clip(MaterialTheme.shapes.small).background(bg).padding(horizontal = 6.dp, vertical = 5.dp)
            .testTag("set_row_${exerciseName}_$label"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable { menu = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (set.isWarmup) Gt.colors.warmup else MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(if (set.isWarmup) "Marcar como série de trabalho" else "Marcar como aquecimento") }, onClick = { menu = false; vm.setWarmup(set.id, !set.isWarmup) })
                DropdownMenuItem(text = { Text("Registrar RPE / RIR") }, onClick = { menu = false; effortDialog = true })
                DropdownMenuItem(text = { Text("Excluir série", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; vm.deleteSet(set.id) })
            }
        }
        Text(
            prev?.let { "${fmtNum(it.weight)} × ${it.reps ?: 0}" + (it.rpe?.let { r -> " @${Format.weight(r)}" } ?: "") } ?: "—",
            Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable(enabled = prev != null && !set.completed) { prev?.let { vm.fill(set.id, it.weight, it.reps) } }.padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1,
        )
        NumberBox(
            value = weightText, placeholder = "kg", width = 72, decimal = true, completed = set.completed, tag = "weight_${exerciseName}_$label",
            onChange = { t -> weightText = t; val v = if (t.isBlank()) null else Format.parseDecimal(t); if (t.isBlank() || v != null) vm.setWeight(set.id, v) },
            imeAction = ImeAction.Next, onIme = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) },
        )
        Spacer(Modifier.width(6.dp))
        NumberBox(
            value = repsText, placeholder = entry.repsMax?.let { "${entry.repsMin ?: it}-$it" } ?: "reps", width = 60, decimal = false, completed = set.completed, tag = "reps_${exerciseName}_$label",
            onChange = { t -> val clean = t.filter { it.isDigit() }.take(4); repsText = clean; vm.setReps(set.id, clean.toIntOrNull()) },
            imeAction = ImeAction.Done, onIme = { focus.clearFocus() },
        )
        if (showRpe) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.width(44.dp).height(44.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable { effortDialog = true }, contentAlignment = Alignment.Center) {
                Text(set.rpe?.let { Format.weight(it) } ?: set.rir?.let { "R$it" } ?: "–", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.width(8.dp))
        val checkBg by animateColorAsState(if (set.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest, label = "check")
        Box(
            Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(checkBg)
                .clickable { focus.clearFocus(); vm.toggle(set, entry, exerciseName) }
                .semantics { contentDescription = if (set.completed) "Série $label concluída. Toque para desmarcar" else "Concluir série $label" }
                .testTag("check_${exerciseName}_$label"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, null, tint = if (set.completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (effortDialog) EffortDialog(set.rpe, set.rir, onDismiss = { effortDialog = false }) { rpe, rir -> vm.setEffort(set.id, rpe, rir); effortDialog = false }
}

@Composable
private fun NumberBox(
    value: String, placeholder: String, width: Int, decimal: Boolean, completed: Boolean, tag: String,
    onChange: (String) -> Unit, imeAction: ImeAction, onIme: () -> Unit,
) {
    val color = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = value,
        onValueChange = { onChange(it.take(7)) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onIme() }, onDone = { onIme() }),
        modifier = Modifier.width(width.dp).height(44.dp).testTag(tag),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.small)
                    .background(if (completed) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, if (completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                inner()
            }
        },
    )
}

@Composable
fun RestPickerDialog(current: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var custom by rememberSaveable { mutableStateOf(current.toString()) }
    val options = listOf(0, 30, 45, 60, 75, 90, 120, 150, 180, 240, 300)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tempo de descanso") },
        text = {
            Column {
                options.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { sec -> FilterChip(selected = sec == current, onClick = { onPick(sec) }, label = { Text(if (sec == 0) "Sem" else if (sec < 60) "${sec}s" else "${sec / 60}:${"%02d".format(sec % 60)}") }) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(custom, { custom = it.filter(Char::isDigit).take(3) }, label = { Text("Personalizado (segundos)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { TextButton(onClick = { custom.toIntOrNull()?.let { onPick(it.coerceIn(0, 900)) } }) { Text("Definir") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun EffortDialog(rpe: Double?, rir: Int?, onDismiss: () -> Unit, onSave: (Double?, Int?) -> Unit) {
    var selRpe by remember { mutableStateOf(rpe) }
    var selRir by remember { mutableStateOf(rir) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Esforço da série") },
        text = {
            Column {
                Text("RPE (percepção de esforço, 6–10)", style = MaterialTheme.typography.labelMedium)
                listOf(6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0).chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { row.forEach { v -> FilterChip(selected = selRpe == v, onClick = { selRpe = if (selRpe == v) null else v }, label = { Text(Format.weight(v)) }) } }
                }
                Spacer(Modifier.height(10.dp))
                Text("RIR (repetições em reserva)", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { (0..5).forEach { v -> FilterChip(selected = selRir == v, onClick = { selRir = if (selRir == v) null else v }, label = { Text(if (v == 5) "5+" else "$v") }) } }
                Spacer(Modifier.height(6.dp))
                Text("Ambos são opcionais.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selRpe, selRir) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { onSave(null, null) }) { Text("Limpar") } },
    )
}
