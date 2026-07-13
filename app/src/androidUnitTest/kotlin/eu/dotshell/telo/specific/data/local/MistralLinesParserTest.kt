package eu.dotshell.telo.specific.data.local

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses the real bundled lines.bin (RLN2) and checks the Réseau Mistral
 * network invariants: line count, known lines with their GTFS colors and
 * route types (bus, bateau-bus, cable car), and that every shape point falls
 * inside the Toulon bounding box.
 */
class MistralLinesParserTest {

    private fun linesBin(): ByteArray {
        // Unit tests run with the module directory as working dir; fall back to
        // the repo root in case the runner uses it instead.
        val candidates = listOf(
            "src/commonMain/composeResources/files/raptor/lines.bin",
            "app/src/commonMain/composeResources/files/raptor/lines.bin"
        )
        val file = candidates.map(::File).firstOrNull(File::exists)
        assertNotNull("lines.bin not found from ${File(".").absolutePath}", file)
        return file!!.readBytes()
    }

    @Test
    fun parsesTheFullMistralNetwork() {
        val lines = RtmLinesParser.parse(linesBin())
        assertEquals("Mistral GTFS has 51 routes", 51, lines.size)

        val byName = lines.associateBy { it.name }

        val u = byName["U"]
        assertNotNull("U missing", u)
        assertEquals("FC9D44", u!!.colorHex.uppercase())
        assertEquals("U is a bus (GTFS route_type 3)", 3, u.gtfsRouteType)
        assertTrue("U has at least one shape", u.paths.isNotEmpty())

        val cableCar = byName["T"]
        assertNotNull("T (Mont Faron cable car) missing", cableCar)
        assertEquals("DB0000", cableCar!!.colorHex.uppercase())
        assertEquals("T is an aerial lift (GTFS route_type 6)", 6, cableCar.gtfsRouteType)

        for ((boat, color) in listOf("8M" to "9AAAD7", "18M" to "3A75C4", "28M" to "003893")) {
            val line = byName[boat]
            assertNotNull("$boat missing", line)
            assertEquals("$boat is a ferry (GTFS route_type 4)", 4, line!!.gtfsRouteType)
            assertEquals(color, line.colorHex.uppercase())
        }

        for (navette in listOf("BN1", "BN3")) {
            val line = byName[navette]
            assertNotNull("$navette missing", line)
            assertEquals("$navette is a bus (GTFS route_type 3)", 3, line!!.gtfsRouteType)
        }
    }

    @Test
    fun everyShapePointIsInsideTheToulonBoundingBox() {
        val lines = RtmLinesParser.parse(linesBin())
        var points = 0
        for (line in lines) {
            assertTrue("${line.name} has no shape", line.paths.isNotEmpty())
            for (path in line.paths) {
                assertTrue("${line.name} path with <2 points", path.points.size >= 2)
                for (point in path.points) {
                    val lon = point[0]
                    val lat = point[1]
                    assertTrue("${line.name}: lat $lat out of bounds", lat in 42.95..43.25)
                    assertTrue("${line.name}: lon $lon out of bounds", lon in 5.7..6.3)
                    points++
                }
            }
        }
        // The bundled Mistral lines.bin carries a dense shape set (~478 KiB).
        assertTrue("expected a dense shape set, got $points points", points > 30_000)
    }
}
