package com.gymtrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymtrack.domain.MuscleGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val PtBr: Locale = Locale("pt", "BR")
fun fmtDate(ms: Long, pattern: String = "dd/MM/yyyy"): String = SimpleDateFormat(pattern, PtBr).format(Date(ms))
fun fmtDayMonth(ms: Long): String = SimpleDateFormat("dd MMM", PtBr).format(Date(ms)).replace(".", "")
fun fmtWeekday(ms: Long): String = SimpleDateFormat("EEE, dd 'de' MMM", PtBr).format(Date(ms)).replace(".", "").replaceFirstChar { it.uppercase() }

@Composable
fun GtCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = modifier.fillMaxWidth().clip(shape).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = color,
    ) { Column(Modifier.padding(contentPadding), content = content) }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(action) }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null, accent: Boolean = false) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 1)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun StatRow(content: @Composable RowScope.() -> Unit) =
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surfaceContainerHighest, textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Box(modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, maxLines = 1)
    }
}

@Composable
fun MusclePill(m: MuscleGroup?, modifier: Modifier = Modifier) = Pill(m?.label ?: "–", modifier)

@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary, bg: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), size: Int = 40) {
    Box(modifier.size(size.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size((size * 0.5).dp))
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(icon, size = 64)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, containerColor: Color = MaterialTheme.colorScheme.primary, contentColor: Color = MaterialTheme.colorScheme.onPrimary) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp), shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        if (icon != null) { Icon(icon, null); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, destructive: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun TextInputDialog(title: String, initial: String, label: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit, confirm: String = "Salvar", singleLine: Boolean = true) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(label) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onConfirm(value.trim()); onDismiss() }) { Text(confirm, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { IconBadge(icon, size = 36); Spacer(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) trailing()
    }
}
