package com.msealk.scheduler

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.msealk.data.AppSealPrefs
import com.msealk.data.AppSettings
import com.msealk.data.Corte
import com.msealk.data.KnownMatchState
import com.msealk.data.sha256
import com.msealk.notifications.AppSealNotifications
import com.msealk.notifications.NotificationPayload
import com.msealk.util.CorteZoneMatch
import com.msealk.util.DateParser
import com.msealk.util.TextMatch
import com.msealk.workers.NotificationWorker
import com.msealk.workers.SyncWorker
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object AppSealScheduler {
    const val SYNC_WORK = "app_seal_auto_sync"
    private const val ALERTS_TAG = "app_seal_alerts"

    fun applyConfiguration(context: Context, settings: AppSettings, cortes: List<Corte>) {
        val normalized = settings.normalized()
        val previous = AppSealPrefs.getSettings(context)
        AppSealPrefs.saveSettings(context, normalized)

        if (normalized.autoSyncEnabled) {
            scheduleAutomaticSync(context, normalized, reset = ScheduleRules.syncKey(previous) != ScheduleRules.syncKey(normalized))
        } else {
            cancelAutomaticSync(context)
        }

        processAfterDataUpdate(context, cortes, normalized, manual = true)
    }

    fun scheduleAutomaticSync(context: Context, settings: AppSettings = AppSealPrefs.getSettings(context), reset: Boolean = false) {
        val normalized = settings.normalized()
        val intervalDays = if (normalized.syncEveryMonday) 7 else normalized.syncIntervalDays.coerceIn(1, 7)
        val now = LocalDateTime.now()
        val initialDelayMillis = Duration.between(now,
            ScheduleRules.nextSync(normalized, now, AppSealPrefs.lastSyncMillis(context))).toMillis().coerceAtLeast(0)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(intervalDays.toLong(), TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK,
            if (reset) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelAutomaticSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK)
    }

    fun cancelAlerts(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(ALERTS_TAG)
    }

    /**
     * Se llama después de cada descarga manual o automática.
     * Aquí se detectan nuevos/cambiados/eliminados, se actualiza el estado local y se programan alertas.
     */
    fun processAfterDataUpdate(
        context: Context,
        cortes: List<Corte>,
        settings: AppSettings = AppSealPrefs.getSettings(context),
        manual: Boolean
    ) {
        val zones = AppSealPrefs.getZones(context)
        val matches = TextMatch.findMatches(cortes, zones)
        val previous = AppSealPrefs.getKnownMatches(context)
        val nowMillis = System.currentTimeMillis()

        val current = mutableMapOf<String, KnownMatchState>()
        for (match in matches) {
            val previousState = previous[match.matchKey]
            current[match.matchKey] = KnownMatchState(
                contentHash = match.corte.contentHash(),
                label = match.corte.shortLabel(),
                zoneName = match.zone.name,
                firstSeenMillis = previousState?.firstSeenMillis ?: nowMillis,
                lastSeenMillis = nowMillis
            )
        }

        val isFirstScan = previous.isEmpty()
        val newMatches = matches.filter { it.matchKey !in previous }
        val changedMatches = matches.filter { match ->
            val old = previous[match.matchKey]
            old != null && old.contentHash != match.corte.contentHash()
        }
        val removedMatches = previous.filterKeys { it !in current }

        AppSealPrefs.saveKnownMatches(context, current)

        if (settings.alertsEnabled) {
            if (!isFirstScan || manual) {
                notifyNewAndChanged(context, settings, newMatches, changedMatches, removedMatches)
            }
            scheduleAlerts(context, settings, matches)
        } else {
            cancelAlerts(context)
        }

        if (newMatches.isNotEmpty()) {
            AppSealPrefs.addHistory(context, "Nuevos cortes coincidentes detectados: ${newMatches.size}.", "alert")
        }
        if (changedMatches.isNotEmpty()) {
            AppSealPrefs.addHistory(context, "Cortes coincidentes actualizados: ${changedMatches.size}.", "alert")
        }
        if (removedMatches.isNotEmpty()) {
            AppSealPrefs.addHistory(context, "Cortes coincidentes eliminados del JSON: ${removedMatches.size}.", "alert")
        }
    }

    private fun notifyNewAndChanged(
        context: Context,
        settings: AppSettings,
        newMatches: List<CorteZoneMatch>,
        changedMatches: List<CorteZoneMatch>,
        removedMatches: Map<String, KnownMatchState>
    ) {
        if (settings.notifyNewCuts) {
            newMatches.filter { it.corte.inicioDateTime()?.isAfter(LocalDateTime.now()) == true }.forEach { match ->
                AppSealNotifications.showIfNew(
                    context,
                    payloadForCorte(
                        key = "new|${match.matchKey}",
                        title = "Nuevo corte SEAL en ${match.zone.name}",
                        headline = "Se detectó un nuevo corte que coincide con tu zona.",
                        match = match
                    )
                )
            }
        }

        if (settings.notifyChangedCuts) {
            changedMatches.filter { it.corte.inicioDateTime()?.isAfter(LocalDateTime.now()) == true }.forEach { match ->
                AppSealNotifications.showIfNew(
                    context,
                    payloadForCorte(
                        key = "changed|${match.matchKey}|${match.corte.contentHash()}",
                        title = "Corte SEAL actualizado",
                        headline = "Un corte que coincide con ${match.zone.name} cambió su información.",
                        match = match
                    )
                )
            }
        }

        if (settings.notifyRemovedCuts) {
            removedMatches.forEach { (key, state) ->
                val payload = NotificationPayload(
                    notificationKey = "removed|$key|${state.contentHash}",
                    title = "Corte SEAL eliminado",
                    body = "Ya no aparece un corte asociado a ${state.zoneName}.",
                    bigText = "Ya no aparece en el JSON un corte que antes coincidía con ${state.zoneName}.\n\n${state.label}",
                    type = "removed"
                )
                AppSealNotifications.showIfNew(context, payload)
            }
        }
    }

    fun scheduleAlerts(context: Context, settings: AppSettings, matches: List<CorteZoneMatch>) {
        val normalized = settings.normalized()
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag(ALERTS_TAG)

        if (!normalized.alertsEnabled) return

        val now = LocalDateTime.now()
        for (match in matches) {
            val start = match.corte.inicioDateTime() ?: continue

            if (normalized.notifyOneDayBefore) {
                val alertAt = start.toLocalDate().minusDays(1)
                    .atTime(normalized.oneDayHour, normalized.oneDayMinute)
                scheduleOneAlert(context, match, "one_day", "Mañana tienes corte SEAL", alertAt, start, now)
            }

            if (normalized.notifySameDayMorning) {
                val alertAt = ScheduleRules.sameDayReminder(start, normalized.sameDayHour, normalized.sameDayMinute)
                val previousAt = start.toLocalDate().minusDays(1).atTime(normalized.oneDayHour, normalized.oneDayMinute)
                if (!normalized.notifyOneDayBefore || alertAt != previousAt) {
                    scheduleOneAlert(context, match, "today", "Recordatorio de corte SEAL", alertAt, start, now)
                }
            }
        }
    }

    private fun scheduleOneAlert(
        context: Context,
        match: CorteZoneMatch,
        type: String,
        title: String,
        alertAt: LocalDateTime,
        corteStart: LocalDateTime,
        now: LocalDateTime
    ) {
        val key = "alert|$type|${match.matchKey}"
        val payload = payloadForCorte(
            key = key,
            title = title,
            headline = title,
            match = match
        )

        val delayMillis = Duration.between(now, alertAt).toMillis()
        if (delayMillis <= 0L) {
            // Si la app sincronizó tarde, evita perder el aviso de mañana/hoy mientras el corte todavía no inicia.
            if (ScheduleRules.canCatchUp(type, alertAt, corteStart, now)) {
                AppSealNotifications.showIfNew(context, payload)
            }
            return
        }

        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(androidx.work.Data.Builder()
                .putAll(payload.copy(bigText = payload.bigText.take(1800), body = payload.body.take(300)).toData())
                .putString("match_key", match.matchKey)
                .putString("content_hash", match.corte.contentHash())
                .putString("reminder_type", type)
                .putLong("deadline", ScheduleRules.deadline(type, alertAt, corteStart)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                .build())
            .addTag(ALERTS_TAG)
            .build()

        val uniqueName = "alert_${sha256(key).take(24)}"
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun payloadForCorte(
        key: String,
        title: String,
        headline: String,
        match: CorteZoneMatch
    ): NotificationPayload {
        val corte = match.corte
        val body = listOf(
            match.zone.name,
            corte.fechaBonita(),
            corte.rangoHora(),
            corte.distrito.ifBlank { corte.zonaAfectada }
        ).filter { it.isNotBlank() }.joinToString(" · ")
        val keywords = if (match.matchedKeywords.isNotEmpty()) {
            "\nCoincidencias: ${match.matchedKeywords.joinToString(", ")}"
        } else {
            ""
        }
        val big = buildString {
            appendLine(headline)
            appendLine()
            appendLine("Día/fecha: ${corte.fechaBonita()}")
            if (corte.rangoHora().isNotBlank()) appendLine("Hora: ${corte.rangoHora()}")
            if (corte.distrito.isNotBlank()) appendLine("Distrito: ${corte.distrito}")
            if (corte.zonaAfectada.isNotBlank()) appendLine("Zona afectada: ${corte.zonaAfectada}")
            if (corte.ubicacion.isNotBlank()) appendLine("Ubicación: ${corte.ubicacion}")
            if (corte.motivo.isNotBlank()) appendLine("Motivo: ${corte.motivo}")
            if (corte.subestaciones.isNotBlank()) appendLine("SED/Subestación: ${corte.subestaciones}")
            append("Zona guardada: ${match.zone.name}")
            append(keywords)
        }
        return NotificationPayload(
            notificationKey = key,
            title = title,
            body = body.ifBlank { "Corte programado para ${match.zone.name}" },
            bigText = big,
            type = "corte"
        )
    }

    fun nextSyncDescription(settings: AppSettings): String {
        val textTime = DateParser.formatTime(settings.syncHour, settings.syncMinute)
        return if (settings.syncEveryMonday) {
            "Cada lunes, alrededor de las $textTime"
        } else {
            "${if (settings.syncIntervalDays.coerceIn(1, 7) == 1) "Cada día" else "Cada ${settings.syncIntervalDays.coerceIn(1, 7)} días"}, alrededor de las $textTime"
        }
    }
}
