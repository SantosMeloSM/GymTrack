package com.gymtrack.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.gymtrack.app.AppContainer
import com.gymtrack.app.data.db.BodyMeasurementEntity
import com.gymtrack.app.data.db.ProgressPhotoEntity
import com.gymtrack.app.ui.components.ChartPoint
import com.gymtrack.app.ui.components.ConfirmDialog
import com.gymtrack.app.ui.components.EmptyState
import com.gymtrack.app.ui.components.GtCard
import com.gymtrack.app.ui.components.GtScaffold
import com.gymtrack.app.ui.components.LineChart
import com.gymtrack.app.ui.components.StatRow
import com.gymtrack.app.ui.components.StatTile
import com.gymtrack.app.ui.components.appViewModel
import com.gymtrack.app.ui.components.fmtDate
import com.gymtrack.app.ui.components.fmtDayMonth
import com.gymtrack.domain.Format
import java.io.File
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Measurement fields (label, unit, getter, setter). */
enum class Measure(val label: String, val unit: String, val get: (BodyMeasurementEntity) -> Double?, val set: (BodyMeasurementEntity, Double?) -> BodyMeasurementEntity) {
    WEIGHT("Peso", "kg", { it.weight }, { m, v -> m.copy(weight = v) }),
    HEIGHT("Altura", "cm", { it.height }, { m, v -> m.copy(height = v) }),
    WAIST("Cintura", "cm", { it.waist }, { m, v -> m.copy(waist = v) }),
    CHEST("Peitoral", "cm", { it.chest }, { m, v -> m.copy(chest = v) }),
    ARM_R("Braço direito", "cm", { it.armRight }, { m, v -> m.copy(armRight = v) }),
    ARM_L("Braço esquerdo", "cm", { it.armLeft }, { m, v -> m.copy(armLeft = v) }),
    THIGH_R("Coxa direita", "cm", { it.thighRight }, { m, v -> m.copy(thighRight = v) }),
    THIGH_L("Coxa esquerda", "cm", { it.thighLeft }, { m, v -> m.copy(thighLeft = v) }),
    HIPS("Quadril", "cm", { it.hips }, { m, v -> m.copy(hips = v) }),
    CALVES("Panturrilha", "cm", { it.calves }, { m, v -> m.copy(calves = v) }),
    SHOULDERS("Ombros", "cm", { it.shoulders }, { m, v -> m.copy(shoulders = v) }),
    NECK("Pescoço", "cm", { it.neck }, { m, v -> m.copy(neck = v) }),
    FOREARM("Antebraço", "cm", { it.forearm }, { m, v -> m.copy(forearm = v) }),
}

class BodyViewModel(private val c: AppContainer) : ViewModel() {
    val measurements = c.body.observeMeasurements().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val photos = c.body.observePhotos().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val messages = Channel<String>(Channel.BUFFERED)
    fun photoFile(p: ProgressPhotoEntity): File = c.body.photoFile(p)
    fun save(m: BodyMeasurementEntity) = viewModelScope.launch { c.body.saveMeasurement(m) }
    fun delete(id: Long) = viewModelScope.launch { c.body.deleteMeasurement(id) }
    fun addPhoto(uri: android.net.Uri, pose: String) = viewModelScope.launch {
        runCatching { c.body.addPhoto(uri, System.currentTimeMillis(), pose, "") }.onFailure { messages.trySend(it.message ?: "Erro ao importar a foto") }
    }
    fun deletePhoto(p: ProgressPhotoEntity) = viewModelScope.launch { c.body.deletePhoto(p) }
}

