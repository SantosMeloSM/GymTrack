package com.gymtrack.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gymtrack.app.data.db.ExerciseEntity
import com.gymtrack.app.data.repo.ExerciseDemo
import com.gymtrack.domain.Equipment
import com.gymtrack.domain.MuscleGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val DEMO_RATIO = 3f / 2f
const val ANATOMY_RATIO = 2f

val ExerciseDemo.isAnatomy get() = kind == "anatomy"
private val AnatomyBg = Color(0xFFFFFFFF)

/**
 * Looping "mini video": smoothly blends each real position into the next (hold, then ease),
 * running only while composed. Two frames give a start → end → start loop.
 */
@Composable
fun LoopPlayer(frames: List<Any>, description: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit, holdMs: Int = 520, blendMs: Int = 480) {
    if (frames.isEmpty()) return
    val n = frames.size
    val transition = rememberInfiniteTransition(label = "demo-loop")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = n.toFloat(),
        animationSpec = infiniteRepeatable(tween(n * (holdMs + blendMs), easing = LinearEasing), RepeatMode.Restart),
        label = "demo-t",
    )
    val idx = t.toInt().coerceIn(0, n - 1)
    val hold = holdMs / (holdMs + blendMs).toFloat()
    val local = t - idx
    val raw = ((local - hold) / (1f - hold)).coerceIn(0f, 1f)
    val blend = raw * raw * (3 - 2 * raw) // smoothstep
    val next = (idx + 1) % n
    val ctx = LocalContext.current
    Box(modifier.semantics { contentDescription = description }.testTag("demo_loop")) {
        frames.forEachIndexed { i, f ->
            val alpha = when (i) { idx -> 1f; next -> blend; else -> 0f }
            if (alpha > 0f) AsyncImage(
                model = ImageRequest.Builder(ctx).data(f).crossfade(false).build(), contentDescription = null, contentScale = scale,
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
            )
        }
    }
}

@Composable
fun rememberDemo(e: ExerciseEntity): ExerciseDemo {
    val media = appContainer().media
    return remember(e.id, e.seedKey, e.imageStart, e.imageEnd) { media.demoFor(e) }
}

@Composable
private fun DemoImage(model: Any, description: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop, sizePx: Int? = null) {
    val ctx = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(ctx).data(model).crossfade(150).apply { if (sizePx != null) size(sizePx) }.build(),
        contentDescription = description,
        contentScale = scale,
        modifier = modifier,
    )
}

