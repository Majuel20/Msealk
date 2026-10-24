package com.msealk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.msealk.data.*
import com.msealk.notifications.AppSealNotifications
import com.msealk.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotificationsScreen(
    settings: AppSettings,
    permissionOk: Boolean,
    zonesCount: Int,
    matchesCount: Int,
    onRequestPermission: () -> Unit,
    onPermissionStateChanged: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.testTag("NotificationsScreen").fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenHeader("Alertas", "Mantente informado, con avisos a tu medida.") }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (permissionOk) "Permiso concedido" else "Falta permiso", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(if (permissionOk) "Android permite mostrar alertas." else "Activa el permiso para recibir avisos.", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    NeonChip(if (permissionOk) "Permitidas" else "Revisar", color = if (permissionOk) NeonColor.Green else NeonColor.Red)
                }
                Spacer(Modifier.height(10.dp))
                AdaptiveRow {
                    NeonButton("Permisos", color = if (permissionOk) NeonColor.Green else NeonColor.Red, modifier = Modifier.weight(1f)) {
                        onRequestPermission()
                        onPermissionStateChanged()
                    }
                    NeonOutlinedButton("Enviar prueba", enabled = permissionOk, color = NeonColor.Cyan, modifier = Modifier.weight(1f)) {
                        AppSealNotifications.showTestNotification(context)
                        onPermissionStateChanged()
                    }
                }
                Spacer(Modifier.height(8.dp))
                AdaptiveRow {
                    NeonOutlinedButton("Ajustes de Android", color = NeonColor.Purple, modifier = Modifier.weight(1f)) { AppSealNotifications.openNotificationSettings(context) }
                    NeonOutlinedButton("Batería", color = NeonColor.Orange, modifier = Modifier.weight(1f)) { AppSealNotifications.openBatterySettings(context) }
                }
            }
        }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Así se verán tus avisos", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Los avisos priorizan lo que necesitas saber rápido:", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                NotificationPreview("Nuevo corte SEAL en Casa", "Ejemplo · 06:00 - 14:00 · Cerro Colorado")
                NotificationPreview("Mañana tienes corte SEAL", "Casa · 06:00 - 14:00 · Urb. Cayma · mantenimiento")
                NotificationPreview("Agenda del corte", "Desde la tarjeta puedes crear evento con ubicación, motivo, SED y fuente")
            }
        }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Resumen", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AdaptiveRow {
                    NeonMetric("Zonas", zonesCount.toString(), Modifier.weight(1f), NeonColor.Green)
                    NeonMetric("Coincidencias", matchesCount.toString(), Modifier.weight(1f), NeonColor.Purple)
                    NeonMetric("Alertas", if (settings.alertsEnabled) "Activas" else "Desactivadas", Modifier.weight(1f), if (settings.alertsEnabled) NeonColor.Green else NeonColor.Muted)
                }
            }
        }
    }
}

@Composable
private fun NotificationPreview(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, NeonColor.Cyan.copy(alpha = 0.20f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(body, color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(8.dp))
}

