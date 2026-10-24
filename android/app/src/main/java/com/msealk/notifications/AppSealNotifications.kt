package com.msealk.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import com.msealk.R
import com.msealk.MainActivity
import com.msealk.data.AppSealPrefs

const val NOTIFICATION_CHANNEL_ID = "app_seal_alertas"

data class NotificationPayload(
    val notificationKey: String,
    val title: String,
    val body: String,
    val bigText: String,
    val type: String = "alert"
) {
    fun toData(): Data = Data.Builder()
        .putString("notification_key", notificationKey)
        .putString("title", title)
        .putString("body", body)
        .putString("big_text", bigText)
        .putString("type", type)
        .build()

    companion object {
        fun fromData(data: Data): NotificationPayload = NotificationPayload(
            notificationKey = data.getString("notification_key").orEmpty(),
            title = data.getString("title") ?: "Alerta SEAL",
            body = data.getString("body") ?: "Tienes un corte programado.",
            bigText = data.getString("big_text") ?: data.getString("body") ?: "Tienes un corte programado.",
            type = data.getString("type") ?: "alert"
        )
    }
}

object AppSealNotifications {
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Alertas de cortes SEAL",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de cortes programados de energía de SEAL Arequipa"
            enableVibration(true)
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < 33 ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showIfNew(context: Context, payload: NotificationPayload): Boolean {
        createChannel(context)
        if (!canNotify(context)) {
            AppSealPrefs.addHistory(
                context,
                "No se mostró una notificación porque falta permiso o están bloqueadas.",
                "warning"
            )
            return false
        }

        if (!AppSealPrefs.markNotificationSentIfNew(context, payload.notificationKey)) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            payload.notificationKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_power)
            .setContentTitle(payload.title.take(80))
            .setContentText(payload.body.take(130))
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.bigText.take(1200)))
            .setColor(android.graphics.Color.rgb(0, 229, 255))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(payload.notificationKey.hashCode(), notification)
            AppSealPrefs.addHistory(context, "Notificación enviada: ${payload.title}", "notification")
            true
        } catch (_: SecurityException) {
            AppSealPrefs.addHistory(context, "Android bloqueó la notificación por permiso denegado.", "warning")
            false
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= 26) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }


    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun showTestNotification(context: Context): Boolean {
        return showIfNew(
            context,
            NotificationPayload(
                notificationKey = "test|${System.currentTimeMillis()}",
                title = "MSealk listo",
                body = "Las notificaciones están funcionando correctamente.",
                bigText = "Prueba de notificación de MSealk. Si ves este aviso, Android permite que la app te avise sobre cortes programados.",
                type = "test"
            )
        )
    }

    fun openBatterySettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(fallback)
        }
    }
}
