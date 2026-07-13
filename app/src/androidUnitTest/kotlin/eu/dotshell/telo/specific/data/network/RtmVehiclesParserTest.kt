package eu.dotshell.telo.specific.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RtmVehiclesParserTest {

    private val lineNameById = mapOf("139" to "B1", "2" to "T1", "116" to "M1")

    // Verbatim shape captured from the real webservice on 2026-07-08:
    // a JSON *string* whose content is the JSON array (double-encoded).
    private val doubleEncoded =
        "\"[{\\\"Line\\\":\\\"RTM:LNE:139\\\",\\\"Direction\\\":\\\"2\\\",\\\"Latitude\\\":43.285354," +
            "\\\"Longitude\\\":5.384171,\\\"ValidUntilTime\\\":\\\"2026-07-08T17:11:28.989+02:00\\\"," +
            "\\\"Id\\\":\\\"RTM:VEH:05002121\\\"},{\\\"Line\\\":\\\"RTM:LNE:2\\\",\\\"Direction\\\":\\\"1\\\"," +
            "\\\"Latitude\\\":43.295802,\\\"Longitude\\\":5.401311," +
            "\\\"ValidUntilTime\\\":\\\"2026-07-08T17:11:28.989+02:00\\\",\\\"Id\\\":\\\"RTM:VEH:05000026\\\"}]\""

    @Test
    fun `double-encoded payload from the real webservice is parsed`() {
        val positions = RtmVehiclesParser.parse(doubleEncoded, lineNameById)
        assertEquals(2, positions.size)

        val bus = positions[0]
        assertEquals("RTM:VEH:05002121", bus.vehicleId)
        assertEquals("B1", bus.lineName)
        assertEquals(43.285354, bus.latitude, 1e-9)
        assertEquals(5.384171, bus.longitude, 1e-9)
        assertEquals("2", bus.direction)
        assertNull(bus.bearing)

        assertEquals("T1", positions[1].lineName)
    }

    @Test
    fun `plain array payload is parsed too`() {
        val plain = "[{\"Line\":\"RTM:LNE:116\",\"Direction\":\"1\",\"Latitude\":43.30,\"Longitude\":5.40,\"Id\":\"V1\"}]"
        val positions = RtmVehiclesParser.parse(plain, lineNameById)
        assertEquals(1, positions.size)
        assertEquals("M1", positions[0].lineName)
    }

    @Test
    fun `empty payloads yield no vehicles`() {
        assertTrue(RtmVehiclesParser.parse("[]", lineNameById).isEmpty())
        assertTrue(RtmVehiclesParser.parse("\"[]\"", lineNameById).isEmpty())
        assertTrue(RtmVehiclesParser.parse("", lineNameById).isEmpty())
    }

    @Test
    fun `vehicles of unknown lines or without coordinates are dropped`() {
        val payload = "[" +
            "{\"Line\":\"RTM:LNE:9999\",\"Latitude\":43.3,\"Longitude\":5.4,\"Id\":\"V1\"}," +
            "{\"Line\":\"RTM:LNE:139\",\"Id\":\"V2\"}," +
            "{\"Line\":\"RTM:LNE:139\",\"Latitude\":43.31,\"Longitude\":5.41,\"Id\":\"V3\"}" +
            "]"
        val positions = RtmVehiclesParser.parse(payload, lineNameById)
        assertEquals(1, positions.size)
        assertEquals("V3", positions[0].vehicleId)
    }
}
