package eu.dotshell.pelo.generic.utils.graphics

import eu.dotshell.pelo.generic.data.models.geojson.StopFeature

/**
 * Multiplatform-safe line icon name resolution.
 * Maps line names to drawable file names (e.g., "212" -> "_212", "A" -> "a").
 * Also parses "desserte" strings from stop features.
 */
object LineIconResolver {

    // Simple bounded cache for parsed desserte strings
    private val desserteCache = HashMap<String, List<String>>()
    private const val CACHE_MAX_SIZE = 500

    /**
     * Converts a line name to a drawable name.
     * Lines composed only of digits are prefixed with an underscore.
     *
     * @param lineName The line name (ex: "212", "C17", "A", "NAVI1")
     * @return The corresponding drawable name (ex: "_212", "c17", "a", "navi1")
     */
    fun getDrawableNameForLineName(lineName: String): String {
        if (lineName.isBlank()) return ""
        return if (lineName.all { it.isDigit() }) {
            "_$lineName"
        } else {
            lineName.lowercase()
        }
    }

    /**
     * Returns all lines serving a stop (line names parsed from desserte).
     * Results are cached by desserte string to avoid repeated parsing.
     */
    fun getAllLinesForStop(stopFeature: StopFeature): List<String> {
        val desserte = stopFeature.properties.desserte
        desserteCache[desserte]?.let { return it }
        val result = parseDesserte(desserte)
        if (desserteCache.size >= CACHE_MAX_SIZE) {
            desserteCache.clear()
        }
        desserteCache[desserte] = result
        return result
    }

    fun clearCache() {
        desserteCache.clear()
    }

    /**
     * Parses the desserte string to extract the list of lines.
     * Handled cases:
     *  - "5:A,86:A,JD844:R" -> ["5", "86", "JD844"] (buses with directions)
     *  - "A:A,D:A" -> ["A", "D"] (metros A and D, :A = outbound direction)
     *  - "F1:A,F2:A" -> ["F1", "F2"] (funiculars)
     *  - "M:A:B" -> ["M", "B"] (bus M with multiple destinations, ignore :A/:R)
     *  - "C17:22:31" (old format) -> ["C17", "22", "31"]
     *
     * IMPORTANT: Don't confuse ":A" (outbound direction) with metro line A
     */
    internal fun parseDesserte(desserte: String): List<String> {
        if (desserte.isBlank()) return emptyList()

        val entries = desserte.split(",")
        val rawLines: List<String> = if (entries.size > 1) {
            entries.mapNotNull { part ->
                val trimmed = part.trim()
                if (trimmed.isEmpty()) null else {
                    trimmed.substringBefore(":").trim()
                }
            }.filter { it.isNotEmpty() }
        } else {
            val tokens = desserte.split(":")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return emptyList()

            if (tokens.size == 2) {
                val first = tokens[0]
                val second = tokens[1]
                if (second.uppercase() == "A" || second.uppercase() == "R") {
                    listOf(first)
                } else {
                    tokens
                }
            } else if (tokens.size > 2) {
                val first = tokens.first()
                val rest = tokens.drop(1).filter { t ->
                    val up = t.uppercase()
                    up != "A" && up != "R"
                }
                listOf(first) + rest
            } else {
                tokens
            }
        }

        val seen = HashSet<String>()
        val unique = ArrayList<String>(rawLines.size)
        rawLines.forEach { line ->
            val key = line.uppercase()
            if (seen.add(key)) {
                unique.add(line)
            }
        }
        return unique
    }
}
