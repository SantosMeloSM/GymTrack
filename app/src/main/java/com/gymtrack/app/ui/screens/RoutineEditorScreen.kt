package com.gymtrack.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.gymtrack.app.AppContainer
import com.gymtrack.app.data.db.ExerciseEntity
import com.gymtrack.app.data.db.RoutineEntity
import com.gymtrack.app.data.db.RoutineExerciseEntity
import com.gymtrack.app.data.db.RoutineVersionEntity
import com.gymtrack.app.ui.Routes
import com.gymtrack.app.ui.components.BigButton
import com.gymtrack.app.ui.components.ConfirmDialog
import com.gymtrack.app.ui.components.ExercisePickerDialog
import com.gymtrack.app.ui.components.GtScaffold
import com.gymtrack.app.ui.components.MusclePill
import com.gymtrack.app.ui.components.TextInputDialog
import com.gymtrack.app.ui.components.appViewModel
import com.gymtrack.app.ui.components.fmtDate
import com.gymtrack.domain.Equipment
import com.gymtrack.domain.Format
import com.gymtrack.domain.MuscleGroup
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutineEditorViewModel(private val c: AppContainer, val routineId: Long) : ViewModel() {
    private val repo = c.workouts
    val routine = repo.observeRoutine(routineId).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val items = repo.observeRoutineExercises(routineId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val exercises = repo.observeExercises().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val versions = repo.observeVersions(routineId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val messages = Channel<String>(Channel.BUFFERED)
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init { viewModelScope.launch { for (w in writes) runCatching { w() } } }
    private fun enqueue(b: suspend () -> Unit) { writes.trySend(b) }

    fun updateRoutine(r: RoutineEntity) = enqueue { repo.updateRoutine(r) }
    fun update(e: RoutineExerciseEntity) = enqueue { repo.updateRoutineExercise(e) }
    fun remove(e: RoutineExerciseEntity) = enqueue { repo.removeRoutineExercise(e) }
    fun move(from: Int, to: Int) = enqueue { repo.moveRoutineExercise(routineId, from, to) }
    fun add(ids: List<Long>) = enqueue { repo.addExercisesToRoutine(routineId, ids) }
    fun saveVersion(label: String) = enqueue { repo.saveVersion(routineId, label); messages.trySend("Versão salva") }
    fun restore(v: RoutineVersionEntity) = enqueue { repo.restoreVersion(v); messages.trySend("Versão \"${v.label}\" restaurada") }
    fun deleteVersion(v: RoutineVersionEntity) = enqueue { repo.deleteVersion(v.id) }
    fun duplicate(onDone: (Long) -> Unit) = enqueue { onDone(repo.duplicateRoutine(routineId)) }
    fun delete(onDone: () -> Unit) = enqueue { repo.deleteRoutine(routineId); onDone() }
    fun createExercise(name: String, muscle: MuscleGroup, eq: Equipment, done: (Long) -> Unit) {
        viewModelScope.launch { done(repo.saveExercise(ExerciseEntity(name = name, primaryMuscle = muscle.name, equipment = eq.name, isCustom = true, defaultRestSec = c.settings.current().defaultRestSec))) }
    }
}

@Composable
fun RoutineEditorScreen(nav: NavController, routineId: Long) {
    val vm = appViewModel(key = "routine_$routineId") { RoutineEditorViewModel(it, routineId) }
    val routine by vm.routine.collectAsState()
    val items by vm.items.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val versions by vm.versions.collectAsState()
    val start = rememberStartWorkout(nav)
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    LaunchedEffect(Unit) { for (m in vm.messages) snackbar.showSnackbar(m) }
    var picker by remember { mutableStateOf(false) }
    var versionsDialog by remember { mutableStateOf(false) }
    var saveVersionDialog by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val exMap = exercises.associateBy { it.id }

    val r = routine ?: run { GtScaffold("Ficha", onBack = { nav.popBackStack() }) {}; return }
    var name by remember(r.id) { mutableStateOf(r.name) }
    var program by remember(r.id) { mutableStateOf(r.program) }
    var notes by remember(r.id) { mutableStateOf(r.notes) }
    LaunchedEffect(r.name, r.program, r.notes) {
        // Sync after a version restore.
        if (r.name != name.trim().ifBlank { r.name }) name = r.name
        if (r.program != program.trim()) program = r.program
        if (r.notes != notes) notes = r.notes
    }

    GtScaffold(
        title = "Editar ficha", onBack = { nav.popBackStack() }, snackbar = snackbar,
        actions = {
            IconButton(onClick = { versionsDialog = true }) { Icon(Icons.Outlined.History, "Versões") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Mais opções") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Salvar versão") }, onClick = { menu = false; saveVersionDialog = true })
                    DropdownMenuItem(text = { Text("Duplicar ficha") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { menu = false; vm.duplicate { nav.navigate(Routes.routine(it)) { popUpTo(Routes.routine(routineId)) { inclusive = true } } } })
                    DropdownMenuItem(text = { Text("Excluir ficha", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; confirmDelete = true })
                }
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                BigButton("Iniciar este treino", { start(routineId) }, Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).testTag("start_routine"), icon = Icons.Filled.PlayArrow, enabled = items.isNotEmpty())
            }
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 4.dp, bottom = pad.calculateBottomPadding() + 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                OutlinedTextField(name, { name = it; vm.updateRoutine(r.copy(name = it.trim().ifBlank { r.name }, program = program.trim(), notes = notes)) }, label = { Text("Nome da ficha") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("edit_routine_name"))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(program, { program = it; vm.updateRoutine(r.copy(name = name.trim().ifBlank { r.name }, program = it.trim(), notes = notes)) }, label = { Text("Divisão (ex.: PPL)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(notes, { notes = it; vm.updateRoutine(r.copy(name = name.trim().ifBlank { r.name }, program = program.trim(), notes = it)) }, label = { Text("Observações / objetivo da ficha") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
            item {
                Text("${items.size} exercícios · ${items.sumOf { it.sets }} séries", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            itemsIndexed(items, key = { _, it -> it.id }) { idx, it ->
                val ex = exMap[it.exerciseId]
                RoutineExerciseCard(it, ex, idx, items.size, vm, onInfo = { ex?.let { e -> nav.navigate(Routes.exercise(e.id)) } })
            }
            item {
                FilledTonalButton(onClick = { picker = true }, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("routine_add_exercise"), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar exercícios", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (picker) ExercisePickerDialog(exercises, multi = true, onDismiss = { picker = false }, onPicked = { vm.add(it); picker = false }, onCreate = vm::createExercise)
    if (saveVersionDialog) TextInputDialog("Salvar versão", "Versão de ${fmtDate(System.currentTimeMillis())}", "Nome da versão", { vm.saveVersion(it) }, { saveVersionDialog = false })
    if (confirmDelete) ConfirmDialog("Excluir ficha?", "O histórico de treinos realizados com ela é mantido.", "Excluir", { vm.delete { nav.popBackStack() } }, { confirmDelete = false }, destructive = true)
    if (versionsDialog) {
        var restoring by remember { mutableStateOf<RoutineVersionEntity?>(null) }
        AlertDialog(
            onDismissRequest = { versionsDialog = false },
            title = { Text("Versões da ficha") },
            text = {
                LazyColumn {
                    item { Text("Salve versões para guardar o estado atual e restaurar depois.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (versions.isEmpty()) item { Text("Nenhuma versão salva.", Modifier.padding(vertical = 16.dp)) }
                    items(versions, key = { it.id }) { v ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(v.label, style = MaterialTheme.typography.titleSmall)
                                Text(fmtDate(v.createdAt, "dd/MM/yyyy HH:mm"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { restoring = v }) { Text("Restaurar") }
                            IconButton(onClick = { vm.deleteVersion(v) }) { Icon(Icons.Outlined.Remove, "Excluir versão") }
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { saveVersionDialog = true }) { Text("Salvar versão atual") } },
            dismissButton = { TextButton(onClick = { versionsDialog = false }) { Text("Fechar") } },
        )
        restoring?.let { v -> ConfirmDialog("Restaurar \"${v.label}\"?", "O estado atual será salvo automaticamente como uma nova versão antes da restauração.", "Restaurar", { vm.restore(v); versionsDialog = false }, { restoring = null }) }
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 99, tag: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { if (value > min) onChange(value - 1) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Remove, "Diminuir $label") }
            Text("$value", style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(36.dp).then(if (tag != null) Modifier.testTag(tag) else Modifier), textAlign = TextAlign.Center)
            FilledTonalIconButton(onClick = { if (value < max) onChange(value + 1) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Add, "Aumentar $label") }
        }
    }
}

@Composable
private fun RoutineExerciseCard(e: RoutineExerciseEntity, ex: ExerciseEntity?, index: Int, count: Int, vm: RoutineEditorViewModel, onInfo: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var restDialog by remember { mutableStateOf(false) }
    var repsMin by remember(e.id) { mutableStateOf(e.repsMin.toString()) }
    var repsMax by remember(e.id) { mutableStateOf(e.repsMax.toString()) }
    var weight by remember(e.id) { mutableStateOf(e.plannedWeight?.let { Format.weight(it) } ?: "") }
    var goal by remember(e.id) { mutableStateOf(e.goal) }
    var notes by remember(e.id) { mutableStateOf(e.notes) }
    var expanded by remember(e.id) { mutableStateOf(false) }

    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.testTag("routine_item_${ex?.name}")) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
                if (ex != null) { com.gymtrack.app.ui.components.ExerciseThumb(ex, width = 60.dp, onClick = onInfo); Spacer(Modifier.width(10.dp)) }
                Column(Modifier.weight(1f).clickable(onClick = onInfo)) {
                    Text(ex?.name ?: "Exercício removido", style = MaterialTheme.typography.titleMedium)
                    MusclePill(MuscleGroup.fromName(ex?.primaryMuscle), Modifier.padding(top = 4.dp))
                }
                IconButton(onClick = { vm.move(index, index - 1) }, enabled = index > 0) { Icon(Icons.Filled.KeyboardArrowUp, "Mover para cima") }
                IconButton(onClick = { vm.move(index, index + 1) }, enabled = index < count - 1) { Icon(Icons.Filled.KeyboardArrowDown, "Mover para baixo") }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opções") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(if (expanded) "Ocultar detalhes" else "Meta e observações") }, onClick = { menu = false; expanded = !expanded })
                        DropdownMenuItem(text = { Text("Remover da ficha", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; vm.remove(e) })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stepper("Séries", e.sets, { vm.update(e.copy(sets = it)) }, min = 1, max = 20, tag = "sets_${ex?.name}")
                Stepper("Aquecimento", e.warmupSets, { vm.update(e.copy(warmupSets = it)) }, max = 10)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(repsMin, { v -> repsMin = v.filter(Char::isDigit).take(3); repsMin.toIntOrNull()?.let { vm.update(e.copy(repsMin = it, repsMax = maxOf(it, repsMax.toIntOrNull() ?: it))) } }, label = { Text("Reps mín.") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(repsMax, { v -> repsMax = v.filter(Char::isDigit).take(3); repsMax.toIntOrNull()?.let { vm.update(e.copy(repsMax = maxOf(it, e.repsMin))) } }, label = { Text("Reps máx.") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(weight, { v -> weight = v.take(6); vm.update(e.copy(plannedWeight = if (v.isBlank()) null else Format.parseDecimal(v) ?: e.plannedWeight)) }, label = { Text("Carga (kg)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1.1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.clickable { restDialog = true }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" Descanso: ${if (e.restSec == 0) "sem" else "${e.restSec / 60}:${"%02d".format(e.restSec % 60)}"}", style = MaterialTheme.typography.bodyMedium)
                Text("  alterar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            if (!expanded && (e.goal.isNotBlank() || e.notes.isNotBlank())) {
                if (e.goal.isNotBlank()) Text("Meta: ${e.goal}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                if (e.notes.isNotBlank()) Text(e.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded) {
                OutlinedTextField(goal, { goal = it; vm.update(e.copy(goal = it)) }, label = { Text("Meta (ex.: 100 kg × 8)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(notes, { notes = it; vm.update(e.copy(notes = it)) }, label = { Text("Observações (aparecem no treino)") }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (restDialog) RestPickerDialog(e.restSec, { restDialog = false }) { vm.update(e.copy(restSec = it)); restDialog = false }
}
