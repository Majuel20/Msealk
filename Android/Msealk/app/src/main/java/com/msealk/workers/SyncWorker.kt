package com.msealk.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.msealk.data.AppSealPrefs
import com.msealk.data.AppSealRepository
import com.msealk.scheduler.AppSealScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val cortes = AppSealRepository.updateFromRemote(applicationContext)
            val settings = AppSealPrefs.getSettings(applicationContext)
            AppSealScheduler.processAfterDataUpdate(
                context = applicationContext,
                cortes = cortes,
                settings = settings,
                manual = false
            )
            AppSealPrefs.addHistory(
                applicationContext,
                "Sincronización automática correcta. Cortes descargados: ${cortes.size}.",
                "sync"
            )
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            val message = e.message ?: "error desconocido"
            AppSealPrefs.setLastSyncError(applicationContext, message)
            AppSealPrefs.addHistory(applicationContext, "Error de sincronización: $message", "error")
            Result.retry()
        }
    }
}
