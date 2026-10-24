package com.msealk.data

import org.json.JSONObject

data class KnownMatchState(
    val contentHash: String,
    val label: String,
    val zoneName: String,
    val firstSeenMillis: Long,
    val lastSeenMillis: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("contentHash", contentHash)
        put("label", label)
        put("zoneName", zoneName)
        put("firstSeenMillis", firstSeenMillis)
        put("lastSeenMillis", lastSeenMillis)
    }

    companion object {
        fun fromJson(o: JSONObject): KnownMatchState = KnownMatchState(
            contentHash = o.optString("contentHash"),
            label = o.optString("label"),
            zoneName = o.optString("zoneName"),
            firstSeenMillis = o.optLong("firstSeenMillis", System.currentTimeMillis()),
            lastSeenMillis = o.optLong("lastSeenMillis", System.currentTimeMillis())
        )
    }
}
