package com.msealk.util

import com.msealk.data.Corte

/** Solo transforma la presentación; el aviso original se conserva en el modelo. */
data class CutDetails(val circuit: String, val description: String, val zones: String, val substations: String)

object CutDetailFormatter {
    private fun key(text: String) = TextMatch.normalize(text).trim().trimEnd('.', ':', ';')

    fun distinctEntries(text: String): String {
        val heading = Regex("^(urbanizaciones[^:]*|zonas afectadas[^:]*|subestaciones[^:]*):\\s*", RegexOption.IGNORE_CASE)
            .find(text.trim())?.value.orEmpty()
        val entries = text.trim().removePrefix(heading).split(Regex("[,;\\n]+"))
            .map { it.trim().trimEnd('.') }.filter { it.isNotBlank() }
            .distinctBy { key(it) }.joinToString(", ")
        return heading + entries
    }

    private fun repeatsDateOrTime(line: String, c: Corte): Boolean {
        val date = DateParser.parseDate(c.fecha)
        val dateText = date?.format(java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", java.util.Locale.forLanguageTag("es-PE")))
        if (dateText != null && Regex("^(lunes|martes|miercoles|jueves|viernes|sabado|domingo),?\\s+")
                .replace(line, "") == key(dateText)) return true
        val time = Regex("^hora\\s*:\\s*(\\d{1,2}:\\d{2})\\s*(?:a|-)\\s*(\\d{1,2}:\\d{2})(?:\\s*hrs?\\.?)?$").matchEntire(line)
            ?: return false
        return DateParser.parseTime(time.groupValues[1]) == DateParser.parseTime(c.horaInicio) &&
            DateParser.parseTime(time.groupValues[2]) == DateParser.parseTime(c.horaFin)
    }

    fun format(c: Corte): CutDetails {
        val circuit = c.ubicacion.replace(Regex("^\\s*circuito\\s*:\\s*", RegexOption.IGNORE_CASE), "").trim()
        val zones = distinctEntries(c.zonaAfectada)
        val substations = distinctEntries(c.subestaciones)
        val represented = listOf(c.titulo, c.motivo, c.ubicacion, c.zonaAfectada, c.subestaciones, c.distrito)
            .filter { it.isNotBlank() }.map(::key)
        val extra = c.descripcion.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.filterNot { line ->
            val normalized = key(line)
            val afterLabel = key(line.substringAfter(':', line))
            normalized in represented || afterLabel in represented || repeatsDateOrTime(normalized, c)
        }
        val description = (sequenceOf(c.titulo, c.motivo) + extra).map { it.trim() }
            .filter { it.isNotBlank() }.distinctBy(::key).joinToString("\n")
        return CutDetails(circuit, description, zones, substations)
    }
}
