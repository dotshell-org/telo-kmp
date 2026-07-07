package eu.dotshell.massilia.specific.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiriVehicleMonitoringParserTest {

    // Verbatim (trimmed) empty delivery captured from the real hub on 2026-07-07.
    private val emptyDelivery = """
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><ns1:GetVehicleMonitoringResponse xmlns:ns1="http://wsdl.siri.org.uk"><ServiceDeliveryInfo xmlns:ns3="http://www.siri.org.uk/siri"><ns3:ResponseTimestamp>2026-07-07T21:41:15.062+02:00</ns3:ResponseTimestamp><ns3:ProducerRef>HUB-PAN-VM</ns3:ProducerRef><ns3:ResponseMessageIdentifier>HUB-PAN-VM:ResponseMessage::af432518:LOC</ns3:ResponseMessageIdentifier><ns3:RequestMessageRef>Massilia:vm:lr11</ns3:RequestMessageRef></ServiceDeliveryInfo><Answer xmlns:ns3="http://www.siri.org.uk/siri"><ns3:VehicleMonitoringDelivery version="2.0"><ns3:ResponseTimestamp>2026-07-07T21:41:15.062+02:00</ns3:ResponseTimestamp><ns3:RequestMessageRef>Massilia:vm:lr11</ns3:RequestMessageRef><ns3:Status>true</ns3:Status><ns3:DefaultLanguage>fr</ns3:DefaultLanguage></ns3:VehicleMonitoringDelivery></Answer><AnswerExtension/></ns1:GetVehicleMonitoringResponse></soap:Body></soap:Envelope>
    """.trimIndent()

    private fun activityXml(
        prefix: String,
        vehicleRef: String,
        lineRef: String,
        lat: String,
        lon: String,
        bearing: String? = null,
        destination: String? = null,
        directionRef: String? = null
    ): String {
        val p = if (prefix.isEmpty()) "" else "$prefix:"
        return buildString {
            append("<${p}VehicleActivity>")
            append("<${p}RecordedAtTime>2026-07-07T21:41:10.000+02:00</${p}RecordedAtTime>")
            append("<${p}ValidUntilTime>2026-07-07T21:43:10.000+02:00</${p}ValidUntilTime>")
            append("<${p}VehicleMonitoringRef>$vehicleRef</${p}VehicleMonitoringRef>")
            append("<${p}MonitoredVehicleJourney>")
            append("<${p}LineRef>$lineRef</${p}LineRef>")
            if (directionRef != null) append("<${p}DirectionRef>$directionRef</${p}DirectionRef>")
            append("<${p}FramedVehicleJourneyRef>")
            append("<${p}DataFrameRef>2026-07-07</${p}DataFrameRef>")
            append("<${p}DatedVehicleJourneyRef>RTM:VehicleJourney::J1:LOC</${p}DatedVehicleJourneyRef>")
            append("</${p}FramedVehicleJourneyRef>")
            if (destination != null) append("<${p}DestinationName>$destination</${p}DestinationName>")
            append("<${p}VehicleLocation>")
            append("<${p}Longitude>$lon</${p}Longitude>")
            append("<${p}Latitude>$lat</${p}Latitude>")
            append("</${p}VehicleLocation>")
            if (bearing != null) append("<${p}Bearing>$bearing</${p}Bearing>")
            append("</${p}MonitoredVehicleJourney>")
            append("</${p}VehicleActivity>")
        }
    }

    private fun deliveryWith(vararg activities: String): String {
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>" +
            "<ns1:GetVehicleMonitoringResponse xmlns:ns1=\"http://wsdl.siri.org.uk\">" +
            "<Answer xmlns:ns3=\"http://www.siri.org.uk/siri\">" +
            "<ns3:VehicleMonitoringDelivery version=\"2.0\">" +
            "<ns3:Status>true</ns3:Status>" +
            activities.joinToString("") +
            "</ns3:VehicleMonitoringDelivery>" +
            "</Answer></ns1:GetVehicleMonitoringResponse></soap:Body></soap:Envelope>"
    }

    @Test
    fun `empty delivery from the real hub parses to zero vehicles without error`() {
        val positions = SiriVehicleMonitoringParser.parse(emptyDelivery)
        assertTrue(positions.isEmpty())
        assertNull(SiriVehicleMonitoringParser.faultText(emptyDelivery))
    }

    @Test
    fun `delivery with two vehicles is fully extracted`() {
        val xml = deliveryWith(
            activityXml(
                "ns3", "RTM:Vehicle::7012:LOC", "RTM:Line::B1:LOC",
                lat = "43.30512", lon = "5.39174",
                bearing = "182.5", destination = "La Rose &amp; Frais Vallon",
                directionRef = "RTM:Direction::A:LOC"
            ),
            activityXml(
                "ns3", "RTM:Vehicle::7044:LOC", "RTM:Line::B1:LOC",
                lat = "43.28871", lon = "5.44290"
            )
        )

        val positions = SiriVehicleMonitoringParser.parse(xml)
        assertEquals(2, positions.size)

        val first = positions[0]
        assertEquals("RTM:Vehicle::7012:LOC", first.vehicleId)
        assertEquals("B1", first.lineName)
        assertEquals(43.30512, first.latitude, 1e-9)
        assertEquals(5.39174, first.longitude, 1e-9)
        assertEquals(182.5, first.bearing!!, 1e-9)
        assertEquals("La Rose & Frais Vallon", first.destinationName)
        assertEquals("RTM:Direction::A:LOC", first.direction)

        val second = positions[1]
        assertEquals("RTM:Vehicle::7044:LOC", second.vehicleId)
        assertNull(second.bearing)
        assertNull(second.destinationName)
    }

    @Test
    fun `namespace prefix does not matter`() {
        val noPrefix = deliveryWith(
            activityXml("", "V1", "RTM:Line::T2:LOC", lat = "43.3", lon = "5.4")
        ).replace("ns3:", "")
        val positions = SiriVehicleMonitoringParser.parse(noPrefix)
        assertEquals(1, positions.size)
        assertEquals("T2", positions[0].lineName)
    }

    @Test
    fun `activity without coordinates is skipped`() {
        val broken = deliveryWith(
            "<ns3:VehicleActivity><ns3:VehicleMonitoringRef>V9</ns3:VehicleMonitoringRef>" +
                "<ns3:MonitoredVehicleJourney><ns3:LineRef>RTM:Line::35:LOC</ns3:LineRef>" +
                "</ns3:MonitoredVehicleJourney></ns3:VehicleActivity>",
            activityXml("ns3", "V10", "RTM:Line::35:LOC", lat = "43.31", lon = "5.40")
        )
        val positions = SiriVehicleMonitoringParser.parse(broken)
        assertEquals(1, positions.size)
        assertEquals("V10", positions[0].vehicleId)
    }

    @Test
    fun `soap fault text is surfaced`() {
        val fault = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>" +
            "<soap:Fault><faultcode>soap:Server</faultcode>" +
            "<faultstring>Service [VM] is not enabled for service contract referenced by the participant code (RequestorRef) : PAN-SM</faultstring>" +
            "</soap:Fault></soap:Body></soap:Envelope>"
        val text = SiriVehicleMonitoringParser.faultText(fault)
        assertTrue(text!!.contains("Service [VM] is not enabled"))
    }

    @Test
    fun `delivery status false is surfaced as error text`() {
        val degraded = deliveryWith("").replace(
            "<ns3:Status>true</ns3:Status>",
            "<ns3:Status>false</ns3:Status><ns3:ErrorCondition><ns3:OtherError>" +
                "<ns3:ErrorText>Unable to find service contract for open-data</ns3:ErrorText>" +
                "</ns3:OtherError></ns3:ErrorCondition>"
        )
        assertEquals(
            "Unable to find service contract for open-data",
            SiriVehicleMonitoringParser.faultText(degraded)
        )
    }

    @Test
    fun `line name extraction handles hub ref conventions`() {
        assertEquals("B1", SiriVehicleMonitoringParser.extractLineNameFromRef("RTM:Line::B1:LOC"))
        assertEquals("T7", SiriVehicleMonitoringParser.extractLineNameFromRef("Interpolated:Line::ActIV:Line::T7:SYTRAL"))
        assertEquals("M1", SiriVehicleMonitoringParser.extractLineNameFromRef("M1"))
        assertEquals("35", SiriVehicleMonitoringParser.extractLineNameFromRef("RTM:Line::35:LOC"))
    }
}
