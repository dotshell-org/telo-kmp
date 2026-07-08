package eu.dotshell.massilia

import eu.dotshell.massilia.generic.data.config.AppConfig
import eu.dotshell.massilia.generic.utils.graphics.LineIconResolver
import eu.dotshell.massilia.specific.data.local.RtmLinesParser
import io.raptor.PeriodData
import io.raptor.RaptorLibrary
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Headless end-to-end checks of the bundled RTM data and configuration:
 * the raptor network loads, a real journey (Vieux-Port → Castellane) is
 * computable, every line has its pictogram (or is a known fallback), and
 * config.json deserializes into the AppConfig schema with the RTM values.
 */
class RtmSmokeTest {

    private fun asset(path: String): File {
        val candidates = listOf(
            "src/commonMain/composeResources/files/$path",
            "app/src/commonMain/composeResources/files/$path"
        )
        return candidates.map(::File).firstOrNull(File::exists)
            ?: error("asset $path not found from ${File(".").absolutePath}")
    }

    // ─── Routing ─────────────────────────────────────────────────────────────

    @Test
    fun raptorComputesVieuxPortToCastellane() {
        val library = RaptorLibrary(
            listOf(
                PeriodData(
                    periodId = "school_on_weekdays",
                    stopsBytes = asset("raptor/stops_school_on_weekdays.bin").readBytes(),
                    routesBytes = asset("raptor/routes_school_on_weekdays.bin").readBytes()
                )
            )
        )

        val allStops = library.searchStopsByName("")
        assertEquals("weekday period carries all 2752 stops", 2752, allStops.size)

        val origins = allStops.filter {
            it.name.equals("Vieux Port", true) || it.name.equals("Vieux-Port", true)
        }
        val destinations = allStops.filter { it.name.equals("Castellane", true) }
        assertTrue("Vieux-Port stops found", origins.isNotEmpty())
        assertTrue("Castellane stops found", destinations.isNotEmpty())

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
            "one journey uses the M1 metro",
            journeys.any { journey -> journey.any { leg -> !leg.isTransfer && leg.routeName == "M1" } }
        )
    }

    @Test
    fun blockedRouteNamesExcludeSchoolLines() {
        val library = RaptorLibrary(
            listOf(
                PeriodData(
                    periodId = "school_on_weekdays",
                    stopsBytes = asset("raptor/stops_school_on_weekdays.bin").readBytes(),
                    routesBytes = asset("raptor/routes_school_on_weekdays.bin").readBytes()
                )
            )
        )
        val allStops = library.searchStopsByName("")
        val origins = allStops.filter { it.name.equals("Vieux Port", true) || it.name.equals("Vieux-Port", true) }
        val destinations = allStops.filter { it.name.equals("Castellane", true) }

        val blocked = (1..8).map { "S$it" }.toSet() + setOf("N1", "N2")
        val journeys = library.getOptimizedPaths(
            originStopIds = origins.map { it.id },
            destinationStopIds = destinations.map { it.id },
            departureTime = 9 * 3600,
            blockedRouteNames = blocked
        )
        assertTrue("journeys still exist with S/N lines blocked", journeys.isNotEmpty())
        assertTrue(
            "no journey uses a blocked line",
            journeys.none { journey -> journey.any { !it.isTransfer && it.routeName in blocked } }
        )
    }

    // ─── Icons ───────────────────────────────────────────────────────────────

    @Test
    fun everyLineHasItsPictogramOrKnownFallback() {
        val knownFallbacks = setOf("97JET", "BM1A", "BM1B", "BM2")
        val drawableDirs = listOf(
            "src/commonMain/composeResources/drawable",
            "app/src/commonMain/composeResources/drawable"
        )
        val drawableDir = drawableDirs.map(::File).firstOrNull(File::isDirectory)
            ?: error("drawable dir not found")

        val lines = RtmLinesParser.parse(asset("raptor/lines.bin").readBytes())
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
    fun configJsonMatchesTheRtmSchema() {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        val config = json.decodeFromString<AppConfig>(asset("config.json").readText())

        assertEquals("RTM", config.transport.networkName)
        assertEquals(4, config.transport.regionBounds.size)
        assertEquals(125, config.lineColors.rules.size)
        assertTrue("M1 is a strong line", "M1" in config.rules.strongLines)
        assertFalse(config.realtime.trafficAlertsEnabled)
        // Live vehicle positions come from the webservice of RTM's own interactive map
        assertTrue(config.realtime.vehiclePositionsEnabled)
        assertTrue(
            "the stream URL must point at the RTM interactive map webservice",
            config.transport.vehiclePositionsStreamUrl.startsWith("https://carte-interactive.rtm.fr/")
        )
        assertEquals(125, config.transport.realtimeLineIds.size)
        assertEquals("116", config.transport.realtimeLineIds["M1"])
        assertEquals("139", config.transport.realtimeLineIds["B1"])
        // Measured speed baseline for first-tick dead reckoning
        assertTrue(config.transport.vehicleSpeedBaseline.size > 50)
        val t1Baseline = config.transport.vehicleSpeedBaseline["T1"]!!
        assertTrue("T1 commercial speed sane", t1Baseline.speedMps in 0.5..15.0)
        assertTrue("T1 has measured direction signs", t1Baseline.signs.isNotEmpty())
        // Every strong line must be pollable in global live mode
        config.rules.strongLines.forEach { line ->
            assertTrue("strong line $line needs a realtime id", line in config.transport.realtimeLineIds)
        }
        assertFalse(config.realtime.userStopAlertsEnabled)
        assertEquals(false, config.telemetry?.enabled)
        assertEquals("marseille-rtm", config.telemetry?.networkCode)

        // Every regex in the rules must compile (AppConfigValidator would crash at startup)
        (config.rules.strongLineRegexes + config.rules.lineNameRegexes +
            config.rules.transportTypes.map { it.regex }).forEach { Regex(it) }
    }
}
