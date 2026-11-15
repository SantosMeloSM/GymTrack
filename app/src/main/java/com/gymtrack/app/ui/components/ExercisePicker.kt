package com.gymtrack.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gymtrack.app.data.db.ExerciseEntity
import com.gymtrack.domain.Equipment
import com.gymtrack.domain.MuscleGroup
import java.text.Normalizer

fun String.normalized(): String = Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

fun List<ExerciseEntity>.filterExercises(query: String, muscle: MuscleGroup?, favoritesOnly: Boolean): List<ExerciseEntity> {
    val q = query.trim().normalized()
    return filter { e ->
        (muscle == null || e.primaryMuscle == muscle.name || e.secondaryMuscles.split(',').contains(muscle.name)) &&
            (!favoritesOnly || e.isFavorite) &&
            (q.isEmpty() || q.split(' ').all { token -> e.name.normalized().contains(token) || (MuscleGroup.fromName(e.primaryMuscle)?.label?.normalized()?.contains(token) == true) })
    }.sortedWith(compareByDescending<ExerciseEntity> { it.isFavorite }.thenBy { if (muscle != null && it.primaryMuscle != muscle.name) 1 else 0 }.thenBy { it.name.normalized() })
}

@Composable
fun MuscleFilterRow(selected: MuscleGroup?, onSelect: (MuscleGroup?) -> Unit, favoritesOnly: Boolean, onFavorites: (Boolean) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        item { FilterChip(selected = favoritesOnly, onClick = { onFavorites(!favoritesOnly) }, label = { Text("Favoritos") }, leadingIcon = { Icon(Icons.Filled.Star, null, Modifier.size(16.dp)) }) }
        item { FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Todos") }) }
        items(MuscleGroup.entries) { m -> FilterChip(selected = selected == m, onClick = { onSelect(if (selected == m) null else m) }, label = { Text(m.label) }) }
    }
}

/** Full-screen searchable exercise picker with optional multi-select and quick creation. */
@Composable
fun ExercisePickerDialog(
    exercises: List<ExerciseEntity>,
    multi: Boolean,
    title: String = if (multi) "Adicionar exercícios" else "Escolher exercício",
    onDismiss: () -> Unit,
    onPicked: (List<Long>) -> Unit,
    onCreate: (name: String, muscle: MuscleGroup, equipment: Equipment, done: (Long) -> Unit) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<MuscleGroup?>(null) }
    var fav by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }
    var creating by remember { mutableStateOf(false) }
    val list = remember(exercises, query, muscle, fav) { exercises.filterExercises(query, muscle, fav) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Fechar") }
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { creating = true }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(4.dp)); Text("Novo") }
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("Buscar exercício") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Limpar") } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("picker_search"),
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.size(8.dp))
                MuscleFilterRow(muscle, { muscle = it }, fav, { fav = it })
                Spacer(Modifier.size(4.dp))
                LazyColumn(Modifier.weight(1f)) {
                    if (list.isEmpty()) item {
                        Text("Nenhum exercício encontrado. Toque em \"Novo\" para criar.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(list, key = { it.id }) { e ->
                        val isSel = e.id in selected
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (multi) { if (isSel) selected.remove(e.id) else selected.add(e.id) } else onPicked(listOf(e.id))
                            }.padding(horizontal = 16.dp, vertical = 12.dp).testTag("pick_${e.name}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExerciseThumb(e, width = 66.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(e.name, style = MaterialTheme.typography.titleMedium)
                                    if (e.isFavorite) { Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.Star, "Favorito", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary) }
                                }
                                Text("${MuscleGroup.fromName(e.primaryMuscle)?.label ?: ""} · ${Equipment.fromName(e.equipment).label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (multi) Icon(if (isSel) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                if (multi) {
                    BigButton(
                        text = if (selected.isEmpty()) "Selecione exercícios" else "Adicionar ${selected.size}",
                        onClick = { onPicked(selected.toList()) }, enabled = selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("picker_confirm"),
                    )
                }
            }
        }
    }
    if (creating) QuickCreateExerciseDialog(initialName = query, onDismiss = { creating = false }) { name, m, eq ->
        onCreate(name, m, eq) { id -> creating = false; if (multi) selected.add(id) else onPicked(listOf(id)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EnumDropdown(label: String, value: T, options: List<T>, display: (T) -> String, onChange: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = display(value), onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(display(o)) }, onClick = { onChange(o); open = false }) }
        }
    }
}

@Composable
fun QuickCreateExerciseDialog(initialName: String, onDismiss: () -> Unit, onSave: (String, MuscleGroup, Equipment) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var muscle by remember { mutableStateOf(MuscleGroup.CHEST) }
    var eq by remember { mutableStateOf(Equipment.MACHINE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo exercício") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("quick_name"))
                EnumDropdown("Grupo muscular", muscle, MuscleGroup.entries, { it.label }, { muscle = it })
                EnumDropdown("Equipamento", eq, Equipment.entries, { it.label }, { eq = it })
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), muscle, eq) }) { Text("Criar", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
