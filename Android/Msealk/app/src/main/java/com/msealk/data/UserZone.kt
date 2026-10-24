package com.msealk.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class MatchMode {
    ANY,
    ALL;

    fun label(): String = when (this) {
        ANY -> "contiene cualquiera"
        ALL -> "contiene todas"
    }
}

data class UserZone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val matchMode: MatchMode = MatchMode.ANY,
    val keywords: List<String> = emptyList(),
    val colorKey: String = "cyan"
) {
    fun cleanKeywords(): List<String> = keywords
        .map { it.trim() }
        .filter { it.length >= 2 }
        .distinct()

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("enabled", enabled)
        put("matchMode", matchMode.name)
        put("keywords", JSONArray(cleanKeywords()))
        put("colorKey", colorKey.ifBlank { "cyan" })
    }

    companion object {
        fun fromJson(o: JSONObject): UserZone {
            val arr = o.optJSONArray("keywords") ?: JSONArray()
            val words = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val value = arr.optString(i).trim()
                if (value.isNotBlank()) words.add(value)
            }

            return UserZone(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                name = o.optString("name").ifBlank { "Mi zona" },
                enabled = o.optBoolean("enabled", true),
                matchMode = runCatching { MatchMode.valueOf(o.optString("matchMode", MatchMode.ANY.name)) }
                    .getOrDefault(MatchMode.ANY),
                keywords = words,
                colorKey = o.optString("colorKey", "cyan").ifBlank { "cyan" }
            )
        }
    }
}
