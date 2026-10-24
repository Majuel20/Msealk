package com.msealk.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppSealPrefs {
    private const val PREFS = "app_seal_prefs"

    private const val KEY_SETTINGS_JSON = "settings_json_v2"
    private const val KEY_ZONES_JSON = "zones_json_v2"
    private const val KEY_KNOWN_MATCHES_JSON = "known_matches_json_v2"
    private const val KEY_SENT_NOTIFICATIONS_JSON = "sent_notifications_json_v2"
    private const val KEY_HISTORY_JSON = "history_json_v2"
    private const val KEY_LAST_SYNC_MILLIS = "last_sync_millis_v2"
    private const val KEY_LAST_SYNC_ERROR = "last_sync_error_v2"
    private const val KEY_LAST_JSON_HASH = "last_json_hash_v2"

    // Preferencia antigua de tu primera versión. Sirve para migrar el texto simple a una zona nueva.
    private const val LEGACY_KEY_ZONA = "zona"

    fun getSettings(context: Context): AppSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AppSettings.fromJson(p.getString(KEY_SETTINGS_JSON, null))
    }

    fun saveSettings(context: Context, settings: AppSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SETTINGS_JSON, settings.normalized().toJson().toString())
            .apply()
    }

    fun getZones(context: Context): List<UserZone> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = p.getString(KEY_ZONES_JSON, null)

        if (raw.isNullOrBlank()) {
            val legacy = p.getString(LEGACY_KEY_ZONA, "")?.trim().orEmpty()
            if (legacy.isNotBlank()) {
                val migrated = listOf(
                    UserZone(
                        name = "Mi zona",
                        enabled = true,
                        matchMode = MatchMode.ANY,
                        keywords = splitKeywords(legacy)
                    )
                )
                saveZones(context, migrated)
                return migrated
            }
            return emptyList()
        }

        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(UserZone.fromJson(obj))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveZones(context: Context, zones: List<UserZone>) {
        val arr = JSONArray()
        zones.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ZONES_JSON, arr.toString())
            .apply()
    }

    fun setLastSyncSuccess(context: Context, rawJson: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC_MILLIS, System.currentTimeMillis())
            .putString(KEY_LAST_SYNC_ERROR, "")
            .putString(KEY_LAST_JSON_HASH, sha256(rawJson))
            .apply()
    }

    fun setLastSyncError(context: Context, error: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SYNC_ERROR, error.take(300))
            .apply()
    }

    fun lastSyncMillis(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getLong(KEY_LAST_SYNC_MILLIS, 0L)

    fun lastSyncText(context: Context): String {
        val millis = lastSyncMillis(context)
        if (millis <= 0L) return "Aún no actualizado"
        return formatMillis(millis)
    }

    fun lastSyncError(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_LAST_SYNC_ERROR, "")
        .orEmpty()

    fun lastJsonHash(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_LAST_JSON_HASH, "")
        .orEmpty()

    fun getKnownMatches(context: Context): MutableMap<String, KnownMatchState> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_KNOWN_MATCHES_JSON, "{}")
            .orEmpty()

        return runCatching {
            val obj = JSONObject(raw.ifBlank { "{}" })
            val result = mutableMapOf<String, KnownMatchState>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = obj.optJSONObject(key) ?: continue
                result[key] = KnownMatchState.fromJson(value)
            }
            result
        }.getOrDefault(mutableMapOf())
    }

    fun saveKnownMatches(context: Context, map: Map<String, KnownMatchState>) {
        val obj = JSONObject()
        map.forEach { (key, value) -> obj.put(key, value.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_KNOWN_MATCHES_JSON, obj.toString())
            .apply()
    }

    fun markNotificationSentIfNew(context: Context, notificationKey: String): Boolean = synchronized(this) {
        if (notificationKey.isBlank()) return@synchronized false

        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val obj = JSONObject(p.getString(KEY_SENT_NOTIFICATIONS_JSON, "{}") ?: "{}")
        if (obj.has(notificationKey)) return@synchronized false

        obj.put(notificationKey, System.currentTimeMillis())

        // Mantiene el estado liviano. Si crece demasiado, elimina claves antiguas de forma simple.
        if (obj.length() > 1200) {
            val keys = obj.keys().asSequence().toList()
            keys.take((obj.length() - 900).coerceAtLeast(0)).forEach { obj.remove(it) }
        }

        p.edit().putString(KEY_SENT_NOTIFICATIONS_JSON, obj.toString()).apply()
        true
    }

    fun sentNotificationsCount(context: Context): Int {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SENT_NOTIFICATIONS_JSON, "{}")
            .orEmpty()
        return runCatching { JSONObject(raw.ifBlank { "{}" }).length() }.getOrDefault(0)
    }

    fun addHistory(context: Context, message: String, type: String = "info") = synchronized(this) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = runCatching { JSONArray(p.getString(KEY_HISTORY_JSON, "[]") ?: "[]") }
            .getOrDefault(JSONArray())

        val newArray = JSONArray()
        newArray.put(HistoryItem(System.currentTimeMillis(), message, type).toJson())

        val limit = 100
        var count = 1
        for (i in 0 until old.length()) {
            if (count >= limit) break
            val obj = old.optJSONObject(i) ?: continue
            newArray.put(obj)
            count++
        }

        p.edit().putString(KEY_HISTORY_JSON, newArray.toString()).apply()
    }

    fun getHistory(context: Context): List<HistoryItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY_JSON, "[]")
            .orEmpty()

        return runCatching {
            val arr = JSONArray(raw.ifBlank { "[]" })
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(HistoryItem.fromJson(obj))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun clearHistory(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY_JSON, "[]")
            .apply()
    }


    fun splitKeywords(raw: String): List<String> = raw
        .split(",", ";", "\n")
        .map { it.trim() }
        .filter { it.length >= 2 }
        .distinct()

    fun formatMillis(millis: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date(millis))
    }
}
