package com.msealk.data

import org.json.JSONObject

data class AppSettings(
    val alertsEnabled: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val syncIntervalDays: Int = 1,
    val syncEveryMonday: Boolean = false,
    val syncHour: Int = 6,
    val syncMinute: Int = 30,
    val notifyNewCuts: Boolean = true,
    val notifyChangedCuts: Boolean = true,
    val notifyRemovedCuts: Boolean = false,
    val notifyOneDayBefore: Boolean = true,
    val notifySameDayMorning: Boolean = true,
    val notifyOneHourBefore: Boolean = true,
    val oneDayHour: Int = 6,
    val oneDayMinute: Int = 0,
    val sameDayHour: Int = 5,
    val sameDayMinute: Int = 30,
    val keepPastDays: Int = 2,
    val showPastGhosts: Boolean = true
) {
    fun normalized(): AppSettings = copy(
        syncIntervalDays = syncIntervalDays.coerceIn(1, 7),
        syncHour = syncHour.coerceIn(0, 23),
        syncMinute = syncMinute.coerceIn(0, 59),
        oneDayHour = oneDayHour.coerceIn(0, 23),
        oneDayMinute = oneDayMinute.coerceIn(0, 59),
        sameDayHour = sameDayHour.coerceIn(0, 23),
        sameDayMinute = sameDayMinute.coerceIn(0, 59),
        keepPastDays = keepPastDays.coerceIn(0, 30)
    )

    fun toJson(): JSONObject = JSONObject().apply {
        val s = normalized()
        put("alertsEnabled", s.alertsEnabled)
        put("autoSyncEnabled", s.autoSyncEnabled)
        put("syncIntervalDays", s.syncIntervalDays)
        put("syncEveryMonday", s.syncEveryMonday)
        put("syncHour", s.syncHour)
        put("syncMinute", s.syncMinute)
        put("notifyNewCuts", s.notifyNewCuts)
        put("notifyChangedCuts", s.notifyChangedCuts)
        put("notifyRemovedCuts", s.notifyRemovedCuts)
        put("notifyOneDayBefore", s.notifyOneDayBefore)
        put("notifySameDayMorning", s.notifySameDayMorning)
        put("notifyOneHourBefore", s.notifyOneHourBefore)
        put("oneDayHour", s.oneDayHour)
        put("oneDayMinute", s.oneDayMinute)
        put("sameDayHour", s.sameDayHour)
        put("sameDayMinute", s.sameDayMinute)
        put("keepPastDays", s.keepPastDays)
        put("showPastGhosts", s.showPastGhosts)
    }

    companion object {
        fun fromJson(raw: String?): AppSettings {
            if (raw.isNullOrBlank()) return AppSettings()
            return runCatching {
                val o = JSONObject(raw)
                AppSettings(
                    alertsEnabled = o.optBoolean("alertsEnabled", false),
                    autoSyncEnabled = o.optBoolean("autoSyncEnabled", true),
                    syncIntervalDays = o.optInt("syncIntervalDays", 1),
                    syncEveryMonday = o.optBoolean("syncEveryMonday", false),
                    syncHour = o.optInt("syncHour", 6),
                    syncMinute = o.optInt("syncMinute", 30),
                    notifyNewCuts = o.optBoolean("notifyNewCuts", true),
                    notifyChangedCuts = o.optBoolean("notifyChangedCuts", true),
                    notifyRemovedCuts = o.optBoolean("notifyRemovedCuts", false),
                    notifyOneDayBefore = o.optBoolean("notifyOneDayBefore", true),
                    notifySameDayMorning = o.optBoolean("notifySameDayMorning", true),
                    notifyOneHourBefore = o.optBoolean("notifyOneHourBefore", true),
                    oneDayHour = o.optInt("oneDayHour", 6),
                    oneDayMinute = o.optInt("oneDayMinute", 0),
                    sameDayHour = o.optInt("sameDayHour", 5),
                    sameDayMinute = o.optInt("sameDayMinute", 30),
                    keepPastDays = o.optInt("keepPastDays", 2),
                    showPastGhosts = o.optBoolean("showPastGhosts", true)
                ).normalized()
            }.getOrDefault(AppSettings())
        }
    }
}
