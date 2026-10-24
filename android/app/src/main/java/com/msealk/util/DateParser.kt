package com.msealk.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object DateParser {
    private val esPe: Locale = Locale("es", "PE")

    private val datePatterns = listOf(
        "dd/MM/yyyy",
        "d/M/yyyy",
        "yyyy-MM-dd",
        "dd-MM-yyyy",
        "d-M-yyyy"
    )

    fun parseDateTime(fecha: String, hora: String): LocalDateTime? {
        val date = parseDate(fecha) ?: return null
        val time = parseTime(hora) ?: return null
        return LocalDateTime.of(date, time)
    }

    fun parseDate(fecha: String): LocalDate? {
        val clean = fecha.trim()
        if (clean.isBlank()) return null

        for (pattern in datePatterns) {
            try {
                return LocalDate.parse(clean, DateTimeFormatter.ofPattern(pattern, esPe))
            } catch (_: DateTimeParseException) {
                // Prueba siguiente formato.
            }
        }
        return null
    }

    fun parseTime(hora: String): LocalTime? {
        if (hora.isBlank()) return null

        val clean = hora
            .lowercase(Locale.ROOT)
            .replace("a. m.", "am")
            .replace("p. m.", "pm")
            .replace("a.m.", "am")
            .replace("p.m.", "pm")
            .replace("a. m", "am")
            .replace("p. m", "pm")
            .replace("a m", "am")
            .replace("p m", "pm")
            .replace("a.m", "am")
            .replace("p.m", "pm")
            .replace("hrs", "")
            .replace("horas", "")
            .trim()

        val match = Regex("""(\d{1,2})(?::(\d{1,2}))?(?::\d{1,2})?\s*(am|pm)?""")
            .find(clean) ?: return null

        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: 0
        val ampm = match.groupValues[3]

        if (ampm == "pm" && hour < 12) hour += 12
        if (ampm == "am" && hour == 12) hour = 0

        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }

    fun formatDate(date: LocalDate): String = date.format(
        DateTimeFormatter.ofPattern("EEE d MMM yyyy", esPe)
    ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(esPe) else it.toString() }

    fun formatDateTime(dateTime: LocalDateTime): String = dateTime.format(
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", esPe)
    )

    fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}
