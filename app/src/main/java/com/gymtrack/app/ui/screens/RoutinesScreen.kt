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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.gymtrack.app.AppContainer
import com.gymtrack.app.data.db.RoutineSummary
import com.gymtrack.app.data.seed.ProgramTemplate
import com.gymtrack.app.data.seed.ProgramTemplates
import com.gymtrack.app.ui.Routes
import com.gymtrack.app.ui.components.ConfirmDialog
import com.gymtrack.app.ui.components.EmptyState
import com.gymtrack.app.ui.components.GtCard
import com.gymtrack.app.ui.components.GtScaffold
import com.gymtrack.app.ui.components.SectionHeader
import com.gymtrack.app.ui.components.appViewModel
import com.gymtrack.app.ui.components.fmtDayMonth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutinesViewModel(private val c: AppContainer) : ViewModel() {
    val routines = c.workouts.observeRoutines().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    /** Creates the next lettered routine ("Treino A", "Treino B", …) and opens it for editing. */
    fun createNext(onCreated: (Long) -> Unit) = viewModelScope.launch {
        val names = routines.value.orEmpty().map { it.name }.toSet()
        val name = ('A'..'Z').map { "Treino $it" }.firstOrNull { it !in names } ?: "Novo treino"
        onCreated(c.workouts.createRoutine(name))
    }
    fun duplicate(id: Long) = viewModelScope.launch { c.workouts.duplicateRoutine(id) }
    fun delete(id: Long) = viewModelScope.launch { c.workouts.deleteRoutine(id) }
    fun import(t: ProgramTemplate) = viewModelScope.launch { c.workouts.importTemplate(t) }
}

@Composable
fun RoutinesScreen(nav: NavController) {
    val vm = appViewModel { RoutinesViewModel(it) }
    val routines by vm.routines.collectAsState()
    val start = rememberStartWorkout(nav)
    var templates by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<RoutineSummary?>(null) }

    GtScaffold(
        title = "Treinos", isTab = true,
        actions = { IconButton(onClick = { templates = true }, modifier = Modifier.testTag("templates_button")) { Icon(Icons.Outlined.AutoAwesome, "Modelos de treino") } },
        fab = { ExtendedFloatingActionButton(onClick = { vm.createNext { nav.navigate(Routes.routine(it)) } }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Nova ficha") }, modifier = Modifier.testTag("new_routine")) },
    ) { pad ->
        val list = routines
        if (list != null && list.isEmpty()) {
            Column(Modifier.padding(pad)) {
                EmptyState(Icons.AutoMirrored.Outlined.ListAlt, "Nenhuma ficha ainda", "Crie uma ficha do zero (A, B, C, Push, Pull…) ou importe um modelo editável.", actionLabel = "Ver modelos", onAction = { templates = true })
            }
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            list.orEmpty().groupBy { it.program.ifBlank { "Minhas fichas" } }.forEach { (program, items) ->
                item(key = "h_$program") { SectionHeader(program) }
                items(items, key = { it.id }) { r ->
                    RoutineCard(r, onOpen = { nav.navigate(Routes.routine(r.id)) }, onStart = { start(r.id) }, onDuplicate = { vm.duplicate(r.id) }, onDelete = { deleting = r })
                }
            }
        }
    }

    if (templates) AlertDialog(
        onDismissRequest = { templates = false },
        title = { Text("Modelos de treino") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Importar cria fichas normais, que você pode editar livremente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(ProgramTemplates.all) { t ->
                    GtCard(onClick = { vm.import(t); templates = false }, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.testTag("template_${t.id}")) {
                        Text(t.name, style = MaterialTheme.typography.titleMedium)
                        Text(t.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(t.days.joinToString(" · ") { it.name }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { templates = false }) { Text("Fechar") } },
    )
    deleting?.let { r -> ConfirmDialog("Excluir \"${r.name}\"?", "A ficha será removida. O histórico de treinos já realizados é mantido.", "Excluir", { vm.delete(r.id) }, { deleting = null }, destructive = true) }
}

@Composable
private fun RoutineCard(r: RoutineSummary, onOpen: () -> Unit, onStart: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    GtCard(onClick = onOpen, modifier = Modifier.testTag("routine_${r.name}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${r.exerciseCount} exercícios · ${r.totalSets} séries" + (r.lastPerformed?.let { " · ${fmtDayMonth(it)}" } ?: " · nunca realizado"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (r.notes.isNotBlank()) Text(r.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opções") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Editar") }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Duplicar") }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Excluir", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() })
                }
            }
            Spacer(Modifier.width(4.dp))
            FilledIconButton(onClick = onStart, enabled = r.exerciseCount > 0, modifier = Modifier.testTag("start_${r.name}")) { Icon(Icons.Filled.PlayArrow, "Iniciar ${r.name}") }
        }
    }
}
