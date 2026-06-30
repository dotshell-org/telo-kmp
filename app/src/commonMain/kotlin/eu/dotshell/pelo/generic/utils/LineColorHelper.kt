package eu.dotshell.pelo.generic.utils

import eu.dotshell.pelo.generic.data.models.geojson.Feature
import eu.dotshell.pelo.platform.provideLineColors
import kotlin.concurrent.Volatile

object LineColorHelper {

    // Copy-on-write cache (see FrenchPublicHolidayStrategy): lock-free reads, no CME if accessed
    // off the main thread. A racing double-compute just recomputes the same color once.
    @Volatile
    private var colorIntCache: Map<String, Int> = emptyMap()

    private fun resolveColorHex(lineName: String): String {
        val upper = lineName.trim().uppercase()
        val rules = provideLineColors()

        for (rule in rules.rules) {
            val match = rule.match.trim().uppercase()
            when (rule.type.lowercase()) {
                "exact" -> if (upper == match) return rule.color
                "prefix" -> if (upper.startsWith(match)) return rule.color
                "regex" -> if (upper.matches(Regex(rule.match))) return rule.color
            }
        }

        return rules.fallback
    }

    fun getColorForLine(feature: Feature): String {
        return resolveColorHex(feature.properties.lineName)
    }

    fun getColorForLineStringAux(lineName: String): String {
        return resolveColorHex(lineName)
    }

    fun getColorForLineString(lineName: String): Int {
        val key = lineName.trim().uppercase()
        colorIntCache[key]?.let { return it }
        val color = hexToArgb(resolveColorHex(lineName))
        colorIntCache = colorIntCache + (key to color)
        return color
    }

    private fun hexToArgb(hex: String): Int {
        val color = hex.removePrefix("#")
        return when (color.length) {
            6 -> (0xFF shl 24) or
                (color.substring(0, 2).toInt(16) shl 16) or
                (color.substring(2, 4).toInt(16) shl 8) or
                color.substring(4, 6).toInt(16)
            8 -> (color.substring(0, 2).toInt(16) shl 24) or
                (color.substring(2, 4).toInt(16) shl 16) or
                (color.substring(4, 6).toInt(16) shl 8) or
                color.substring(6, 8).toInt(16)
            else -> throw IllegalArgumentException("Invalid hex color: $hex")
        }
    }
}