/** Standardized 3:2 thumbnail with rounded corners; falls back to a muscle badge for custom exercises without images. */
@Composable
fun ExerciseThumb(e: ExerciseEntity, modifier: Modifier = Modifier, width: Dp = 84.dp, onClick: (() -> Unit)? = null) {
    val demo = rememberDemo(e)
    Box(
        modifier.width(width).aspectRatio(DEMO_RATIO).clip(RoundedCornerShape(12.dp))
            .background(if (demo.isAnatomy) AnatomyBg else MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("thumb_${e.name}"),
        contentAlignment = Alignment.Center,
    ) {
        if (demo.hasImages) {
            if (demo.isAnatomy) DemoImage(demo.frames.last(), "Imagem de ${e.name}", Modifier.fillMaxSize().padding(2.dp), scale = ContentScale.Fit, sizePx = 320)
            else DemoImage(demo.frames.first(), "Imagem de ${e.name}", Modifier.fillMaxSize(), sizePx = 360)
        }
        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(MuscleGroup.fromName(e.primaryMuscle)?.label ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun PageDots(state: PagerState, count: Int, modifier: Modifier = Modifier, dot: Color = Color.White) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(Modifier.size(if (state.currentPage == i) 8.dp else 6.dp).clip(CircleShape).background(if (state.currentPage == i) dot else dot.copy(alpha = 0.45f)))
        }
    }
}

/**
 * Inline demonstration: swipe between start/end positions, play the sequence on demand,
 * and open the full-screen viewer.
 */
@Composable
fun ExerciseDemoView(e: ExerciseEntity, modifier: Modifier = Modifier, onExpand: (Int) -> Unit) {
    val demo = rememberDemo(e)
    if (!demo.hasImages) {
        Box(modifier.fillMaxWidth().aspectRatio(DEMO_RATIO).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(6.dp))
                Text("Sem imagem. Edite o exercício para adicionar fotos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    var playing by remember(e.id) { mutableStateOf(false) }
    if (demo.isAnatomy) {
        Box(modifier.fillMaxWidth().aspectRatio(ANATOMY_RATIO).clip(MaterialTheme.shapes.large).background(AnatomyBg).testTag("demo_${e.name}")) {
            if (playing) {
                LoopPlayer(demo.frames, "Demonstração animada de ${e.name}", Modifier.fillMaxSize().padding(8.dp))
            } else {
                Row(Modifier.fillMaxSize()) {
                    demo.frames.take(2).forEachIndexed { i, f ->
                        Box(Modifier.weight(1f).fillMaxSize().clickable { onExpand(i) }) {
                            DemoImage(f, "${e.name}: ${demo.captions.getOrElse(i) { "" }}", Modifier.fillMaxSize().padding(6.dp), scale = ContentScale.Fit)
                            Text(
                                if (i == 0) "INÍCIO" else "FIM", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5B616B),
                                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                            )
                        }
                    }
                }
            }
            Row(Modifier.align(Alignment.BottomEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (demo.animatable) RoundIcon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pausar demonstração" else "Reproduzir demonstração", tag = "demo_play") { playing = !playing }
                RoundIcon(Icons.Filled.Fullscreen, "Ampliar", tag = "demo_expand") { onExpand(0) }
            }
        }
        return
    }
    val pager = rememberPagerState { demo.frames.size }
    Box(modifier.fillMaxWidth().aspectRatio(DEMO_RATIO).clip(MaterialTheme.shapes.large).background(Color.Black).testTag("demo_${e.name}")) {
        if (playing) {
            LoopPlayer(demo.frames, "Demonstração animada de ${e.name}", Modifier.fillMaxSize(), scale = ContentScale.Crop)
        } else {
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                DemoImage(demo.frames[page], "${e.name}: ${demo.captions.getOrElse(page) { "" }}", Modifier.fillMaxSize().clickable { onExpand(page) })
            }
            // caption chip
            Surface(color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(50), modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
                Text("${pager.currentPage + 1}/${demo.frames.size} · ${demo.captions.getOrElse(pager.currentPage) { "" }}", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (demo.frames.size > 1) PageDots(pager, demo.frames.size, Modifier.align(Alignment.BottomCenter).padding(14.dp))
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (demo.animatable) RoundIcon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pausar demonstração" else "Reproduzir demonstração", tag = "demo_play") { playing = !playing }
            RoundIcon(Icons.Filled.Fullscreen, "Ampliar", tag = "demo_expand") { onExpand(pager.currentPage) }
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, tag: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onClick).testTag(tag), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = Color.White)
    }
}

/** Full-screen viewer with pinch-to-zoom (double tap resets) and on-demand playback. */
@Composable
fun DemoFullscreen(e: ExerciseEntity, startPage: Int, onDismiss: () -> Unit) {
    val demo = rememberDemo(e)
    if (!demo.hasImages) { onDismiss(); return }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val light = demo.isAnatomy
        val fg = if (light) Color(0xFF111317) else Color.White
        Surface(Modifier.fillMaxSize().testTag("demo_fullscreen"), color = if (light) AnatomyBg else Color.Black) {
            val pager = rememberPagerState(initialPage = startPage.coerceIn(0, demo.frames.lastIndex)) { demo.frames.size }
            var playing by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxSize()) {
                if (playing) LoopPlayer(demo.frames, "Demonstração animada de ${e.name}", Modifier.fillMaxSize().padding(vertical = 96.dp, horizontal = 12.dp))
                else HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }
                    DemoImage(
                        demo.frames[page], "${e.name}: ${demo.captions.getOrElse(page) { "" }}",
                        Modifier.fillMaxSize()
                            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = 1f; offset = Offset.Zero }) }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    offset = if (scale == 1f) Offset.Zero else offset + pan
                                }
                            }
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                        scale = ContentScale.Fit,
                    )
                }
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("demo_close")) { Icon(Icons.Filled.Close, "Fechar", tint = fg) }
                    Column(Modifier.weight(1f)) {
                        Text(e.name, color = fg, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (playing) "Demonstração em loop" else "${pager.currentPage + 1}/${demo.frames.size} · ${demo.captions.getOrElse(pager.currentPage) { "" }}", color = fg.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    }
                    if (demo.animatable) IconButton(onClick = { playing = !playing }, modifier = Modifier.testTag("fullscreen_play")) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pausar" else "Reproduzir", tint = fg) }
                }
                Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (demo.frames.size > 1 && !playing) PageDots(pager, demo.frames.size, dot = if (light) Color(0xFF111317) else Color.White)
                    demo.credit?.let { Text(it, color = fg.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp)) }
                    if (!playing) Text("Pinça para ampliar · toque duplo para restaurar", color = fg.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Bottom sheet used during the workout: shows the demonstration over the session without
 * leaving the screen, so logged values and timers stay as they are.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDemoSheet(e: ExerciseEntity, onDismiss: () -> Unit, onOpenDetails: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var full by remember { mutableStateOf<Int?>(null) }
    fun close() { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).verticalScroll(rememberScrollState()).testTag("demo_sheet")) {
            Text(e.name, style = MaterialTheme.typography.titleLarge)
            Text(
                listOfNotNull(MuscleGroup.fromName(e.primaryMuscle)?.label, Equipment.fromName(e.equipment).label).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            ExerciseDemoView(e) { full = it }
            if (e.description.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(e.description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedButton(onClick = onOpenDetails, modifier = Modifier.weight(1f)) { Text("Detalhes") }
                androidx.compose.material3.Button(onClick = { close() }, modifier = Modifier.weight(1f).testTag("back_to_sets")) { Text("Voltar às séries", fontWeight = FontWeight.Bold) }
            }
        }
    }
    full?.let { DemoFullscreen(e, it) { full = null } }
}
