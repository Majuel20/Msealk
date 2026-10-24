package com.msealk.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.msealk.data.*
import com.msealk.util.*
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
internal fun HistoryScreen(
    cortes: List<Corte>,
    matches: List<CorteZoneMatch>,
    history: List<HistoryItem>,
    version: Int,
    hiddenOldCount: Int,
    onClear: () -> Unit
) {
    var view by rememberSaveable { mutableStateOf(HistoryView.UPCOMING) }
    val localHistory = remember(version, history) { history }
    val now = LocalDateTime.now()
    val upcomingMatches = matches
        .filter { (it.corte.finDateTime() ?: it.corte.inicioDateTime() ?: LocalDateTime.MAX).isAfter(now) }
        .sortedBy { it.corte.inicioDateTime() ?: LocalDateTime.MAX }
    val allCortes = cortes.sortedBy { it.inicioDateTime() ?: LocalDateTime.MAX }
    val matchesByCut = matches.groupBy { it.corte.stableKey }

    LazyColumn(
        modifier = Modifier.testTag("HistoryScreen").fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScreenHeader("Cortes programados", "Próximas interrupciones y registro de actividad.")
                if (view == HistoryView.EVENTS && localHistory.isNotEmpty()) NeonOutlinedButton("Limpiar", color = NeonColor.Red, onClick = onClear)
            }
        }
        item { HistoryTabs(selected = view, onSelected = { view = it }) }
        if (hiddenOldCount > 0) item { Text("Ocultos $hiddenOldCount cortes antiguos según Ajustes.", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall) }

        when (view) {
            HistoryView.UPCOMING -> {
                if (upcomingMatches.isEmpty()) item { EmptyCard("Sin cortes próximos", "No hay cortes futuros que coincidan con tus zonas activas.") }
                else upcomingMatches.groupBy { it.corte.inicioDateTime()?.toLocalDate() }.entries.sortedBy { it.key ?: LocalDate.MAX }.forEach { section ->
                    item { DayHeader(section.key) }
                    items(section.value) { match -> CorteCard(match) }
                }
            }
            HistoryView.ALL -> {
                if (allCortes.isEmpty()) item { EmptyCard("Sin cortes descargados", "Pulsa Actualizar en Inicio para consultar las interrupciones publicadas.") }
                else allCortes.groupBy { it.inicioDateTime()?.toLocalDate() }.entries.sortedBy { it.key ?: LocalDate.MAX }.forEach { section ->
                    item { DayHeader(section.key) }
                    items(section.value) { corte ->
                        val cutMatches = matchesByCut[corte.stableKey].orEmpty()
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
                }
            }
            HistoryView.EVENTS -> {
                if (localHistory.isEmpty()) item { EmptyCard("Sin eventos", "Aquí aparecerán sincronizaciones, errores y notificaciones.") }
                else items(localHistory) { item ->
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            NeonChip(historyLabel(item.type), color = historyColor(item.type))
                            Text(AppSealPrefs.formatMillis(item.timestampMillis), color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.message, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTabs(selected: HistoryView, onSelected: (HistoryView) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(HistoryView.values().toList()) { item ->
            NeonFilter(item.title, selected == item, { onSelected(item) })
        }
    }
}

