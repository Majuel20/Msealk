package com.msealk.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msealk.data.Corte
import com.msealk.util.CorteZoneMatch
import com.msealk.util.CutDetailFormatter
import com.msealk.util.ShareUtils
import java.time.LocalDateTime

@Composable
fun CorteCard(match: CorteZoneMatch, modifier: Modifier = Modifier) {
    CorteCard(match.corte, modifier, match.zone.name, match.matchedKeywords, match.zone.colorKey)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CorteCard(corte: Corte, modifier: Modifier = Modifier, zonaGuardada: String? = null,
    coincidencias: List<String> = emptyList(), zoneColorKey: String? = null) {
    val context = LocalContext.current
    val accent = zoneColorKey?.let { zoneColorByKey(it) } ?: NeonColor.Cyan
    val details = remember(corte) { CutDetailFormatter.format(corte) }
    val now = LocalDateTime.now()
    val start = corte.inicioDateTime()
    val end = corte.finDateTime()
    val isPast = end?.isBefore(now) == true
    val isActive = start != null && end != null && !start.isAfter(now) && end.isAfter(now)
    var expanded by rememberSaveable(corte.stableKey) { mutableStateOf(false) }
    GlassPanel(modifier.fillMaxWidth().animateContentSize(), accent = zoneColorKey?.let { accent }, contentPadding = 16.dp) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (zonaGuardada != null) NeonChip(zonaGuardada, color = accent)
            when {
                isPast -> NeonChip("Finalizado", color = NeonColor.Muted)
                isActive -> NeonChip("En horario activo", color = NeonColor.Orange)
                start == null -> NeonChip("Fecha por confirmar", color = NeonColor.Orange)
                else -> NeonChip("Programado", color = NeonColor.Cyan)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(corte.fechaBonita().ifBlank { "Fecha por confirmar" }, color = NeonColor.Muted,
            style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Text(corte.rangoHora().ifBlank { "Horario por confirmar" }, color = NeonColor.Text,
            style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(corte.distrito.ifBlank { "Corte programado" }, color = NeonColor.Text,
            style = MaterialTheme.typography.titleMedium)
        if (!expanded && details.zones.isNotBlank()) Text(details.zones, color = NeonColor.Muted,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (coincidencias.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Coincide con: ${coincidencias.joinToString(", ")}", color = accent,
                style = MaterialTheme.typography.bodySmall)
        }
        if (!expanded) DetailToggle(false) { expanded = true }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (details.circuit.isNotBlank()) CutInfo("Circuito", details.circuit)
                if (details.description.isNotBlank()) CutInfo("Descripción", details.description)
                if (details.zones.isNotBlank()) CutInfo("Zonas afectadas", details.zones)
                if (details.substations.isNotBlank()) CutInfo("Subestaciones", details.substations)
            }
            DetailToggle(true) { expanded = false }
        }
        AdaptiveRow {
            NeonOutlinedButton("Calendario", modifier = Modifier.weight(1f),
                onClick = { ShareUtils.addCorteToCalendar(context, corte, zonaGuardada) })
            if (corte.url.isNotBlank()) NeonOutlinedButton("Ver fuente", modifier = Modifier.weight(1f),
                onClick = { ShareUtils.openUrl(context, corte.url) })
            else NeonOutlinedButton("Compartir", modifier = Modifier.weight(1f),
                onClick = { ShareUtils.shareCorte(context, corte, zonaGuardada) })
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            AdaptiveRow {
                NeonOutlinedButton("Copiar aviso", color = NeonColor.Muted, modifier = Modifier.weight(1f),
                    onClick = { ShareUtils.copyCorteToClipboard(context, corte, zonaGuardada) })
                if (corte.url.isNotBlank()) NeonOutlinedButton("Compartir", color = NeonColor.Muted, modifier = Modifier.weight(1f),
                    onClick = { ShareUtils.shareCorte(context, corte, zonaGuardada) })
            }
        }
    }
}

@Composable
private fun CutInfo(label: String, value: String) {
    var expanded by rememberSaveable(label, value) { mutableStateOf(false) }
    var overflow by remember(value) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = NeonColor.Cyan, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(value, color = NeonColor.Muted, style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflow = it.hasVisualOverflow })
        if (overflow || expanded) TextButton(onClick = { expanded = !expanded },
            modifier = Modifier.heightIn(min = 48.dp).semantics {
                stateDescription = if (expanded) "$label completo" else "$label resumido"
            }) {
            Text(if (expanded) "▴ Ver menos" else "▾ Ver más", color = NeonColor.Cyan, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DetailToggle(expanded: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .semantics { stateDescription = if (expanded) "Detalles expandidos" else "Detalles contraídos" }) {
        Text(if (expanded) "Ocultar detalles" else "Ver detalles", color = NeonColor.Cyan, fontWeight = FontWeight.Bold)
    }
}
