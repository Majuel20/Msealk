package com.msealk.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.msealk.R
import com.msealk.util.CorteZoneMatch
import java.time.LocalDateTime

enum class SupplyStatus { NO_DATA, NO_ZONES, ACTIVE, UPCOMING, UNDATED, CLEAR }

data class SupplyOverview(val status: SupplyStatus, val match: CorteZoneMatch? = null)

/** Only describes published schedules; it cannot verify the actual electrical supply. */
fun supplyOverview(matches: List<CorteZoneMatch>, activeZones: Int, synced: Boolean, now: LocalDateTime): SupplyOverview {
    if (!synced) return SupplyOverview(SupplyStatus.NO_DATA)
    if (activeZones == 0) return SupplyOverview(SupplyStatus.NO_ZONES)
    val active = matches.firstOrNull {
        val start = it.corte.inicioDateTime()
        val end = it.corte.finDateTime()
        start != null && end != null && !start.isAfter(now) && end.isAfter(now)
    }
    if (active != null) return SupplyOverview(SupplyStatus.ACTIVE, active)
    val next = matches.filter { it.corte.inicioDateTime()?.isAfter(now) == true }
        .minByOrNull { it.corte.inicioDateTime()!! }
    if (next != null) return SupplyOverview(SupplyStatus.UPCOMING, next)
    val undated = matches.firstOrNull { it.corte.inicioDateTime() == null || it.corte.finDateTime() == null }
    return if (undated != null) SupplyOverview(SupplyStatus.UNDATED, undated) else SupplyOverview(SupplyStatus.CLEAR)
}

@Composable
fun HomeOverview(
    overview: SupplyOverview,
    lastSync: String,
    loading: Boolean,
    hasError: Boolean,
    onRefresh: () -> Unit
) {
    val accent = when {
        hasError -> NeonColor.Orange
        overview.status == SupplyStatus.ACTIVE -> NeonColor.Orange
        overview.status == SupplyStatus.CLEAR -> NeonColor.Green
        else -> NeonColor.Cyan
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassPanel(Modifier.fillMaxWidth().animateContentSize(), contentPadding = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.12f)).padding(10.dp)) {
                    Icon(painterResource(if (overview.status == SupplyStatus.CLEAR) R.drawable.ic_ui_check else R.drawable.ic_ui_cuts),
                        contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("ESTADO DE TUS ZONAS", color = accent, style = MaterialTheme.typography.labelMedium)
                    Text("Cortes programados · SEAL", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(when (overview.status) {
                SupplyStatus.NO_DATA -> "Consulta tus cortes"
                SupplyStatus.NO_ZONES -> "Tu zona, al día"
                SupplyStatus.ACTIVE -> "Corte en horario activo"
                SupplyStatus.UPCOMING -> "Tienes un próximo corte"
                SupplyStatus.UNDATED -> "Hay horarios por confirmar"
                SupplyStatus.CLEAR -> "Sin cortes próximos"
            }, color = NeonColor.Text, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(when (overview.status) {
                SupplyStatus.NO_DATA -> "Actualiza para consultar las interrupciones publicadas y organizar tus avisos."
                SupplyStatus.NO_ZONES -> "Agrega tu casa o trabajo para identificar los cortes que te afectan."
                SupplyStatus.ACTIVE -> "Según el horario del aviso publicado."
                SupplyStatus.UPCOMING -> "Próxima interrupción publicada para tus zonas."
                SupplyStatus.UNDATED -> "Revisa el aviso original para confirmar la fecha y el horario."
                SupplyStatus.CLEAR -> "No hay interrupciones próximas en tus zonas según la información guardada."
            }, color = NeonColor.Muted, style = MaterialTheme.typography.bodyMedium)
            overview.match?.let { match ->
                Spacer(Modifier.height(6.dp))
                Text(match.zone.name, color = accent, style = MaterialTheme.typography.titleMedium)
                Text("${match.corte.fechaBonita().ifBlank { "Fecha por confirmar" }} · ${match.corte.rangoHora().ifBlank { "Horario por confirmar" }}",
                    color = NeonColor.Text, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
            Text("Actualizado: $lastSync", color = NeonColor.Muted, style = MaterialTheme.typography.labelSmall)
            if (hasError) {
                Spacer(Modifier.height(12.dp))
                Text("No se pudo actualizar. Revisa tu conexión y vuelve a intentarlo. Se conserva la información guardada.",
                    color = NeonColor.Orange, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        Button(onClick = onRefresh, enabled = !loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonColor.Cyan, contentColor = NeonColor.Dark,
                disabledContainerColor = NeonColor.Cyan.copy(alpha = 0.18f), disabledContentColor = NeonColor.Cyan)) {
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = NeonColor.Cyan, strokeWidth = 2.dp)
            else Icon(painterResource(R.drawable.ic_ui_refresh), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(if (loading) "ACTUALIZANDO…" else "ACTUALIZAR", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
}
