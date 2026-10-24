package com.msealk.data

import com.msealk.util.DateParser
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDateTime

/**
 * Modelo principal de un corte programado de SEAL.
 * La app NO hace scraping: solo consume el JSON publicado en GitHub Actions.
 */
data class Corte(
    val id: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val distrito: String,
    val zonaAfectada: String,
    val motivo: String,
    val ubicacion: String,
    val titulo: String,
    val subestaciones: String,
    val descripcion: String,
    val url: String,
    val hash: String
) {
    val stableKey: String
        get() = hash.ifBlank { sha256("$id|$fecha|$horaInicio|$horaFin|$distrito|$zonaAfectada") }

    fun inicioDateTime(): LocalDateTime? = DateParser.parseDateTime(fecha, horaInicio)

    fun finDateTime(): LocalDateTime? = DateParser.parseDateTime(fecha, horaFin)

    fun fechaBonita(): String = DateParser.parseDate(fecha)?.let { DateParser.formatDate(it) } ?: fecha

    fun rangoHora(): String = listOf(horaInicio, horaFin)
        .filter { it.isNotBlank() }
        .joinToString(" - ")

    fun textoBusqueda(): String = listOf(
        distrito,
        zonaAfectada,
        motivo,
        ubicacion,
        titulo,
        subestaciones,
        descripcion
    ).joinToString(" ")

    fun contentHash(): String = sha256(
        listOf(
            id,
            fecha,
            horaInicio,
            horaFin,
            distrito,
            zonaAfectada,
            motivo,
            ubicacion,
            titulo,
            subestaciones,
            descripcion,
            url
        ).joinToString("|")
    )

    fun shortLabel(): String {
        val zona = zonaAfectada.ifBlank { ubicacion }.ifBlank { distrito }.ifBlank { "zona registrada" }
        val hora = listOf(horaInicio, horaFin).filter { it.isNotBlank() }.joinToString(" - ")
        return listOf(fecha, hora, zona).filter { it.isNotBlank() }.joinToString(" · ")
    }

    fun toNotificationBody(): String {
        return listOf(
            fechaBonita(),
            rangoHora(),
            distrito,
            zonaAfectada.ifBlank { ubicacion }
        ).filter { it.isNotBlank() }.joinToString(" · ")
    }

    fun toFriendlyMessage(zoneName: String? = null): String {
        return buildString {
            appendLine("⚡ Corte programado SEAL")
            if (!zoneName.isNullOrBlank()) appendLine("Zona guardada: $zoneName")
            if (fecha.isNotBlank()) appendLine("Día/fecha: ${fechaBonita()}")
            if (rangoHora().isNotBlank()) appendLine("Hora: ${rangoHora()}")
            if (distrito.isNotBlank()) appendLine("Distrito: $distrito")
            if (zonaAfectada.isNotBlank()) appendLine("Zona afectada: $zonaAfectada")
            if (ubicacion.isNotBlank()) appendLine("Ubicación: $ubicacion")
            if (motivo.isNotBlank()) appendLine("Motivo: $motivo")
            if (subestaciones.isNotBlank()) appendLine("SED/Subestación: $subestaciones")
            if (url.isNotBlank()) appendLine("Fuente: $url")
            append("Enviado desde MSealk")
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("fecha", fecha)
        put("hora_inicio", horaInicio)
        put("hora_fin", horaFin)
        put("distrito", distrito)
        put("zona_afectada", zonaAfectada)
        put("motivo", motivo)
        put("ubicacion", ubicacion)
        put("titulo", titulo)
        put("subestaciones", subestaciones)
        put("descripcion", descripcion)
        put("url", url)
        put("hash", hash)
    }
}

fun sha256(text: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}
