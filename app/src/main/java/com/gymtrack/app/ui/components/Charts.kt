package com.gymtrack.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtrack.app.ui.theme.Gt
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

data class ChartPoint(val y: Float, val xLabel: String, val tooltip: String)

/** "Nice" axis ticks so gridlines land on round numbers. */
private fun niceTicks(minV: Float, maxV: Float, count: Int = 4): List<Float> {
    var lo = minV; var hi = maxV
    if (hi - lo < 1e-6f) { hi = lo + 1f; lo = (lo - 1f).coerceAtLeast(0f) }
    val rawStep = (hi - lo) / count
    val mag = 10.0.pow(floor(log10(rawStep.toDouble()))).toFloat()
    val norm = rawStep / mag
    val step = when { norm <= 1f -> 1f; norm <= 2f -> 2f; norm <= 2.5f -> 2.5f; norm <= 5f -> 5f; else -> 10f } * mag
    val start = floor(lo / step) * step
    val end = ceil(hi / step) * step
    val out = mutableListOf<Float>()
    var v = start
    while (v <= end + step / 2) { out += v; v += step }
    return out
}

private fun compact(v: Float): String = when {
    abs(v) >= 10_000 -> "${(v / 1000).let { if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it) }}k"
    v % 1f == 0f -> v.toInt().toString()
    else -> "%.1f".format(v)
}

