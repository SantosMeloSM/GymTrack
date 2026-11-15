package com.gymtrack.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.gymtrack.app.AppContainer
import com.gymtrack.app.data.db.ExerciseEntity
import com.gymtrack.app.data.repo.Analytics
import com.gymtrack.app.data.repo.AppSettings
import com.gymtrack.app.ui.Routes
import com.gymtrack.app.ui.components.ChartPoint
import com.gymtrack.app.ui.components.ConfirmDialog
import com.gymtrack.app.ui.components.EmptyState
import com.gymtrack.app.ui.components.EnumDropdown
import com.gymtrack.app.ui.components.ExerciseThumb
import com.gymtrack.app.ui.components.ExerciseDemoView
import com.gymtrack.app.ui.components.DemoFullscreen
import com.gymtrack.app.ui.components.rememberDemo
import com.gymtrack.app.ui.components.GtCard
import com.gymtrack.app.ui.components.GtScaffold
import com.gymtrack.app.ui.components.LineChart
import com.gymtrack.app.ui.components.MuscleFilterRow
import com.gymtrack.app.ui.components.MusclePill
import com.gymtrack.app.ui.components.Pill
import com.gymtrack.app.ui.components.SectionHeader
import com.gymtrack.app.ui.components.StatRow
import com.gymtrack.app.ui.components.StatTile
import com.gymtrack.app.ui.components.appViewModel
import com.gymtrack.app.ui.components.filterExercises
import com.gymtrack.app.ui.components.fmtDate
import com.gymtrack.app.ui.components.fmtDayMonth
import com.gymtrack.domain.Equipment
import com.gymtrack.domain.Format
import com.gymtrack.domain.MuscleGroup
import com.gymtrack.domain.OneRepMax
import com.gymtrack.domain.Records
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val c: AppContainer) : ViewModel() {
    val exercises = c.workouts.observeExercises().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun toggleFavorite(e: ExerciseEntity) = viewModelScope.launch { c.workouts.setFavorite(e.id, !e.isFavorite) }
}

