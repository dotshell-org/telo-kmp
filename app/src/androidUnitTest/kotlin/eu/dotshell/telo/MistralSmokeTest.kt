package eu.dotshell.telo

import eu.dotshell.telo.generic.data.config.AppConfig
import eu.dotshell.telo.generic.utils.graphics.LineIconResolver
import eu.dotshell.telo.specific.data.local.MistralLinesParser
import io.raptor.PeriodData
import io.raptor.RaptorLibrary
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Headless end-to-end checks of the bundled Réseau Mistral data and
 * configuration: the raptor network loads, a real journey
 * (Liberté → Hôpital Ste Musse) is computable, every line has its badge,
 * and config.json deserializes into the AppConfig schema.
 */
class MistralSmokeTest {

    private fun asset(path: String): File {
        val candidates = listOf(
            "src/commonMain/composeResources/files/$path",
            "app/src/commonMain/composeResources/files/$path"
        )
        return candidates.map(::File).firstOrNull(File::exists)
            ?: error("asset $path not found from ${File(".").absolutePath}")
    }

    private fun weekdayLibrary() = RaptorLibrary(
        listOf(
            PeriodData(
                periodId = "school_on_weekdays",
                stopsBytes = asset("raptor/stops_school_on_weekdays.bin").readBytes(),
                routesBytes = asset("raptor/routes_school_on_weekdays.bin").readBytes()
            )
        )
    )

    // ─── Routing ─────────────────────────────────────────────────────────────

    @Test
    fun raptorComputesLiberteToHopitalSteMusse() {
        val library = weekdayLibrary()

        val allStops = library.searchStopsByName("")
        assertTrue("weekday period carries the full network (got ${allStops.size} stops)", allStops.size > 1500)

        val origins = allStops.filter { it.name.equals("Liberté", true) }
        val destinations = allStops.filter { it.name.startsWith("Hôpital Ste Musse", true) }
        assertTrue("Liberté stops found", origins.isNotEmpty())
        assertTrue("Hôpital Ste Musse stops found", destinations.isNotEmpty())

        val journeys = library.getOptimizedPaths(
            originStopIds = origins.map { it.id },
            destinationStopIds = destinations.map { it.id },
            departureTime = 9 * 3600
        )
        assertTrue("at least one journey found", journeys.isNotEmpty())

        val best = journeys.first()
        val duration = best.last().arrivalTime - best.first().departureTime
        assertTrue("journey shorter than 45 min (got ${duration / 60} min)", duration < 45 * 60)
        assertTrue(
            "one journey rides bus 1 or 9 (the direct lines)",
            journeys.any { journey -> journey.any { leg -> !leg.isTransfer && leg.routeName in setOf("1", "9") } }
        )
    }

    @Test
    fun blockedRouteNamesExcludeTheDirectLines() {
        val library = weekdayLibrary()
        val allStops = library.searchStopsByName("")
        val origins = allStops.filter { it.name.equals("Liberté", true) }
        val destinations = allStops.filter { it.name.startsWith("Hôpital Ste Musse", true) }

        val blocked = setOf("1", "9")
        val journeys = library.getOptimizedPaths(
            originStopIds = origins.map { it.id },
            destinationStopIds = destinations.map { it.id },
            departureTime = 9 * 3600,
            blockedRouteNames = blocked
        )
        assertTrue("journeys still exist with lines 1 and 9 blocked", journeys.isNotEmpty())
        assertTrue(
            "no journey uses a blocked line",
            journeys.none { journey -> journey.any { !it.isTransfer && it.routeName in blocked } }
        )
    }

    // ─── Icons ───────────────────────────────────────────────────────────────

    @Test
    fun everyLineHasItsPictogramOrKnownFallback() {
        val knownFallbacks = emptySet<String>()
        val drawableDirs = listOf(
            "src/commonMain/composeResources/drawable",
            "app/src/commonMain/composeResources/drawable"
        )
        val drawableDir = drawableDirs.map(::File).firstOrNull(File::isDirectory)
            ?: error("drawable dir not found")

        val lines = MistralLinesParser.parse(asset("raptor/lines.bin").readBytes())
        val missing = lines
            .map { it.name }
            .filter { it !in knownFallbacks }
            .filter { name ->
                !File(drawableDir, LineIconResolver.getDrawableNameForLineName(name) + ".xml").exists()
            }
        assertTrue("lines without pictogram: $missing", missing.isEmpty())
    }

    // ─── Configuration ───────────────────────────────────────────────────────

    @Test
    fun configJsonMatchesTheMistralSchema() {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        val config = json.decodeFromString<AppConfig>(asset("config.json").readText())

        assertEquals("Réseau Mistral", config.transport.networkName)
        assertEquals(4, config.transport.regionBounds.size)
        assertEquals(51, config.lineColors.rules.size)
        assertTrue("U is a strong line", "U" in config.rules.strongLines)
        assertTrue("T (cable car) is a strong line", "T" in config.rules.strongLines)
        assertFalse(config.realtime.trafficAlertsEnabled)
        // Live vehicle positions stay disabled until the GTFS-RT service lands
        assertFalse(config.realtime.vehiclePositionsEnabled)
        assertTrue(
            "the stream URL must point at the official Mistral GTFS-RT feed",
            config.transport.vehiclePositionsStreamUrl.startsWith("https://feed-rdtpm-toulon.ratpdev.com/")
        )
        assertEquals(51, config.transport.realtimeLineIds.size)
        assertEquals("0001", config.transport.realtimeLineIds["1"])
        assertEquals("011B", config.transport.realtimeLineIds["11B"])
        assertEquals("U", config.transport.realtimeLineIds["U"])
        // No measured speed baseline: the interpolator degrades cleanly on an empty map
        assertTrue(config.transport.vehicleSpeedBaseline.isEmpty())
        // Every strong line must be pollable in global live mode
        config.rules.strongLines.forEach { line ->
            assertTrue("strong line $line needs a realtime id", line in config.transport.realtimeLineIds)
        }
        assertFalse(config.realtime.userStopAlertsEnabled)
        assertEquals(false, config.telemetry?.enabled)
        assertEquals("toulon-mistral", config.telemetry?.networkCode)

        // Every regex in the rules must compile (AppConfigValidator would crash at startup)
        (config.rules.strongLineRegexes + config.rules.lineNameRegexes +
            config.rules.transportTypes.map { it.regex }).forEach { Regex(it) }
    }
}
