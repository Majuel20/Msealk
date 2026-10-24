package com.msealk.util

import com.msealk.data.Corte
import com.msealk.data.MatchMode
import com.msealk.data.UserZone
import java.text.Normalizer
import java.util.Locale

data class CorteZoneMatch(
    val corte: Corte,
    val zone: UserZone,
    val matchedKeywords: List<String>
) {
    val matchKey: String get() = "${zone.id}|${corte.stableKey}"
}

object TextMatch {
    fun normalize(text: String): String {
        val normalized = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return normalized
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    fun findMatches(cortes: List<Corte>, zones: List<UserZone>): List<CorteZoneMatch> {
        val activeZones = zones.filter { it.enabled && it.cleanKeywords().isNotEmpty() }
        if (activeZones.isEmpty()) return emptyList()

        val result = mutableListOf<CorteZoneMatch>()
        for (corte in cortes) {
            for (zone in activeZones) {
                val matched = matchedKeywords(corte, zone)
                val ok = when (zone.matchMode) {
                    MatchMode.ANY -> matched.isNotEmpty()
                    MatchMode.ALL -> matched.size == zone.cleanKeywords().size
                }
                if (ok) result.add(CorteZoneMatch(corte, zone, matched))
            }
        }

        return result.sortedBy { it.corte.inicioDateTime() ?: java.time.LocalDateTime.MAX }
    }

    fun matchedKeywords(corte: Corte, zone: UserZone): List<String> {
        val text = normalize(corte.textoBusqueda())
        return zone.cleanKeywords().filter { keyword ->
            val clean = normalize(keyword)
            clean.length >= 2 && text.contains(clean)
        }
    }
}
