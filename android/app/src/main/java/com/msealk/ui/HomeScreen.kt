package com.msealk.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.msealk.data.*
import com.msealk.util.*
import java.time.LocalDateTime

@Composable
internal fun HomeScreen(
    settings: AppSettings,
    cortes: List<Corte>,
    matches: List<CorteZoneMatch>,
    zones: List<UserZone>,
    activeZonesCount: Int,
    hiddenOldCount: Int,
    status: String,
    loading: Boolean,
    permissionOk: Boolean,
    now: LocalDateTime,
    onRefresh: () -> Unit,
    onOpenZones: () -> Unit,
    onOpenDiagnostic: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(HomeFilter.MATCHES) }
    var selectedZoneId by rememberSaveable { mutableStateOf<String?>(null) }
    val today = now.toLocalDate()
    val context = LocalContext.current
    val overview = supplyOverview(matches, activeZonesCount, AppSealPrefs.lastSyncMillis(context) > 0, now)

    LaunchedEffect(zones) {
        if (selectedZoneId != null && zones.none { it.id == selectedZoneId }) selectedZoneId = null
    }

    val zoneMatches = matches.filter { selectedZoneId == null || it.zone.id == selectedZoneId }
    val filteredMatches = when (filter) {
        HomeFilter.MATCHES -> zoneMatches
        HomeFilter.TODAY -> zoneMatches.filter { it.corte.inicioDateTime()?.toLocalDate() == today }
        HomeFilter.WEEK -> zoneMatches.filter { match ->
            val date = match.corte.inicioDateTime()?.toLocalDate()
            date != null && !date.isBefore(today) && !date.isAfter(today.plusDays(6))
        }
        HomeFilter.ALL -> emptyList()
    }
    val filteredCortes = when (filter) {
        HomeFilter.ALL -> cortes.sortedBy { it.inicioDateTime() ?: LocalDateTime.MAX }
        else -> emptyList()
    }

    LazyColumn(
        modifier = Modifier.testTag("HomeScreen")
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("Inicio", "Tu energía, bajo control.")
        }
        item {
            HomeOverview(overview, AppSealPrefs.lastSyncText(context), loading,
                AppSealPrefs.lastSyncError(context).isNotBlank(), onRefresh)
        }
        item {
            AdaptiveRow {
                NeonMetric("Cortes visibles", cortes.size.toString(), Modifier.weight(1f))
                NeonMetric("Coincidencias", matches.size.toString(), Modifier.weight(1f), NeonColor.Purple)
            }
        }
        item {
            GlassPanel(Modifier.fillMaxWidth()) {
                SectionHeading("Tu seguimiento")
                Spacer(Modifier.height(6.dp))
                Text("${if (activeZonesCount == 1) "1 zona activa" else "$activeZonesCount zonas activas"} · ${shortSyncText(settings)}", color = NeonColor.Muted)
                if (!permissionOk) {
                    Spacer(Modifier.height(8.dp))
                    Text("Activa el permiso de notificaciones para recibir avisos.", color = NeonColor.Orange)
                }
                Spacer(Modifier.height(12.dp))
                AdaptiveRow {
                    NeonOutlinedButton("Mis zonas", modifier = Modifier.weight(1f), onClick = onOpenZones)
                    NeonOutlinedButton("Alertas", modifier = Modifier.weight(1f), onClick = onOpenNotifications)
                }
                TextButton(onClick = onOpenDiagnostic) { Text("Ver diagnóstico", color = NeonColor.Muted) }
                if (status != "Listo" && !loading && !status.startsWith("Error:")) {
                    Text(status, color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                if (hiddenOldCount > 0) Text("$hiddenOldCount cortes antiguos ocultos", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }

        item { CompactFilterRow(selected = filter, onSelected = { filter = it }) }

        if (zones.any { it.enabled } && filter != HomeFilter.ALL) {
            item { ZoneChipRow(zones.filter { it.enabled }, selectedZoneId) { selectedZoneId = it } }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (filter) {
                        HomeFilter.MATCHES -> "Cortes coincidentes"
                        HomeFilter.TODAY -> "Cortes de hoy"
                        HomeFilter.WEEK -> "Próximos 7 días"
                        HomeFilter.ALL -> "Todos los cortes"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                val count = if (filter == HomeFilter.ALL) filteredCortes.size else filteredMatches.size
                NeonChip(count.toString(), color = if (count > 0) NeonColor.Purple else NeonColor.Muted)
            }
        }

        if (activeZonesCount == 0 && filter != HomeFilter.ALL) {
            item { EmptyCard("Primero agrega una zona", "Guarda un distrito, urbanización, subestación o calle para encontrar tus coincidencias.") }
            item { NeonButton("Agregar zona", modifier = Modifier.fillMaxWidth(), onClick = onOpenZones) }
        } else if (filter == HomeFilter.ALL) {
            if (filteredCortes.isEmpty()) item { EmptyCard("Sin cortes visibles", "Pulsa Actualizar o aumenta los días pasados visibles desde Ajustes.") }
            else items(filteredCortes) { corte ->
                val cutMatches = matches.filter { it.corte.stableKey == corte.stableKey }
                CorteCard(
                    corte = corte,
                    zonaGuardada = when {
                        cutMatches.isEmpty() -> null
                        cutMatches.size == 1 -> cutMatches.first().zone.name
                        else -> "${cutMatches.size} zonas"
                    },
                    coincidencias = cutMatches.flatMap { it.matchedKeywords }.distinct().take(4),
                    zoneColorKey = cutMatches.firstOrNull()?.zone?.colorKey
                )
            }
        } else {
            if (filteredMatches.isEmpty()) item { EmptyCard("Sin resultados", "No hay cortes para este filtro. Prueba Todos o ajusta tus palabras clave.") }
            else items(filteredMatches) { match -> CorteCard(match) }
        }
    }
}

@Composable
private fun CompactFilterRow(selected: HomeFilter, onSelected: (HomeFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(HomeFilter.values().toList()) { item ->
            NeonFilter(item.title, selected == item, { onSelected(item) })
        }
    }
}

@Composable
private fun ZoneChipRow(zones: List<UserZone>, selectedZoneId: String?, onSelected: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        item {
            NeonFilter("Todas las zonas", selectedZoneId == null, { onSelected(null) })
        }
        items(zones) { zone ->
            val color = zoneColorByKey(zone.colorKey)
            NeonFilter(zone.name, selectedZoneId == zone.id, { onSelected(zone.id) }, color)
        }
    }
}

