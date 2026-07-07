package eu.dotshell.massilia.specific.data.network

import eu.dotshell.massilia.generic.data.models.realtime.vehiclepositions.SimpleVehiclePosition

/**
 * Parser for SIRI 2.0 GetVehicleMonitoring SOAP responses (Diginext hub of
 * La Métropole Mobilité). The XML arrives with arbitrary namespace prefixes
 * (ns3:, siri:, …) that depend on the server's serializer, so all matching is
 * done on local element names only. No XML library is required.
 */
object SiriVehicleMonitoringParser {

    /**
     * Extracts vehicle positions from a GetVehicleMonitoring SOAP response.
     * Returns an empty list for a well-formed delivery with no activity.
     */
    fun parse(xml: String): List<SimpleVehiclePosition> {
        return VEHICLE_ACTIVITY.findAll(xml)
            .mapNotNull { parseActivity(it.groupValues[1]) }
            .toList()
    }

    /** Returns the SOAP fault or SIRI error text carried by a response, if any. */
    fun faultText(xml: String): String? {
        text(xml, "faultstring")?.let { return it }
        // Delivery-level failure: <Status>false</Status> + <ErrorText>…</ErrorText>
        if (text(xml, "Status") == "false") {
            return text(xml, "ErrorText") ?: "SIRI delivery status is false"
        }
        return null
    }

    private fun parseActivity(block: String): SimpleVehiclePosition? {
        val latitude = text(block, "Latitude")?.toDoubleOrNull() ?: return null
        val longitude = text(block, "Longitude")?.toDoubleOrNull() ?: return null
        val lineRef = text(block, "LineRef") ?: return null
        val lineName = extractLineNameFromRef(lineRef)
        if (lineName.isBlank()) return null
        val vehicleId = text(block, "VehicleMonitoringRef")
            ?: text(block, "VehicleRef")
            ?: text(block, "DatedVehicleJourneyRef")
            ?: return null

        return SimpleVehiclePosition(
            vehicleId = vehicleId,
            lineName = lineName,
            latitude = latitude,
            longitude = longitude,
            bearing = text(block, "Bearing")?.toDoubleOrNull(),
            destinationName = text(block, "DestinationName")?.let(::unescapeXml),
            direction = text(block, "DirectionRef") ?: text(block, "DirectionName")
        )
    }

    /**
     * The line code is the LAST "::" segment of the ref, up to the next ":".
     * "RTM:Line::B1:LOC" -> "B1" (same convention as the SYTRAL refs handled
     * by the former SSE service).
     */
    fun extractLineNameFromRef(lineRef: String): String {
        val lastSegment = lineRef.split("::").last()
        val colonIndex = lastSegment.indexOf(":")
        return (if (colonIndex > 0) lastSegment.take(colonIndex) else lastSegment).trim()
    }

    /** First occurrence of a leaf element's text, ignoring namespace prefixes. */
    private fun text(xml: String, localName: String): String? {
        val regex = FIELD_REGEXES.getOrElse(localName) { fieldRegex(localName) }
        return regex.find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun unescapeXml(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")

    private fun fieldRegex(localName: String) =
        Regex("<(?:[A-Za-z0-9_.-]+:)?$localName(?:\\s[^>]*)?>([^<]*)</(?:[A-Za-z0-9_.-]+:)?$localName>")

    private val VEHICLE_ACTIVITY = Regex(
        "<(?:[A-Za-z0-9_.-]+:)?VehicleActivity(?:\\s[^>]*)?>(.*?)</(?:[A-Za-z0-9_.-]+:)?VehicleActivity>",
        RegexOption.DOT_MATCHES_ALL
    )

    // Precompiled per field: parsing runs for every vehicle on every poll.
    private val FIELD_REGEXES: Map<String, Regex> = listOf(
        "Latitude", "Longitude", "LineRef", "VehicleMonitoringRef", "VehicleRef",
        "DatedVehicleJourneyRef", "Bearing", "DestinationName", "DirectionRef",
        "DirectionName", "faultstring", "Status", "ErrorText"
    ).associateWith { fieldRegex(it) }
}
