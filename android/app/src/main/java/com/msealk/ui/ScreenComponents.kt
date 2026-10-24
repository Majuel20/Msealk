package com.msealk.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.msealk.data.*
import com.msealk.util.*
import java.time.LocalDate

@Composable
internal fun ScreenHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        ScreenTitle(title)
        if (subtitle.isNotBlank()) Text(subtitle, color = NeonColor.Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = modifier.semantics { heading() })
}

@Composable
internal fun EmptyCard(title: String, subtitle: String) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = NeonColor.Muted)
    }
}

@Composable
internal fun CompactSwitchRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (enabled) Color.White else NeonColor.Muted, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = null)
    }
}

@Composable
internal fun NeonSwitch(checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit, label: String? = null) {
    Switch(
        modifier = Modifier.semantics { if (label != null) contentDescription = label },
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = NeonColor.Cyan.copy(alpha = 0.75f),
            uncheckedThumbColor = Color.White.copy(alpha = 0.75f),
            uncheckedTrackColor = Color.White.copy(alpha = 0.14f),
            uncheckedBorderColor = Color.White.copy(alpha = 0.25f),
            disabledCheckedTrackColor = Color.White.copy(alpha = 0.14f),
            disabledUncheckedTrackColor = Color.White.copy(alpha = 0.08f)
        )
    )
}

@Composable
internal fun NeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = false,
    minLines: Int = 1,
    supporting: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        supportingText = if (supporting != null) { { Text(supporting) } } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = NeonColor.Cyan,
            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
            focusedLabelColor = NeonColor.Cyan,
            unfocusedLabelColor = NeonColor.Muted,
            focusedSupportingTextColor = NeonColor.Muted,
            unfocusedSupportingTextColor = NeonColor.Muted,
            cursorColor = NeonColor.Cyan
        )
    )
}

@Composable
internal fun NumberAdjustRow(title: String, value: Int, suffix: String, enabled: Boolean, min: Int, max: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = if (enabled) NeonColor.Text else NeonColor.Muted)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeonOutlinedButton("−", enabled = enabled && value > min,
                modifier = Modifier.semantics { contentDescription = "Reducir $title" }) { onChange((value - 1).coerceAtLeast(min)) }
            Text("$value ${if (value == 1 && suffix == "días") "día" else suffix}", color = NeonColor.Text, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            NeonOutlinedButton("+", enabled = enabled && value < max,
                modifier = Modifier.semantics { contentDescription = "Aumentar $title" }) { onChange((value + 1).coerceAtMost(max)) }
        }
    }
}

@Composable
internal fun TimeAdjustRow(title: String, hour: Int, minute: Int, enabled: Boolean, onChange: (Int, Int) -> Unit) {
    fun change(deltaMinutes: Int) {
        val total = (hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59) + deltaMinutes + 24 * 60) % (24 * 60)
        onChange(total / 60, total % 60)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = if (enabled) NeonColor.Text else NeonColor.Muted)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeonOutlinedButton("−15", enabled = enabled,
                modifier = Modifier.semantics { contentDescription = "Restar 15 minutos: $title" }) { change(-15) }
            Text(DateParser.formatTime(hour, minute), color = NeonColor.Text, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            NeonOutlinedButton("+15", enabled = enabled,
                modifier = Modifier.semantics { contentDescription = "Sumar 15 minutos: $title" }) { change(15) }
        }
    }
}

@Composable
internal fun DayHeader(date: LocalDate?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(date?.let { DateParser.formatDate(it) } ?: "Sin fecha clara", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        NeonChip(date?.let { dayCountLabel(it) } ?: "revisar", color = NeonColor.Cyan)
    }
}

internal fun historyLabel(type: String): String = when (type.lowercase()) {
    "error" -> "Error"
    "warning" -> "Aviso"
    "notification", "alert" -> "Notificación"
    "cleanup" -> "Limpieza"
    "sync" -> "Sincronización"
    else -> "Actividad"
}

internal fun historyColor(type: String): Color = when (type.lowercase()) {
    "error" -> NeonColor.Red
    "warning" -> NeonColor.Orange
    "notification", "alert" -> NeonColor.Green
    "cleanup" -> NeonColor.Purple
    "sync" -> NeonColor.Cyan
    else -> NeonColor.Muted
}

private fun dayCountLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when {
        date == today -> "hoy"
        date == today.plusDays(1) -> "mañana"
        date.isBefore(today) -> "pasado"
        else -> "en ${java.time.temporal.ChronoUnit.DAYS.between(today, date)} días"
    }
}

internal fun shortSyncText(settings: AppSettings): String = when {
    !settings.autoSyncEnabled -> "Actualización manual"
    settings.syncEveryMonday -> "Cada lunes"
    settings.syncIntervalDays == 1 -> "Cada día"
    else -> "Cada ${settings.syncIntervalDays} días"
}


@Composable
internal fun DiagnosticLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = NeonColor.Muted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = NeonColor.Text, style = MaterialTheme.typography.bodyMedium)
    }
}

