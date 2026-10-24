package com.msealk.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.msealk.data.AppSealPrefs
import com.msealk.data.AppSealRepository
import com.msealk.notifications.AppSealNotifications
import com.msealk.notifications.NotificationPayload
import com.msealk.util.TextMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = AppSealPrefs.getSettings(applicationContext)
        val type = inputData.getString("reminder_type")
        val enabled = settings.alertsEnabled && when (type) {
            "one_day" -> settings.notifyOneDayBefore
            "today" -> settings.notifySameDayMorning
            else -> false // Trabajos antiguos sin vencimiento: no se entregan.
        }
        if (!enabled || System.currentTimeMillis() >= inputData.getLong("deadline", 0)) return@withContext Result.success()
        val matches = TextMatch.findMatches(AppSealRepository.loadLocal(applicationContext), AppSealPrefs.getZones(applicationContext))
        val match = matches.firstOrNull { it.matchKey == inputData.getString("match_key") }
            ?: return@withContext Result.success()
        if (match.corte.contentHash() != inputData.getString("content_hash") ||
            match.corte.inicioDateTime()?.isAfter(LocalDateTime.now()) != true) return@withContext Result.success()
        AppSealNotifications.showIfNew(applicationContext, NotificationPayload.fromData(inputData))
        Result.success()
    }
}