@Composable
fun LibraryScreen(nav: NavController) {
    val vm = appViewModel { LibraryViewModel(it) }
    val all by vm.exercises.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by remember { mutableStateOf<MuscleGroup?>(null) }
    var fav by rememberSaveable { mutableStateOf(false) }
    val list = remember(all, query, muscle, fav) { all.filterExercises(query, muscle, fav) }

    GtScaffold(
        title = "Exercícios", isTab = true,
        fab = { ExtendedFloatingActionButton(onClick = { nav.navigate(Routes.exerciseEdit(0)) }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Personalizado") }) },
    ) { pad ->
        Column(Modifier.padding(top = pad.calculateTopPadding()).fillMaxSize()) {
            OutlinedTextField(
                query, { query = it }, singleLine = true, placeholder = { Text("Buscar entre ${all.size} exercícios") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Limpar") } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("library_search"), shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(8.dp))
            MuscleFilterRow(muscle, { muscle = it }, fav, { fav = it })
            LazyColumn(Modifier.weight(1f).imePadding(), contentPadding = PaddingValues(bottom = 96.dp)) {
                if (list.isEmpty()) item { EmptyState(Icons.Outlined.FitnessCenter, "Nada encontrado", "Tente outro termo ou crie um exercício personalizado.") }
                items(list, key = { it.id }) { e ->
                    GtCard(
                        onClick = { nav.navigate(Routes.exercise(e.id)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp).testTag("lib_${e.name}"),
                        contentPadding = PaddingValues(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExerciseThumb(e, width = 96.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(e.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    MusclePill(MuscleGroup.fromName(e.primaryMuscle))
                                    Pill(Equipment.fromName(e.equipment).label)
                                }
                                if (e.isCustom) Pill("Personalizado", Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.secondaryContainer, textColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            IconButton(onClick = { vm.toggleFavorite(e) }) {
                                Icon(if (e.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder, if (e.isFavorite) "Remover dos favoritos" else "Favoritar", tint = if (e.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, text: String) = GtCard {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

class ExerciseDetailViewModel(private val c: AppContainer, id: Long) : ViewModel() {
    val exercise = c.workouts.observeExercise(id).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val rows = c.workouts.observeExerciseRows(id).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    fun toggleFavorite() = viewModelScope.launch { exercise.value?.let { c.workouts.setFavorite(it.id, !it.isFavorite) } }
    fun delete(onDone: () -> Unit) = viewModelScope.launch { exercise.value?.let { c.workouts.deleteExercise(it.id) }; onDone() }
}

private enum class ExChart(val label: String) { E1RM("1RM est."), WEIGHT("Carga máx."), VOLUME("Volume"), REPS("Reps") }

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(nav: NavController, id: Long) {
    val vm = appViewModel(key = "ex_$id") { ExerciseDetailViewModel(it, id) }
    val e by vm.exercise.collectAsState()
    val rows by vm.rows.collectAsState()
    val settings by vm.settings.collectAsState()
    var chart by rememberSaveable { mutableStateOf(ExChart.E1RM) }
    var confirmDelete by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf<Int?>(null) }
    val ex = e ?: run { GtScaffold("Exercício", onBack = { nav.popBackStack() }) {}; return }
    val demo = rememberDemo(ex)
    val points = remember(rows, settings.oneRmFormula) { Analytics.exercisePoints(rows, settings.oneRmFormula) }
    val records = remember(rows, settings.oneRmFormula) { Records.compute(Analytics.performances(rows), settings.oneRmFormula) }

    GtScaffold(
        title = ex.name, onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { vm.toggleFavorite() }) { Icon(if (ex.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder, "Favorito", tint = if (ex.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = { nav.navigate(Routes.exerciseEdit(ex.id)) }) { Icon(Icons.Outlined.Edit, "Editar") }
            if (ex.isCustom) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Excluir") }
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ExerciseDemoView(ex) { fullscreen = it } }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MusclePill(MuscleGroup.fromName(ex.primaryMuscle))
                    Pill(Equipment.fromName(ex.equipment).label)
                    Pill("Descanso ${ex.defaultRestSec}s")
                }
            }
            if (ex.description.isNotBlank()) item { InfoCard("Execução", ex.description) }
            item {
                val sec = ex.secondaryMuscles.split(',').mapNotNull { MuscleGroup.fromName(it) }
                GtCard {
                    Text("Músculos trabalhados", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Principal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(92.dp))
                        Pill(MuscleGroup.fromName(ex.primaryMuscle)?.label ?: "–", color = MaterialTheme.colorScheme.primaryContainer, textColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    if (sec.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Text("Secundários", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(92.dp).padding(top = 4.dp))
                            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { sec.forEach { Pill(it.label) } }
                        }
                    }
                }
            }
            if (ex.tips.isNotBlank()) item { InfoCard("Orientações técnicas", ex.tips) }
            if (ex.mistakes.isNotBlank()) item {
                GtCard {
                    Text("Erros comuns", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                    ex.mistakes.split(';').map { it.trim().trimEnd('.') }.filter { it.isNotEmpty() }.forEach { m ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("✕", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(20.dp))
                            Text(m.replaceFirstChar { it.uppercase() } + ".", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item { SectionHeader("Seu desempenho") }
            if (rows.isEmpty()) item {
                GtCard { Text("Sem registros ainda", style = MaterialTheme.typography.titleMedium); Text("Depois de concluir séries deste exercício, os recordes e gráficos aparecem aqui.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                item {
                    StatRow {
                        StatTile("Maior carga", records.maxWeight?.let { "${Format.weight(it)} kg" } ?: "–", Modifier.weight(1f), sub = records.maxWeightReps?.let { "× $it reps" }, accent = true)
                        StatTile("1RM estimado", records.bestEstimated1Rm?.let { "${Format.weight(OneRepMax.round(it))} kg" } ?: "–", Modifier.weight(1f), sub = settings.oneRmFormula.label)
                    }
                }
                item {
                    StatRow {
                        StatTile("Máx. reps", records.maxReps?.toString() ?: "–", Modifier.weight(1f), sub = records.maxRepsWeight?.let { "com ${Format.weight(it)} kg" })
                        StatTile("Melhor volume", records.maxSessionVolume?.let { Format.volume(it) } ?: "–", Modifier.weight(1f), sub = "${points.size} sessões")
                    }
                }
                item {
                    GtCard {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ExChart.entries.forEachIndexed { i, c ->
                                SegmentedButton(selected = chart == c, onClick = { chart = c }, shape = SegmentedButtonDefaults.itemShape(i, ExChart.entries.size), label = { Text(c.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val data = points.mapNotNull { p ->
                            val y = when (chart) { ExChart.E1RM -> p.estimated1Rm; ExChart.WEIGHT -> p.maxWeight; ExChart.VOLUME -> p.volume; ExChart.REPS -> p.totalReps.toDouble() } ?: return@mapNotNull null
                            val unit = if (chart == ExChart.REPS) " reps" else " kg"
                            ChartPoint(y.toFloat(), fmtDayMonth(p.date), "${fmtDayMonth(p.date)} · ${Format.weight(OneRepMax.round(y))}$unit")
                        }
                        if (data.size < 2) Text("Registre ao menos 2 sessões para ver a evolução.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else LineChart(data, description = "Evolução: ${chart.label}")
                        if (chart == ExChart.E1RM) Text("1RM estimado com séries de até ${OneRepMax.MAX_REPS} repetições. É uma estimativa, não um teste real de carga máxima.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                records.bestEstimated1Rm?.let { orm ->
                    item {
                        GtCard {
                            Text("Cargas de referência (% do 1RM estimado)", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            OneRepMax.percentages(orm).chunked(3).forEach { row ->
                                Row(Modifier.fillMaxWidth()) { row.forEach { (pct, kg) -> Column(Modifier.weight(1f).padding(vertical = 4.dp)) { Text("$pct%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${Format.weight(OneRepMax.round(kg))} kg", style = MaterialTheme.typography.titleSmall) } } }
                            }
                        }
                    }
                }
            }
            if (points.isNotEmpty()) {
                item { SectionHeader("Histórico de desempenho") }
                items(points.reversed(), key = { it.sessionId }) { p ->
                    GtCard(onClick = { nav.navigate(Routes.session(p.sessionId)) }, contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(fmtDate(p.date), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(Format.volume(p.volume), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            p.sets.filter { it.isWorking }.joinToString("   ") { "${Format.weight(it.weight)}×${it.reps}" + (it.rpe?.let { r -> "@${Format.weight(r)}" } ?: "") },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        p.estimated1Rm?.let { Text("1RM est. ${Format.weight(OneRepMax.round(it))} kg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            demo.credit?.let { c -> item { Text(c, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) } }
        }
    }
    fullscreen?.let { DemoFullscreen(ex, it) { fullscreen = null } }
    if (confirmDelete) ConfirmDialog("Excluir exercício?", "Se ele já tiver histórico, será apenas arquivado para preservar seus registros.", "Excluir", { vm.delete { nav.popBackStack() } }, { confirmDelete = false }, destructive = true)
}

class ExerciseEditViewModel(private val c: AppContainer, val id: Long) : ViewModel() {
    var loaded by mutableStateOf<ExerciseEntity?>(null); private set
    /** Image file names being edited (index 0 = start, 1 = end). */
    val images = mutableStateListOf<String?>(null, null)
    private val created = mutableListOf<String>()
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)
    val media get() = c.media

    init {
        viewModelScope.launch {
            val e = if (id == 0L) ExerciseEntity(name = "", primaryMuscle = MuscleGroup.CHEST.name, equipment = Equipment.MACHINE.name, isCustom = true, defaultRestSec = c.settings.current().defaultRestSec) else c.workouts.getExercise(id)
            images[0] = e?.imageStart; images[1] = e?.imageEnd
            loaded = e
        }
    }

    fun setImage(slot: Int, uri: android.net.Uri) = viewModelScope.launch {
        busy = true
        runCatching { c.media.importImage(uri) }
            .onSuccess { name -> created += name; images[slot] = name }
            .onFailure { error = it.message ?: "Não foi possível importar a imagem" }
        busy = false
    }

    fun removeImage(slot: Int) { images[slot] = null }

    fun save(e: ExerciseEntity, onDone: (Long) -> Unit) = viewModelScope.launch {
        val old = listOfNotNull(loaded?.imageStart, loaded?.imageEnd)
        // Keep order: if only the end image is set it is still shown as the single image.
        val saved = e.copy(imageStart = images[0], imageEnd = images[1])
        val newId = c.workouts.saveExercise(saved)
        (old + created).filter { it != images[0] && it != images[1] }.forEach { c.media.delete(it) }
        created.clear()
        onDone(newId)
    }

    /** Discards images imported in this screen but not saved. */
    override fun onCleared() { created.filter { it != loaded?.imageStart && it != loaded?.imageEnd }.forEach { c.media.delete(it) } }
}

@Composable
private fun ImageSlot(label: String, file: java.io.File?, onGallery: () -> Unit, onCamera: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth().aspectRatio(1.5f).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (file != null) coil.compose.AsyncImage(model = file, contentDescription = label, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Outlined.AddAPhoto, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row {
            IconButton(onClick = onGallery) { Icon(Icons.Outlined.PhotoLibrary, "Escolher da galeria: $label") }
            IconButton(onClick = onCamera) { Icon(Icons.Outlined.PhotoCamera, "Tirar foto: $label") }
            if (file != null) IconButton(onClick = onRemove) { Icon(Icons.Outlined.Delete, "Remover imagem: $label") }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditScreen(nav: NavController, id: Long) {
    val vm = appViewModel(key = "exedit_$id") { ExerciseEditViewModel(it, id) }
    val base = vm.loaded ?: run { GtScaffold(if (id == 0L) "Novo exercício" else "Editar", onBack = { nav.popBackStack() }) {}; return }
    var name by remember { mutableStateOf(base.name) }
    var muscle by remember { mutableStateOf(MuscleGroup.fromName(base.primaryMuscle) ?: MuscleGroup.CHEST) }
    var equipment by remember { mutableStateOf(Equipment.fromName(base.equipment)) }
    val secondary = remember { mutableStateListOf<MuscleGroup>().apply { addAll(base.secondaryMuscles.split(',').mapNotNull { MuscleGroup.fromName(it) }) } }
    var description by remember { mutableStateOf(base.description) }
    var tips by remember { mutableStateOf(base.tips) }
    var rest by remember { mutableStateOf(base.defaultRestSec.toString()) }
    var mistakes by remember { mutableStateOf(base.mistakes) }
    var pendingSlot by remember { mutableStateOf(0) }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val gallery = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.setImage(pendingSlot, it) } }
    val camera = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { ok -> val u = cameraUri; if (ok && u != null) vm.setImage(pendingSlot, u) }
    fun openGallery(slot: Int) { pendingSlot = slot; gallery.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    fun openCamera(slot: Int) {
        pendingSlot = slot
        val f = vm.media.newCameraFile()
        val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
        cameraUri = uri
        runCatching { camera.launch(uri) }.onFailure { vm.error = "Nenhum app de câmera disponível" }
    }
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    androidx.compose.runtime.LaunchedEffect(vm.error) { vm.error?.let { snackbar.showSnackbar(it); vm.error = null } }

    GtScaffold(title = if (id == 0L) "Novo exercício" else "Editar exercício", onBack = { nav.popBackStack() }, snackbar = snackbar, actions = {
        androidx.compose.material3.TextButton(enabled = name.isNotBlank(), onClick = {
            vm.save(base.copy(name = name.trim(), primaryMuscle = muscle.name, equipment = equipment.name, secondaryMuscles = secondary.filter { it != muscle }.joinToString(",") { it.name }, description = description.trim(), tips = tips.trim(), mistakes = mistakes.trim(), defaultRestSec = rest.toIntOrNull()?.coerceIn(0, 900) ?: base.defaultRestSec)) { nav.popBackStack() }
        }, modifier = Modifier.testTag("save_exercise")) { Text("Salvar", fontWeight = FontWeight.Bold) }
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("exercise_name")) }
            item {
                GtCard {
                    Text("Imagens do exercício", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (base.seedKey != null) "Opcional: suas imagens substituem a demonstração da biblioteca neste aparelho." else "Adicione as posições inicial e final (galeria ou câmera). Ficam salvas só no aparelho.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (vm.busy) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Posição inicial", "Posição final").forEachIndexed { i, label ->
                            ImageSlot(label, vm.images[i]?.let { java.io.File(vm.media.imagesDir, it) }, { openGallery(i) }, { openCamera(i) }, { vm.removeImage(i) }, Modifier.weight(1f))
                        }
                    }
                }
            }
            item { EnumDropdown("Grupo muscular principal", muscle, MuscleGroup.entries, { it.label }, { muscle = it }) }
            item { EnumDropdown("Equipamento", equipment, Equipment.entries, { it.label }, { equipment = it }) }
            item {
                Text("Músculos secundários", style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MuscleGroup.entries.filter { it != muscle }.forEach { m -> FilterChip(selected = m in secondary, onClick = { if (m in secondary) secondary.remove(m) else secondary.add(m) }, label = { Text(m.label) }) }
                }
            }
            item { OutlinedTextField(rest, { rest = it.filter(Char::isDigit).take(3) }, label = { Text("Descanso padrão (segundos)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(description, { description = it }, label = { Text("Descrição da execução") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(tips, { tips = it }, label = { Text("Orientações gerais") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(mistakes, { mistakes = it }, label = { Text("Erros comuns (separe com ;)") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
        }
    }
}
