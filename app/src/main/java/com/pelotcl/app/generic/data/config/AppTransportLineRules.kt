package com.pelotcl.app.generic.data.config

import com.pelotcl.app.generic.data.models.ui.VehicleMarkerType
import com.pelotcl.app.generic.data.network.transport.TransportLineRules

class AppTransportLineRules(private val data: RulesData) : TransportLineRules {
    override fun normalizeAlertToken(raw: String): String {
        val token = raw.uppercase()
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")
            .replace("LIGNES", "")
            .replace("LIGNE", "")
            .replace("TRAM", "")
            .replace("METRO", "")
            .trim()

        return if (token.contains("RHONEXPRESS")) "RX" else token
    }

    override fun isLikelyLineToken(token: String): Boolean {
        if (token.isBlank()) return false
        if (isStrongLine(token)) return true
        return data.lineNameRegexes.any { regex ->
            token.matches(Regex(regex))
        }
    }

    override fun canonicalRouteName(raw: String): String {
        val token = raw.trim().uppercase()
        return when (token) {
            "NAVI1" -> "NAV1"
            else -> token
        }
    }

    override fun equivalentRouteNames(raw: String): List<String> {
        val token = raw.trim().uppercase()
        return when (token) {
            "NAVI1", "NAV1" -> listOf("NAVI1", "NAV1")
            else -> listOf(token)
        }
    }

    override fun normalizeForComparison(raw: String): String {
        val token = raw.trim().uppercase()
        return if (token.contains("RHONEXPRESS")) {
            "RX"
        } else {
            canonicalRouteName(token)
        }
    }

    override fun isStrongLine(lineName: String): Boolean {
        val upperName = lineName.uppercase()
        if (data.strongLines.any { upperName == it.uppercase() }) return true
        return data.strongLineRegexes.any { regex ->
            upperName.matches(Regex(regex))
        }
    }

    override fun getTransportType(lineName: String): String {
        val upperName = lineName.uppercase()
        for (type in data.transportTypes) {
            if (upperName.matches(Regex(type.regex))) {
                return type.name
            }
        }
        return "Bus" // Default
    }

    override fun getModeIcon(lineName: String): String? {
        if (isStrongLine(lineName)) return null
        val upperName = lineName.uppercase()
        for (type in data.transportTypes) {
            if (upperName.matches(Regex(type.regex))) {
                return type.icon
            }
        }
        return "mode_bus"
    }

    override fun isNavigoneLine(lineName: String): Boolean {
        return getTransportType(lineName) == "Navigone"
    }

    override fun isLiveTrackableLine(lineName: String): Boolean {
        val upperName = lineName.uppercase()
        if (isStrongLine(lineName)) {
            // Tram lines are trackable despite being strong
            return upperName.startsWith("T")
        }
        return true
    }

    override fun getVehicleMarkerType(lineName: String): VehicleMarkerType {
        val upperName = lineName.uppercase()
        return when {
            upperName.startsWith("TB") -> VehicleMarkerType.BUS
            upperName.startsWith("T") -> VehicleMarkerType.TRAM
            else -> VehicleMarkerType.BUS
        }
    }

    override fun normalizeLineNameForUi(lineName: String): String {
        return if (canonicalRouteName(lineName) == "NAV1") "NAVI1" else lineName
    }

    override fun sortLines(lines: List<String>): List<String> {
        data class Key(
            val family: Int,
            val subFamily: String = "",
            val number: Int = Int.MAX_VALUE,
            val raw: String = ""
        )

        fun keyFor(lineRaw: String): Key {
            val up = lineRaw.trim().uppercase()

            when (up) {
                "A" -> return Key(1000, number = 0, raw = up)
                "B" -> return Key(1001, number = 0, raw = up)
                "C" -> return Key(1002, number = 0, raw = up)
                "D" -> return Key(1003, number = 0, raw = up)
            }

            if (up.startsWith("F")) {
                val num = up.drop(1).toIntOrNull()
                if (num != null) return Key(2000, number = num, raw = up)
            }

            if (up.startsWith("T")) {
                val num = up.drop(1).toIntOrNull()
                if (num != null) return Key(3000, number = num, raw = up)
            }

            val regex = Regex("^([A-Z]+)(\\d+)([A-Z]*)$")
            val match = regex.matchEntire(up)
            if (match != null) {
                val prefix = match.groupValues[1]
                val num = match.groupValues[2].toIntOrNull() ?: Int.MAX_VALUE
                return Key(4000, subFamily = prefix, number = num, raw = up)
            }

            val pureNum = up.toIntOrNull()
            if (pureNum != null) {
                return Key(5000, number = pureNum, raw = up)
            }

            return Key(9000, subFamily = up, number = Int.MAX_VALUE, raw = up)
        }

        return lines
            .filter { !it.equals("T36", ignoreCase = true) }
            .sortedWith(Comparator { a, b ->
                val ka = keyFor(a)
                val kb = keyFor(b)
                when {
                    ka.family != kb.family -> ka.family - kb.family
                    ka.subFamily != kb.subFamily -> ka.subFamily.compareTo(kb.subFamily)
                    ka.number != kb.number -> ka.number - kb.number
                    else -> ka.raw.compareTo(kb.raw)
                }
            })
    }
}