/**
 * Interactive line chart (one series). Tap or drag to inspect a point; a crosshair and
 * tooltip show the exact value. X positions are evenly spaced by session.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    description: String = "Gráfico de linha",
) {
    if (points.isEmpty()) return
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val measurer = rememberTextMeasurer()
    val grid = Gt.colors.chartGrid
    val axisText = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipFg = MaterialTheme.colorScheme.inverseOnSurface
    val reveal = remember(points) { Animatable(0f) }
    LaunchedEffect(points) { reveal.animateTo(1f, tween(600)) }

    val ticks = niceTicks(points.minOf { it.y }, points.maxOf { it.y })
    val yMin = ticks.first(); val yMax = ticks.last()
    val labelStyle = TextStyle(fontSize = 10.sp, color = axisText)

    Canvas(
        modifier.fillMaxWidth().height(height)
            .semantics { contentDescription = "$description. ${points.size} pontos. Último: ${points.last().tooltip}" }
            .pointerInput(points) {
                fun pick(x: Float) {
                    val left = 40.dp.toPx(); val right = size.width - 12.dp.toPx()
                    val n = points.size
                    selected = if (n == 1) 0 else (((x - left) / (right - left)) * (n - 1)).let { Math.round(it).coerceIn(0, n - 1) }
                }
                detectTapGestures(onTap = { pick(it.x) })
            }
            .pointerInput(points) {
                fun pick(x: Float) {
                    val left = 40.dp.toPx(); val right = size.width - 12.dp.toPx()
                    val n = points.size
                    selected = if (n == 1) 0 else (((x - left) / (right - left)) * (n - 1)).let { Math.round(it).coerceIn(0, n - 1) }
                }
                detectDragGestures(onDragStart = { pick(it.x) }, onDrag = { c, _ -> pick(c.position.x) })
            },
    ) {
        val left = 40.dp.toPx(); val right = size.width - 12.dp.toPx()
        val top = 28.dp.toPx(); val bottom = size.height - 22.dp.toPx()
        fun yPos(v: Float) = bottom - (v - yMin) / (yMax - yMin) * (bottom - top)
        fun xPos(i: Int) = if (points.size == 1) (left + right) / 2 else left + i * (right - left) / (points.size - 1)

        ticks.forEach { t ->
            val y = yPos(t)
            drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            val tl = measurer.measure(compact(t), labelStyle)
            drawText(tl, topLeft = Offset(left - tl.size.width - 6.dp.toPx(), y - tl.size.height / 2))
        }
        // x labels: first, middle, last
        val idx = listOf(0, points.size / 2, points.size - 1).distinct()
        idx.forEach { i ->
            val tl = measurer.measure(points[i].xLabel, labelStyle)
            val x = (xPos(i) - tl.size.width / 2).coerceIn(left - 10f, size.width - tl.size.width.toFloat())
            drawText(tl, topLeft = Offset(x, bottom + 6.dp.toPx()))
        }

        val visible = (points.size * reveal.value).coerceAtLeast(1f)
        val path = Path(); val area = Path()
        points.forEachIndexed { i, p ->
            if (i > visible) return@forEachIndexed
            val o = Offset(xPos(i), yPos(p.y))
            if (i == 0) { path.moveTo(o.x, o.y); area.moveTo(o.x, bottom); area.lineTo(o.x, o.y) } else { path.lineTo(o.x, o.y); area.lineTo(o.x, o.y) }
        }
        val lastIdx = floor(visible).toInt().coerceAtMost(points.size - 1)
        area.lineTo(xPos(lastIdx), bottom); area.close()
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)), startY = top, endY = bottom))
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (points.size <= 40) points.forEachIndexed { i, p ->
            if (i > visible) return@forEachIndexed
            val o = Offset(xPos(i), yPos(p.y))
            drawCircle(surface, radius = 6.dp.toPx(), center = o)
            drawCircle(color, radius = 4.dp.toPx(), center = o)
        }

        selected?.let { i ->
            val o = Offset(xPos(i), yPos(points[i].y))
            drawLine(axisText.copy(alpha = 0.5f), Offset(o.x, top), Offset(o.x, bottom), strokeWidth = 1.dp.toPx())
            drawCircle(surface, radius = 8.dp.toPx(), center = o)
            drawCircle(color, radius = 6.dp.toPx(), center = o)
            val tl = measurer.measure(points[i].tooltip, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tooltipFg))
            val pad = 8.dp.toPx()
            val w = tl.size.width + pad * 2; val h = tl.size.height + pad
            val bx = (o.x - w / 2).coerceIn(0f, size.width - w)
            drawRoundRect(tooltipBg, topLeft = Offset(bx, 0f), size = Size(w, h), cornerRadius = CornerRadius(8.dp.toPx()))
            drawText(tl, topLeft = Offset(bx + pad, pad / 2))
        }
    }
}

data class BarDatum(val value: Float, val label: String, val tooltip: String)

/** Vertical bar chart with rounded data-ends; tap a bar to see its value. */
@Composable
fun BarChart(
    bars: List<BarDatum>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    highlightLast: Boolean = true,
    target: Float? = null,
    description: String = "Gráfico de barras",
) {
    if (bars.isEmpty()) return
    var selected by remember(bars) { mutableStateOf<Int?>(null) }
    val measurer = rememberTextMeasurer()
    val grid = Gt.colors.chartGrid
    val axisText = MaterialTheme.colorScheme.onSurfaceVariant
    val muted = color.copy(alpha = 0.45f)
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipFg = MaterialTheme.colorScheme.inverseOnSurface
    val grow = remember(bars) { Animatable(0f) }
    LaunchedEffect(bars) { grow.animateTo(1f, tween(500)) }
    val maxV = max(bars.maxOf { it.value }, target ?: 0f)
    val ticks = niceTicks(0f, if (maxV <= 0f) 1f else maxV, 3)
    val yMax = ticks.last()
    val labelStyle = TextStyle(fontSize = 10.sp, color = axisText, textAlign = TextAlign.Center)

    Canvas(
        modifier.fillMaxWidth().height(height)
            .semantics { contentDescription = "$description. " + bars.joinToString { it.tooltip } }
            .pointerInput(bars) {
                detectTapGestures { pos ->
                    val left = 32.dp.toPx(); val slot = (size.width - left) / bars.size
                    val i = ((pos.x - left) / slot).toInt()
                    selected = if (i in bars.indices) (if (selected == i) null else i) else null
                }
            },
    ) {
        val left = 32.dp.toPx(); val top = 26.dp.toPx(); val bottom = size.height - 20.dp.toPx()
        val slot = (size.width - left) / bars.size
        val barW = (slot * 0.62f).coerceAtMost(28.dp.toPx())
        fun yPos(v: Float) = bottom - v / yMax * (bottom - top)
        ticks.forEach { t ->
            val y = yPos(t)
            drawLine(grid, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val tl = measurer.measure(compact(t), labelStyle)
            drawText(tl, topLeft = Offset(left - tl.size.width - 6.dp.toPx(), y - tl.size.height / 2))
        }
        target?.let {
            val y = yPos(it)
            drawLine(axisText, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        }
        val every = ceil(bars.size / 8f).toInt().coerceAtLeast(1)
        bars.forEachIndexed { i, b ->
            val cx = left + slot * i + slot / 2
            val h = (b.value / yMax * (bottom - top)) * grow.value
            val c = when {
                selected == i -> color
                selected != null -> muted
                highlightLast && i == bars.lastIndex -> color
                highlightLast -> muted
                else -> color
            }
            if (h > 0.5f) {
                val r = 4.dp.toPx().coerceAtMost(h)
                val path = Path().apply {
                    addRoundRect(RoundRect(cx - barW / 2, bottom - h, cx + barW / 2, bottom, topLeftCornerRadius = CornerRadius(r), topRightCornerRadius = CornerRadius(r)))
                }
                drawPath(path, c)
            }
            if (i % every == 0 || i == bars.lastIndex) {
                val tl = measurer.measure(b.label, labelStyle)
                drawText(tl, topLeft = Offset(cx - tl.size.width / 2, bottom + 5.dp.toPx()))
            }
        }
        selected?.let { i ->
            val cx = left + slot * i + slot / 2
            val tl = measurer.measure(bars[i].tooltip, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tooltipFg))
            val pad = 8.dp.toPx(); val w = tl.size.width + pad * 2; val h = tl.size.height + pad
            val bx = (cx - w / 2).coerceIn(0f, size.width - w)
            drawRoundRect(tooltipBg, topLeft = Offset(bx, 0f), size = Size(w, h), cornerRadius = CornerRadius(8.dp.toPx()))
            drawText(tl, topLeft = Offset(bx + pad, pad / 2))
        }
    }
}

/** Ranked horizontal bars (e.g. sets per muscle group). Labels and values in text ink. */
@Composable
fun HorizontalBars(items: List<Triple<String, Float, String>>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val maxV = items.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (label, v, text) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(104.dp), maxLines = 1)
                Box(Modifier.weight(1f).height(12.dp)) {
                    Box(Modifier.fillMaxWidth(v / maxV).height(12.dp).clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).background(color))
                }
                Spacer(Modifier.width(8.dp))
                Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
            }
        }
    }
}

/** Monday-first month grid; training days are filled, today is outlined. */
@Composable
fun MonthCalendar(
    month: YearMonth,
    trainingDays: Set<Int>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onDayClick: ((LocalDate) -> Unit)? = null,
) {
    val today = LocalDate.now()
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    val title = month.month.getDisplayName(java.time.format.TextStyle.FULL, PtBr).replaceFirstChar { it.uppercase() } + " ${month.year}"
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mês anterior") }
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButton(onClick = onNext, enabled = month < YearMonth.now()) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Próximo mês") }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        val cells = offset + days
        val rows = ceil(cells / 7f).toInt()
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val date = month.atDay(day)
                            val trained = day in trainingDays
                            val isToday = date == today
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (trained) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .then(if (isToday && !trained) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                    .then(if (onDayClick != null && trained) Modifier.clickable { onDayClick(date) } else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day", style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (trained || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (trained) MaterialTheme.colorScheme.onPrimary else if (date.isAfter(today)) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}
