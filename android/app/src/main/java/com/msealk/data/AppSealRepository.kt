package com.msealk.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime

object AppSealRepository {
    const val DATA_URL: String =
        "https://raw.githubusercontent.com/Majuel20/MSealk/main/datos/cortes_programados.json"

    private const val LOCAL_JSON = "cortes_seal_latest.json"

    fun loadLocal(context: Context): List<Corte> {
        val file = context.filesDir.resolve(LOCAL_JSON)
        if (!file.exists()) return emptyList()
        return runCatching { parseJson(file.readText(Charsets.UTF_8)) }.getOrDefault(emptyList())
    }

    suspend fun updateFromRemote(context: Context): List<Corte> = withContext(Dispatchers.IO) {
        val json = downloadJson()
        val cortes = parseJson(json)
        val cache = android.util.AtomicFile(context.filesDir.resolve(LOCAL_JSON))
        val stream = cache.startWrite()
        try {
            stream.write(json.toByteArray(Charsets.UTF_8))
            cache.finishWrite(stream)
        } catch (error: Exception) {
            cache.failWrite(stream)
            throw error
        }
        AppSealPrefs.setLastSyncSuccess(context, json)
        cortes
    }

    fun saveLocal(context: Context, cortes: List<Corte>) {
        val arr = JSONArray()
        cortes.forEach { arr.put(it.toJson()) }
        context.filesDir.resolve(LOCAL_JSON).writeText(arr.toString(), Charsets.UTF_8)
    }

    fun pruneLocal(context: Context, keepPastDays: Int): Int {
        val all = loadLocal(context)
        val limit = LocalDate.now().minusDays(keepPastDays.coerceIn(0, 30).toLong())
        val kept = all.filter { corte ->
            val end = corte.finDateTime() ?: corte.inicioDateTime()
            end == null || !end.toLocalDate().isBefore(limit)
        }
        if (kept.size != all.size) saveLocal(context, kept)
        return all.size - kept.size
    }

    private fun downloadJson(): String {
        val conn = (URL(DATA_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MSealk-Android")
        }

        return try {
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code al descargar JSON")
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    fun parseJson(json: String): List<Corte> {
        val trimmed = json.trim()
        val arr = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> JSONObject(trimmed).optJSONArray("cortes") ?: error("La respuesta no contiene la lista de cortes")
        }

        val result = mutableListOf<Corte>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = optText(o, "id", "ID")
            val fecha = optText(o, "fecha", "Fecha")
            val horaInicio = optText(o, "hora_inicio", "horaInicio", "hora_ini", "inicio", "HoraInicio")
            val horaFin = optText(o, "hora_fin", "horaFin", "fin", "HoraFin")
            val distrito = optText(o, "distrito", "Distrito")
            val zonaAfectada = optText(o, "zona_afectada", "zona", "Zona", "zonaAfectada")
            val motivo = optText(o, "motivo", "Motivo")
            val ubicacion = optText(o, "ubicacion", "ubicación", "Ubicacion", "Ubicación")
            val titulo = optText(o, "titulo", "título", "Title", "title")
            val subestaciones = optText(o, "subestaciones", "sed", "SED", "subestacion", "subestación")
            val descripcion = optText(o, "descripcion", "descripción", "Descripcion", "Descripción", "body")
            val url = optText(o, "url", "URL", "link")
            val rawHash = optText(o, "hash")

            val corte = Corte(
                id = id,
                fecha = fecha,
                horaInicio = horaInicio,
                horaFin = horaFin,
                distrito = distrito,
                zonaAfectada = zonaAfectada,
                motivo = motivo,
                ubicacion = ubicacion,
                titulo = titulo,
                subestaciones = subestaciones,
                descripcion = descripcion,
                url = url,
                hash = rawHash.ifBlank {
                    sha256("$id|$fecha|$horaInicio|$horaFin|$distrito|$zonaAfectada|$descripcion")
                }
            )
            result.add(corte)
        }

        return result.sortedBy { it.inicioDateTime() ?: LocalDateTime.MAX }
    }

    private fun optText(o: JSONObject, vararg keys: String): String {
        for (key in keys) {
            if (!o.has(key) || o.isNull(key)) continue
            val value = o.opt(key) ?: continue
            val text = when (value) {
                is JSONArray -> buildList {
                    for (i in 0 until value.length()) add(value.optString(i))
                }.joinToString(", ")
                is JSONObject -> value.toString()
                else -> value.toString()
            }.trim()
            if (text.isNotBlank() && text != "null") return text
        }
        return ""
    }
}
