package com.msealk.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.msealk.data.*
import com.msealk.scheduler.AppSealScheduler
import com.msealk.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreen(
    settings: AppSettings,
    cortes: List<Corte>,
    zones: List<UserZone>,
    matchesCount: Int,
    hiddenOldCount: Int,
    onSettingsChanged: (AppSettings) -> Unit,
    onPruneOldCuts: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenDiagnostic: () -> Unit
) {
    val context = LocalContext.current
    var draft by rememberSaveable(settings, stateSaver = Saver<AppSettings, String>(
        save = { it.toJson().toString() }, restore = { AppSettings.fromJson(it) }
    )) { mutableStateOf(settings) }
    val dirty = draft.normalized() != settings.normalized()
    val syncFlow = remember(context) { androidx.work.WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(AppSealScheduler.SYNC_WORK) }
    val syncWork by syncFlow.collectAsState(initial = emptyList())
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { clock = System.currentTimeMillis(); kotlinx.coroutines.delay(30_000) }
    }
    val lastSync = AppSealPrefs.lastSyncMillis(context)
    val now = java.time.Instant.ofEpochMilli(clock).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    val scheduleChanged = com.msealk.scheduler.ScheduleRules.syncKey(draft) != com.msealk.scheduler.ScheduleRules.syncKey(settings)
    val work = syncWork.firstOrNull { !it.state.isFinished }
    val nextMillis = work?.nextScheduleTimeMillis?.takeIf { it != Long.MAX_VALUE }

    Box(Modifier.fillMaxSize()) {

    LazyColumn(
        modifier = Modifier.testTag("SettingsScreen").fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = if (dirty) 100.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenHeader("Ajustes", "Configuración clara: datos, alertas, agenda y limpieza.") }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Estado rápido", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AdaptiveRow {
                    NeonMetric("Cortes", cortes.size.toString(), Modifier.weight(1f), NeonColor.Cyan)
                    NeonMetric("Coincidencias", matchesCount.toString(), Modifier.weight(1f), NeonColor.Purple)
                    NeonMetric("Zonas", zones.count { it.enabled }.toString(), Modifier.weight(1f), NeonColor.Green)
                }
                Spacer(Modifier.height(8.dp))
                AdaptiveRow {
                    NeonOutlinedButton("Alertas", color = NeonColor.Green, modifier = Modifier.weight(1f), onClick = onOpenNotifications)
                    NeonOutlinedButton("Diagnóstico", color = NeonColor.Cyan, modifier = Modifier.weight(1f), onClick = onOpenDiagnostic)
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Actualización automática", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                CompactSwitchRow(
                    title = "Actualizar en segundo plano",
                    subtitle = if (draft.autoSyncEnabled) AppSealScheduler.nextSyncDescription(draft) else "Sincronización automática desactivada",
                    checked = draft.autoSyncEnabled,
                    onCheckedChange = { draft = draft.copy(autoSyncEnabled = it) }
                )
                Spacer(Modifier.height(8.dp))
                CompactSwitchRow(
                    title = "Solo lunes",
                    subtitle = if (draft.syncEveryMonday) "Revisión semanal fija" else "Usa intervalo por días",
                    checked = draft.syncEveryMonday,
                    enabled = draft.autoSyncEnabled,
                    onCheckedChange = { draft = draft.copy(syncEveryMonday = it) }
                )
                Spacer(Modifier.height(8.dp))
                NumberAdjustRow("Cada", draft.syncIntervalDays, "días", draft.autoSyncEnabled && !draft.syncEveryMonday, 1, 7) {
                    draft = draft.copy(syncIntervalDays = it)
                }
                Spacer(Modifier.height(8.dp))
                TimeAdjustRow("Hora de revisión", draft.syncHour, draft.syncMinute, draft.autoSyncEnabled) { h, m ->
                    draft = draft.copy(syncHour = h, syncMinute = m)
                }
                Spacer(Modifier.height(12.dp))
                Text("Última actualización correcta: ${if (lastSync > 0) com.msealk.scheduler.ScheduleRules.formatInstant(lastSync) else "Todavía no se han descargado datos"}", color = NeonColor.Muted)
                val nextText = when {
                    !draft.autoSyncEnabled -> "Desactivada"
                    scheduleChanged -> "Al guardar: " + com.msealk.scheduler.ScheduleRules.formatDateTime(
                        com.msealk.scheduler.ScheduleRules.nextSync(draft, now, lastSync))
                    work?.state == androidx.work.WorkInfo.State.RUNNING -> "Actualizando ahora"
                    nextMillis != null && nextMillis <= clock -> "Pendiente desde " + com.msealk.scheduler.ScheduleRules.formatInstant(nextMillis)
                    nextMillis != null -> com.msealk.scheduler.ScheduleRules.formatInstant(nextMillis)
                    else -> "Preparando programación…"
                }
                Text("Próxima actualización estimada: $nextText", color = NeonColor.Cyan)
                Text("Hora local del teléfono. Android puede retrasar la revisión por conexión o ahorro de batería. GitHub publica habitualmente desde las 05:17 de Perú; la revisión predeterminada es a las 06:30.",
                    color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)

            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Tus avisos", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Elige qué avisos recibir y cuándo recordarlos.", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                CompactSwitchRow("Alertas activas", "Permite programar avisos según tus zonas", draft.alertsEnabled) { draft = draft.copy(alertsEnabled = it) }
                Spacer(Modifier.height(8.dp))
                CompactSwitchRow("Cortes nuevos", "Aviso al detectar un nuevo corte coincidente", draft.notifyNewCuts, draft.alertsEnabled) { draft = draft.copy(notifyNewCuts = it) }
                Spacer(Modifier.height(8.dp))
                CompactSwitchRow("Cortes modificados", "Aviso si cambia fecha, hora o zona", draft.notifyChangedCuts, draft.alertsEnabled) { draft = draft.copy(notifyChangedCuts = it) }
                Spacer(Modifier.height(8.dp))
                CompactSwitchRow("Aviso de la víspera", "A la hora elegida, el día anterior al corte", draft.notifyOneDayBefore, draft.alertsEnabled) { draft = draft.copy(notifyOneDayBefore = it) }
                Spacer(Modifier.height(8.dp))
                TimeAdjustRow("Hora del aviso de la víspera", draft.oneDayHour, draft.oneDayMinute, draft.alertsEnabled && draft.notifyOneDayBefore) { h, m -> draft = draft.copy(oneDayHour = h, oneDayMinute = m) }
                Spacer(Modifier.height(8.dp))
                CompactSwitchRow("Recordatorio antes del corte", "Usa tu hora del mismo día. Si coincide con el inicio o es posterior, avisa 90 minutos antes; puede ser la noche anterior", draft.notifySameDayMorning, draft.alertsEnabled) { draft = draft.copy(notifySameDayMorning = it) }
                Spacer(Modifier.height(8.dp))
                TimeAdjustRow("Hora preferida del recordatorio", draft.sameDayHour, draft.sameDayMinute, draft.alertsEnabled && draft.notifySameDayMorning) { h, m -> draft = draft.copy(sameDayHour = h, sameDayMinute = m) }
                Spacer(Modifier.height(8.dp))
                Text("Si los datos llegan tarde, solo se recupera un aviso si faltan al menos 15 minutos para el corte. Los recordatorios vencidos se descartan.", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Herramientas de cada corte", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Guarda un corte en tu calendario, comparte el aviso o consulta la publicación original.", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                SettingsInfoLine("Calendario", "abre el calendario con fecha, hora, ubicación, motivo, zona y fuente para guardar el evento", NeonColor.Green)
                SettingsInfoLine("Copiar aviso", "copia una plantilla lista para pegar donde quieras", NeonColor.Purple)
                SettingsInfoLine("Fuente", "abre el enlace oficial del corte para validarlo", NeonColor.Cyan)
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Historial y cortes pasados", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                CompactSwitchRow("Mostrar cortes finalizados", "Conserva el contexto de las interrupciones recientes", draft.showPastGhosts) { draft = draft.copy(showPastGhosts = it) }
                Spacer(Modifier.height(8.dp))
                NumberAdjustRow("Conservar pasados", draft.keepPastDays, "días", true, 0, 30) { draft = draft.copy(keepPastDays = it) }
                if (hiddenOldCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Ocultos actualmente: $hiddenOldCount", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                NeonOutlinedButton("Eliminar cortes antiguos", color = NeonColor.Red, modifier = Modifier.fillMaxWidth(), onClick = onPruneOldCuts)
            }
        }

        item {
            GlassPanel(Modifier.fillMaxWidth()) {
                NeonTitle("MSealk")
                Text("By Majuel20", color = NeonColor.Cyan, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Text("Comparte la app y su página de descarga con tus contactos.", color = NeonColor.Muted)
                Spacer(Modifier.height(12.dp))
                NeonButton("Compartir app", modifier = Modifier.fillMaxWidth(), onClick = { ShareUtils.shareApp(context) })
                Spacer(Modifier.height(8.dp))
                NeonOutlinedButton("Ver en GitHub", modifier = Modifier.fillMaxWidth(), onClick = { ShareUtils.openUrl(context, ShareUtils.APP_DOWNLOAD_URL) })
            }
        }
    }
        if (dirty) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = NeonColor.Dark2,
                shadowElevation = 8.dp) {
                NeonButton("Guardar cambios", modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth(), onClick = {
                    onSettingsChanged(draft.normalized())
                    android.widget.Toast.makeText(context, "Cambios guardados", android.widget.Toast.LENGTH_SHORT).show()
                })
            }
        }
    }
}


@Composable
private fun SettingsInfoLine(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        NeonChip(label, color = color)
        Spacer(Modifier.width(8.dp))
        Text(value, color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

