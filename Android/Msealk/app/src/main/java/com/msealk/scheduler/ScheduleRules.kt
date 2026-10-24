package com.msealk.scheduler

import com.msealk.data.AppSettings
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Reglas de calendario compartidas por la interfaz y los trabajos. */
object ScheduleRules {
    fun syncKey(s: AppSettings): String = "${s.autoSyncEnabled}|${s.syncEveryMonday}|${s.syncIntervalDays}|${s.syncHour}|${s.syncMinute}"

    fun nextSync(settings: AppSettings, now: LocalDateTime, lastSuccessMillis: Long = 0): LocalDateTime {
        val s = settings.normalized()
        val time = LocalTime.of(s.syncHour, s.syncMinute)
        if (s.syncEveryMonday) {
            val monday = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).atTime(time)
            return if (monday.isAfter(now)) monday else monday.plusWeeks(1)
        }
        val last = lastSuccessMillis.takeIf { it > 0 }?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        var target = (last?.plusDays(s.syncIntervalDays.toLong()) ?: now.toLocalDate()).atTime(time)
        while (!target.isAfter(now)) target = target.plusDays(s.syncIntervalDays.toLong())
        return target
    }

    fun sameDayReminder(start: LocalDateTime, hour: Int, minute: Int): LocalDateTime {
        val preferred = start.toLocalDate().atTime(hour, minute)
        return if (preferred.isBefore(start)) preferred else start.minusMinutes(90)
    }

    fun canCatchUp(type: String, alertAt: LocalDateTime, start: LocalDateTime, now: LocalDateTime): Boolean {
        if (now.isBefore(alertAt) || now.isAfter(start.minusMinutes(15))) return false
        return when (type) {
            "one_day" -> now.toLocalDate() == alertAt.toLocalDate()
            "today" -> now.toLocalDate() == alertAt.toLocalDate() || now.toLocalDate() == start.toLocalDate()
            else -> false
        }
    }

    fun deadline(type: String, alertAt: LocalDateTime, start: LocalDateTime): LocalDateTime =
        if (type == "one_day") minOf(start, alertAt.toLocalDate().plusDays(1).atStartOfDay()) else start

    private val formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy, HH:mm", Locale.forLanguageTag("es-PE"))
    fun formatDateTime(date: LocalDateTime): String = date.format(formatter)
    fun formatInstant(millis: Long): String = formatDateTime(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime())
}
