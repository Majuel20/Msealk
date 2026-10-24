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
import com.msealk.BuildConfig
import com.msealk.data.*
import com.msealk.util.*

@Composable
internal fun DiagnosticScreen(
    settings: AppSettings,
    cortes: List<Corte>,
    zones: List<UserZone>,
    matchesCount: Int,
    hiddenOldCount: Int,
    status: String,
    onRequestPermission: () -> Unit,
    onOpenNotifications: () -> Unit,
    onPruneOldCuts: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.testTag("DiagnosticScreen").fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenHeader("Diagnóstico", "Estado de los datos y de tus notificaciones.") }
        item { DiagnosticCard(settings, cortes.size, zones.count { it.enabled }, matchesCount, onRequestPermission) }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                DiagnosticLine("Hash JSON", AppSealPrefs.lastJsonHash(context).take(12).ifBlank { "Sin hash" })
                DiagnosticLine("Cortes ocultos", hiddenOldCount.toString())
                DiagnosticLine("Estado", status)
            }
        }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Acciones", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AdaptiveRow {
                    NeonOutlinedButton("Permisos", color = NeonColor.Green, modifier = Modifier.weight(1f), onClick = onRequestPermission)
                    NeonOutlinedButton("Alertas", color = NeonColor.Purple, modifier = Modifier.weight(1f), onClick = onOpenNotifications)
                }
                Spacer(Modifier.height(8.dp))
                NeonOutlinedButton("Limpiar antiguos", color = NeonColor.Red, modifier = Modifier.fillMaxWidth(), onClick = onPruneOldCuts)
            }
        }
        item { AboutPanel() }
    }
}

@Composable
private fun AboutPanel() {
    val context = LocalContext.current
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LogoMark(compact = true)
            Column(modifier = Modifier.weight(1f)) {
                NeonTitle("MSealk")
                Text("Versión ${BuildConfig.VERSION_NAME} · Majuel20", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
                Text("Alertas de cortes programados SEAL Arequipa", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { ShareUtils.openUrl(context, SOCIAL_URL) }) { Text("Conoce al autor", color = NeonColor.Cyan) }
    }
}

