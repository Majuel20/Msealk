package com.msealk.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.msealk.data.Corte
import com.msealk.util.CorteZoneMatch
import com.msealk.util.ShareUtils
import java.time.LocalDateTime

@Composable
fun CorteCard(match: CorteZoneMatch, modifier: Modifier = Modifier) {
    CorteCard(match.corte, modifier, match.zone.name, match.matchedKeywords, match.zone.colorKey)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CorteCard(corte: Corte, modifier: Modifier = Modifier, zonaGuardada: String? = null, coincidencias: List<String> = emptyList(),
    zoneColorKey: String? = null) {
    val context = LocalContext.current
    val accent = zoneColorKey?.let { zoneColorByKey(it) } ?: NeonColor.Cyan
    val now = LocalDateTime.now()
    val start = corte.inicioDateTime()
    val end = corte.finDateTime()
    val isPast = end?.isBefore(now) == true
    val isActive = start != null && end != null && !start.isAfter(now) && end.isAfter(now)
    var expanded by rememberSaveable(corte.stableKey) { mutableStateOf(false) }

    GlassPanel(modifier.fillMaxWidth().animateContentSize()) {
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
        Text(corte.distrito.ifBlank { corte.ubicacion.ifBlank { "Corte programado" } }, color = NeonColor.Text,
            style = MaterialTheme.typography.titleMedium)
        if (corte.zonaAfectada.isNotBlank()) Text(corte.zonaAfectada, color = NeonColor.Muted,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        if (coincidencias.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Coincide con: ${coincidencias.joinToString(", ")}", color = accent,
                style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(min = 48.dp)
            .semantics { stateDescription = if (expanded) "Detalles expandidos" else "Detalles contraídos" }) {
            Text(if (expanded) "Ocultar detalles" else "Ver detalles", color = NeonColor.Cyan)
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (corte.ubicacion.isNotBlank()) CutInfo("Ubicación", corte.ubicacion)
                if (corte.motivo.isNotBlank()) CutInfo("Motivo", corte.motivo)
                if (corte.subestaciones.isNotBlank()) CutInfo("Subestaciones", corte.subestaciones)
                if (corte.descripcion.isNotBlank()) CutInfo("Descripción", corte.descripcion)
            }
            Spacer(Modifier.height(12.dp))
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
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = NeonColor.Cyan, style = MaterialTheme.typography.labelMedium)
        Text(value, color = NeonColor.Muted, style = MaterialTheme.typography.bodyMedium)
    }
}
