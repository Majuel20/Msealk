package com.msealk.data

import org.json.JSONObject

data class HistoryItem(
    val timestampMillis: Long,
    val message: String,
    val type: String = "info"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("timestampMillis", timestampMillis)
        put("message", message)
        put("type", type)
    }

    companion object {
        fun fromJson(o: JSONObject): HistoryItem = HistoryItem(
            timestampMillis = o.optLong("timestampMillis", 0L),
            message = o.optString("message"),
            type = o.optString("type", "info")
        )
    }
}
