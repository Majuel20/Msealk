package com.msealk.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.msealk.notifications.AppSealNotifications
import com.msealk.notifications.NotificationPayload

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val payload = NotificationPayload.fromData(inputData)
        AppSealNotifications.showIfNew(applicationContext, payload)
        return Result.success()
    }
}
