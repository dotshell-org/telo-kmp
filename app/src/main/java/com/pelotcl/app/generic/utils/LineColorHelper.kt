package com.pelotcl.app.generic.utils

import androidx.core.graphics.toColorInt
import com.pelotcl.app.generic.data.config.AppConfigLoader
import com.pelotcl.app.generic.data.models.geojson.Feature

/**
 * Helper to determine line colors from declarative config.
 */
object LineColorHelper {

    // Cache for toColorInt() results — small set of unique colors
    private val colorIntCache = HashMap<String, Int>(20)

    private fun resolveColorHex(lineName: String): String {
        val upper = lineName.trim().uppercase()
        val rules = AppConfigLoader.getConfig().lineColors

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
        val color = resolveColorHex(lineName).toColorInt()
        colorIntCache[key] = color
        return color
    }
}
