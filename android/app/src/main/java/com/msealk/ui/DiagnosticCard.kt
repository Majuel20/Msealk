package com.msealk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msealk.data.AppSealPrefs
import com.msealk.data.AppSettings
import com.msealk.notifications.AppSealNotifications
import com.msealk.scheduler.AppSealScheduler

@Composable
fun DiagnosticCard(
    settings: AppSettings,
    cortesCount: Int,
    zonesCount: Int,
    matchesCount: Int,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canNotify = AppSealNotifications.canNotify(context)
    val lastError = AppSealPrefs.lastSyncError(context)

    GlassPanel(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Estado del sistema",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            NeonChip(if (lastError.isNotBlank() || !canNotify) "Revisar" else "Disponible", color = if (lastError.isNotBlank() || !canNotify) NeonColor.Orange else NeonColor.Green)
        }

        Spacer(Modifier.height(10.dp))

        AdaptiveRow {
            NeonMetric("Notificaciones", if (canNotify) "Permitidas" else "Sin permiso", Modifier.weight(1f), if (canNotify) NeonColor.Green else NeonColor.Red)
            NeonMetric("Sincronización", if (settings.autoSyncEnabled) "Activa" else "Desactivada", Modifier.weight(1f), NeonColor.Cyan)
        }
        Spacer(Modifier.height(8.dp))
        AdaptiveRow {
            NeonMetric("Cortes", cortesCount.toString(), Modifier.weight(1f), NeonColor.Purple)
            NeonMetric("Zonas", zonesCount.toString(), Modifier.weight(1f), NeonColor.Green)
        }
        Spacer(Modifier.height(8.dp))
        AdaptiveRow {
            NeonMetric("Coincidencias", matchesCount.toString(), Modifier.weight(1f), NeonColor.Orange)
            NeonMetric("Enviadas", AppSealPrefs.sentNotificationsCount(context).toString(), Modifier.weight(1f), NeonColor.Pink)
        }

        Spacer(Modifier.height(10.dp))
        DiagnosticLine("Próxima consulta", if (settings.autoSyncEnabled) AppSealScheduler.nextSyncDescription(settings) else "Desactivada")
        DiagnosticLine("Última actualización", AppSealPrefs.lastSyncText(context))
        DiagnosticLine("Último error", lastError.ifBlank { "Ninguno" })

        if (!canNotify) {
            Spacer(Modifier.height(10.dp))
            NeonButton(
                text = "Activar notificaciones",
                color = NeonColor.Red,
                modifier = Modifier.fillMaxWidth(),
                onClick = onRequestPermission
            )
            Spacer(Modifier.height(8.dp))
            NeonOutlinedButton(
                text = "Abrir ajustes de notificaciones",
                color = NeonColor.Red,
                modifier = Modifier.fillMaxWidth(),
                onClick = { AppSealNotifications.openNotificationSettings(context) }
            )
        }

        Spacer(Modifier.height(8.dp))
        NeonOutlinedButton(
            text = "Abrir ajustes de batería",
            color = NeonColor.Orange,
            modifier = Modifier.fillMaxWidth(),
            onClick = { AppSealNotifications.openBatterySettings(context) }
        )
    }
}

