package eu.dotshell.telo.specific.data.network.gtfsrt

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the hand-rolled wire-format parser two ways:
 * - a synthetic FeedMessage (bytes below, generated offline) covering a
 *   complete vehicle, an entity without position (must be dropped) and
 *   unknown fields at every level (must be skipped);
 * - a real payload captured from the Mistral GTFS-RT feed
 *   (fixtures/mistral_vehicle_positions.pb, 2026-07-13, Licence Ouverte v2.0).
 */
class GtfsRtVehicleParserTest {

    /*
     * FeedMessage {
     *   header { gtfs_realtime_version: "2.0", incrementality: 0, timestamp: 1783943655 }
     *   entity {
     *     id: "10"
     *     vehicle {
     *       trip { trip_id: "5950765", <unknown 7: 42>, route_id: "0018", direction_id: 1 }
     *       position { lat: 43.0912, lon: 5.8842, bearing: 137.0, <odometer: 12.5>, speed: 6.5 }
     *       timestamp: 1783943640
     *       vehicle { id: "SEIVAOB", label: "10" }
     *       <unknown 9: 2>
     *     }
     *   }
     *   entity { id: "ghost", vehicle { trip { route_id: "0070" } } }   // no position
     *   <unknown top-level 9: "future extension">
     * }
     */
    private val syntheticFeed = byteArrayOf(
        10, 13, 10, 3, 50, 46, 48, 16, 0, 24, -25, -93, -45, -46, 6, 18,
        77, 10, 2, 49, 48, 34, 71, 10, 19, 10, 7, 53, 57, 53, 48, 55,
        54, 53, 56, 42, 42, 4, 48, 48, 49, 56, 48, 1, 18, 25, 13, 100,
        93, 44, 66, 21, 94, 75, -68, 64, 29, 0, 0, 9, 67, 37, 0, 0,
        72, 65, 45, 0, 0, -48, 64, 40, -40, -93, -45, -46, 6, 66, 13, 10,
        7, 83, 69, 73, 86, 65, 79, 66, 18, 2, 49, 48, 72, 2, 18, 17,
        10, 5, 103, 104, 111, 115, 116, 34, 8, 10, 6, 42, 4, 48, 48, 55,
        48, 74, 18, 10, 16, 102, 117, 116, 117, 114, 101, 32, 101, 120, 116, 101,
        110, 115, 105, 111, 110
    )

    @Test
    fun parsesACompleteVehicleAndSkipsUnknownFields() {
        val feed = GtfsRtVehicleParser.parse(syntheticFeed)

        assertEquals(1783943655L, feed.headerTimestamp)
        assertEquals("only the vehicle with a position survives", 1, feed.vehicles.size)

        val vehicle = feed.vehicles.single()
        assertEquals("10", vehicle.entityId)
        assertEquals("5950765", vehicle.tripId)
        assertEquals("0018", vehicle.routeId)
        assertEquals(1, vehicle.directionId)
        assertEquals(43.0912, vehicle.latitude, 1e-4)
        assertEquals(5.8842, vehicle.longitude, 1e-4)
        assertEquals(137.0, vehicle.bearing!!, 1e-4)
        assertEquals(6.5, vehicle.speedMps!!, 1e-4)
        assertEquals(1783943640L, vehicle.timestamp)
        assertEquals("SEIVAOB", vehicle.vehicleId)
        assertEquals("10", vehicle.vehicleLabel)
    }

    @Test
    fun emptyFeedParsesToNothing() {
        val feed = GtfsRtVehicleParser.parse(ByteArray(0))
        assertNull(feed.headerTimestamp)
        assertTrue(feed.vehicles.isEmpty())
    }

    @Test
    fun truncatedFeedFailsInsteadOfReturningGarbage() {
        val truncated = syntheticFeed.copyOfRange(0, 60)
        val result = runCatching { GtfsRtVehicleParser.parse(truncated) }
        assertTrue("truncated payload must throw", result.isFailure)
    }

    @Test
    fun parsesARealCapturedMistralPayload() {
        val candidates = listOf(
            "src/androidUnitTest/fixtures/mistral_vehicle_positions.pb",
            "app/src/androidUnitTest/fixtures/mistral_vehicle_positions.pb"
        )
        val file = candidates.map(::File).firstOrNull(File::exists)
        assertNotNull("fixture not found from ${File(".").absolutePath}", file)

        val feed = GtfsRtVehicleParser.parse(file!!.readBytes())

        assertNotNull("real feed carries a header timestamp", feed.headerTimestamp)
        assertTrue("captured mid-day payload carries many vehicles (got ${feed.vehicles.size})",
            feed.vehicles.size > 50)
        feed.vehicles.forEach { vehicle ->
            assertTrue("${vehicle.entityId}: lat ${vehicle.latitude} outside Toulon area",
                vehicle.latitude in 42.8..43.4)
            assertTrue("${vehicle.entityId}: lon ${vehicle.longitude} outside Toulon area",
                vehicle.longitude in 5.5..6.5)
        }
        assertTrue("most vehicles carry a route_id",
            feed.vehicles.count { !it.routeId.isNullOrBlank() } > feed.vehicles.size / 2)
    }
}