@Composable
fun BodyScreen(nav: NavController) {
    val vm = appViewModel { BodyViewModel(it) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var editing by remember { mutableStateOf<BodyMeasurementEntity?>(null) }
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    LaunchedEffect(Unit) { for (m in vm.messages) snackbar.showSnackbar(m) }
    var pose by remember { mutableStateOf("Frente") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.addPhoto(it, pose) } }
    var choosePose by remember { mutableStateOf(false) }

    GtScaffold("Evolução corporal", onBack = { nav.popBackStack() }, snackbar = snackbar, fab = {
        if (tab == 0) ExtendedFloatingActionButton(onClick = { editing = BodyMeasurementEntity(date = System.currentTimeMillis()) }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Nova medição") }, modifier = Modifier.testTag("add_measurement"))
        else ExtendedFloatingActionButton(onClick = { choosePose = true }, icon = { Icon(Icons.Outlined.AddAPhoto, null) }, text = { Text("Adicionar foto") })
    }) { pad ->
        Column(Modifier.padding(top = pad.calculateTopPadding()).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                listOf("Medidas", "Fotos").forEachIndexed { i, l -> SegmentedButton(selected = tab == i, onClick = { tab = i }, shape = SegmentedButtonDefaults.itemShape(i, 2), label = { Text(l) }) }
            }
            Spacer(Modifier.height(8.dp))
            if (tab == 0) MeasurementsTab(vm) { editing = it } else PhotosTab(vm)
        }
    }
    editing?.let { m -> MeasurementEditor(m, onDismiss = { editing = null }, onSave = { vm.save(it); editing = null }, onDelete = if (m.id != 0L) ({ vm.delete(m.id); editing = null }) else null) }
    if (choosePose) AlertDialog(
        onDismissRequest = { choosePose = false },
        title = { Text("Tipo de foto") },
        text = { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Frente", "Lado", "Costas", "Outra").forEach { p -> FilterChip(selected = pose == p, onClick = { pose = p }, label = { Text(p) }) } } },
        confirmButton = { TextButton(onClick = { choosePose = false; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Escolher imagem") } },
        dismissButton = { TextButton(onClick = { choosePose = false }) { Text("Cancelar") } },
    )
}

private enum class BodyPeriod(val label: String, val days: Long) { D30("30 dias", 30), D90("90 dias", 90), Y1("1 ano", 365), ALL("Tudo", 365L * 100) }

@Composable
private fun MeasurementsTab(vm: BodyViewModel, onEdit: (BodyMeasurementEntity) -> Unit) {
    val list = vm.measurements.collectAsState().value ?: return
    var measure by rememberSaveable { mutableStateOf(Measure.WEIGHT) }
    var period by rememberSaveable { mutableStateOf(BodyPeriod.D90) }
    if (list.isEmpty()) { EmptyState(Icons.Outlined.MonitorWeight, "Nenhuma medição", "Registre peso e medidas (todas opcionais) para acompanhar sua evolução ao longo do tempo."); return }
    val available = Measure.entries.filter { m -> list.any { m.get(it) != null } }
    val sel = if (measure in available) measure else available.firstOrNull() ?: Measure.WEIGHT
    val from = System.currentTimeMillis() - period.days * 86_400_000L
    val series = list.filter { sel.get(it) != null && it.date >= from }.sortedBy { it.date }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(available) { m -> FilterChip(selected = sel == m, onClick = { measure = m }, label = { Text(m.label) }) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { BodyPeriod.entries.forEach { p -> FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) }) } } }
        item {
            if (series.isNotEmpty()) {
                val first = sel.get(series.first())!!; val last = sel.get(series.last())!!
                val delta = last - first
                StatRow {
                    StatTile("Início do período", "${Format.weight(first)} ${sel.unit}", Modifier.weight(1f), sub = fmtDate(series.first().date))
                    StatTile("Atual", "${Format.weight(last)} ${sel.unit}", Modifier.weight(1f), sub = fmtDate(series.last().date), accent = true)
                    StatTile("Variação", (if (delta > 0) "+" else if (delta < 0) "−" else "") + "${Format.weight(kotlin.math.abs(Math.round(delta * 10) / 10.0))} ${sel.unit}", Modifier.weight(1f), sub = "${series.size} registros")
                }
            } else Text("Sem registros de ${sel.label.lowercase()} neste período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (series.size >= 2) item {
            GtCard {
                Text("${sel.label} (${sel.unit})", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LineChart(series.map { ChartPoint(sel.get(it)!!.toFloat(), fmtDayMonth(it.date), "${fmtDayMonth(it.date)} · ${Format.weight(sel.get(it))} ${sel.unit}") }, color = MaterialTheme.colorScheme.secondary, description = sel.label)
            }
        }
        items(list, key = { it.id }) { m ->
            GtCard(onClick = { onEdit(m) }, contentPadding = PaddingValues(14.dp)) {
                Text(fmtDate(m.date), style = MaterialTheme.typography.titleSmall)
                Text(Measure.entries.mapNotNull { f -> f.get(m)?.let { "${f.label}: ${Format.weight(it)} ${f.unit}" } }.joinToString("  ·  ").ifEmpty { "—" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (m.other.isNotBlank()) Text(m.other.lines().joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (m.notes.isNotBlank()) Text(m.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MeasurementEditor(initial: BodyMeasurementEntity, onDismiss: () -> Unit, onSave: (BodyMeasurementEntity) -> Unit, onDelete: (() -> Unit)?) {
    val values = remember { mutableStateListOf<String>().apply { addAll(Measure.entries.map { m -> m.get(initial)?.let { Format.weight(it) } ?: "" }) } }
    var other by remember { mutableStateOf(initial.other) }
    var notes by remember { mutableStateOf(initial.notes) }
    var dateText by remember { mutableStateOf(fmtDate(initial.date)) }
    var confirmDelete by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding().imePadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Fechar") }
                    Text(if (initial.id == 0L) "Nova medição" else "Editar medição", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (onDelete != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Excluir") }
                    TextButton(onClick = {
                        val date = runCatching { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).apply { isLenient = false }.parse(dateText)!!.time }.getOrNull() ?: initial.date
                        var m = initial.copy(date = date, other = other.trim(), notes = notes.trim())
                        Measure.entries.forEachIndexed { i, f -> m = f.set(m, Format.parseDecimal(values[i])?.takeIf { it > 0 }) }
                        onSave(m)
                    }, modifier = Modifier.testTag("save_measurement")) { Text("Salvar", fontWeight = FontWeight.Bold) }
                }
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text("Todos os campos são opcionais. Medidas em cm, peso em kg.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    item { OutlinedTextField(dateText, { dateText = it.take(10) }, label = { Text("Data (dd/mm/aaaa)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                    Measure.entries.chunked(2).forEach { pair ->
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { f ->
                                    val i = f.ordinal
                                    OutlinedTextField(values[i], { values[i] = it.take(6) }, label = { Text("${f.label} (${f.unit})") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).testTag("measure_${f.name}"))
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    item { OutlinedTextField(other, { other = it }, label = { Text("Outras medidas (uma por linha, ex.: Abdômen=82)") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(notes, { notes = it }, label = { Text("Observações") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                }
            }
        }
    }
    if (confirmDelete && onDelete != null) ConfirmDialog("Excluir medição?", "Esta medição será removida.", "Excluir", onDelete, { confirmDelete = false }, destructive = true)
}

@Composable
private fun PhotosTab(vm: BodyViewModel) {
    val photos by vm.photos.collectAsState()
    var viewing by remember { mutableStateOf<ProgressPhotoEntity?>(null) }
    var compareMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<ProgressPhotoEntity>() }
    var comparing by remember { mutableStateOf(false) }
    if (photos.isEmpty()) { EmptyState(Icons.Outlined.AddAPhoto, "Nenhuma foto", "Adicione fotos de evolução. Elas ficam apenas no armazenamento privado do app, neste aparelho."); return }
    Column {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (compareMode) "Selecione 2 fotos (${selected.size}/2)" else "${photos.size} fotos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            TextButton(onClick = { compareMode = !compareMode; selected.clear() }) { Icon(Icons.Outlined.CompareArrows, null); Text(if (compareMode) " Cancelar" else " Comparar") }
        }
        LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(photos, key = { it.id }) { p ->
                val isSel = p in selected
                Box(Modifier.aspectRatio(0.75f).clip(MaterialTheme.shapes.small).then(if (isSel) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small) else Modifier).clickable {
                    if (compareMode) {
                        if (isSel) selected.remove(p) else if (selected.size < 2) selected.add(p)
                        if (selected.size == 2) comparing = true
                    } else viewing = p
                }) {
                    AsyncImage(model = vm.photoFile(p), contentDescription = "Foto ${p.pose} de ${fmtDate(p.date)}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Text("${fmtDayMonth(p.date)} · ${p.pose}", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(6.dp))
                }
            }
        }
    }
    viewing?.let { p ->
        var confirm by remember { mutableStateOf(false) }
        Dialog(onDismissRequest = { viewing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Black) {
                Box {
                    AsyncImage(model = vm.photoFile(p), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewing = null }) { Icon(Icons.Filled.Close, "Fechar", tint = androidx.compose.ui.graphics.Color.White) }
                        Text("${fmtDate(p.date)} · ${p.pose}", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { confirm = true }) { Icon(Icons.Outlined.Delete, "Excluir foto", tint = androidx.compose.ui.graphics.Color.White) }
                    }
                }
            }
        }
        if (confirm) ConfirmDialog("Excluir foto?", "A foto será apagada do aparelho.", "Excluir", { vm.deletePhoto(p); viewing = null }, { confirm = false }, destructive = true)
    }
    if (comparing && selected.size == 2) {
        val (a, b) = selected.sortedBy { it.date }
        Dialog(onDismissRequest = { comparing = false; selected.clear(); compareMode = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Black) {
                Column(Modifier.statusBarsPadding()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { comparing = false; selected.clear(); compareMode = false }) { Icon(Icons.Filled.Close, "Fechar", tint = androidx.compose.ui.graphics.Color.White) }
                        Text("Comparação · ${((b.date - a.date) / 86_400_000L)} dias de diferença", color = androidx.compose.ui.graphics.Color.White)
                    }
                    Row(Modifier.fillMaxSize().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(a, b).forEach { p ->
                            Column(Modifier.weight(1f)) {
                                AsyncImage(model = vm.photoFile(p), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.weight(1f).fillMaxWidth())
                                Text("${fmtDate(p.date)} · ${p.pose}", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
