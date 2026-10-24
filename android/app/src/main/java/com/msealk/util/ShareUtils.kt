package com.msealk.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import com.msealk.data.Corte
import java.time.ZoneId

object ShareUtils {
    const val APP_DOWNLOAD_URL = "https://github.com/Majuel20/MSealk/releases"

    internal fun appShareIntent(): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "MSealk · by Majuel20")
        putExtra(Intent.EXTRA_TEXT, "MSealk · by Majuel20\nConsulta los cortes programados de SEAL Arequipa y recibe avisos para tus zonas.\nDescarga la app en GitHub:\n$APP_DOWNLOAD_URL")
    }

    fun shareApp(context: Context) {
        val chooser = Intent.createChooser(appShareIntent(), "Compartir MSealk")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("MSealk", APP_DOWNLOAD_URL))
            Toast.makeText(context, "Enlace de la app copiado", Toast.LENGTH_SHORT).show()
        }
    }

    fun openUrl(context: Context, url: String) {
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun corteMessage(corte: Corte, zoneName: String? = null): String = corte.toFriendlyMessage(zoneName)

    /**
     * Crea una ficha profesional en el calendario del teléfono.
     * No necesita permiso de calendario porque se abre la app Calendario para que el usuario confirme.
     */
    fun addCorteToCalendar(context: Context, corte: Corte, zoneName: String? = null) {
        val start = corte.inicioDateTime()
        val end = corte.finDateTime() ?: start?.plusHours(1)
        val zoneId = ZoneId.systemDefault()
        val startMillis = start?.atZone(zoneId)?.toInstant()?.toEpochMilli()
        val endMillis = end?.atZone(zoneId)?.toInstant()?.toEpochMilli()
        val titleZone = zoneName?.takeIf { it.isNotBlank() } ?: corte.distrito.ifBlank { "SEAL" }
        val location = corte.ubicacion.ifBlank { corte.distrito }.ifBlank { corte.zonaAfectada }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "⚡ Corte SEAL - $titleZone")
            putExtra(CalendarContract.Events.DESCRIPTION, calendarDescription(corte, zoneName))
            if (location.isNotBlank()) putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            startMillis?.let { putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
            endMillis?.let { putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching { context.startActivity(intent) }.onFailure {
            shareCorte(context, corte, zoneName)
        }
    }

    fun copyCorteToClipboard(context: Context, corte: Corte, zoneName: String? = null) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Corte SEAL", corteMessage(corte, zoneName)))
        Toast.makeText(context, "Aviso copiado", Toast.LENGTH_SHORT).show()
    }

    fun shareCorte(context: Context, corte: Corte, zoneName: String? = null) {
        val text = corteMessage(corte, zoneName)
        val chooser = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "Compartir corte"
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        try {
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            copyCorteToClipboard(context, corte, zoneName)
        }
    }

    private fun calendarDescription(corte: Corte, zoneName: String? = null): String = buildString {
        appendLine("⚡ Corte programado SEAL")
        if (!zoneName.isNullOrBlank()) appendLine("Zona guardada: $zoneName")
        if (corte.fecha.isNotBlank()) appendLine("Día/fecha: ${corte.fechaBonita()}")
        if (corte.rangoHora().isNotBlank()) appendLine("Hora: ${corte.rangoHora()}")
        if (corte.distrito.isNotBlank()) appendLine("Distrito: ${corte.distrito}")
        if (corte.zonaAfectada.isNotBlank()) appendLine("Zona afectada: ${corte.zonaAfectada}")
        if (corte.ubicacion.isNotBlank()) appendLine("Ubicación: ${corte.ubicacion}")
        if (corte.motivo.isNotBlank()) appendLine("Motivo: ${corte.motivo}")
        if (corte.subestaciones.isNotBlank()) appendLine("SED/Subestación: ${corte.subestaciones}")
        if (corte.url.isNotBlank()) appendLine("Fuente oficial: ${corte.url}")
        append("Creado desde MSealk")
    }
}
