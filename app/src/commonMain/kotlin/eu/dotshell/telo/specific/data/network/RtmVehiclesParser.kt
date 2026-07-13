package eu.dotshell.telo.specific.data.network

import eu.dotshell.telo.generic.data.models.realtime.vehiclepositions.SimpleVehiclePosition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parser for the vehicle positions webservice backing RTM's own interactive
 * map (https://carte-interactive.rtm.fr/WS/siri/Vehicles?lines=RTM:LNE:139;…).
 *
 * The payload is a JSON array of vehicles — but the server sometimes
 * double-encodes it as a JSON *string* containing the array, so both shapes
 * are accepted.
 */
object RtmVehiclesParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Serializable
    private data class WsVehicle(
        @SerialName("Line") val line: String? = null,
        @SerialName("Direction") val direction: String? = null,
        @SerialName("Latitude") val latitude: Double? = null,
        @SerialName("Longitude") val longitude: Double? = null,
        @SerialName("Id") val id: String? = null
    )

    /**
     * @param lineNameById reverse mapping of the internal line id to the
     *   commercial name ("139" -> "B1"); vehicles of unknown lines are dropped.
     */
    fun parse(body: String, lineNameById: Map<String, String>): List<SimpleVehiclePosition> {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return emptyList()
        val payload = if (trimmed.startsWith("\"")) json.decodeFromString<String>(trimmed) else trimmed
        val vehicles = json.decodeFromString<List<WsVehicle>>(payload)
        return vehicles.mapNotNull { vehicle ->
            val latitude = vehicle.latitude ?: return@mapNotNull null
            val longitude = vehicle.longitude ?: return@mapNotNull null
            val vehicleId = vehicle.id ?: return@mapNotNull null
            val lineId = vehicle.line?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val lineName = lineNameById[lineId] ?: return@mapNotNull null
            SimpleVehiclePosition(
                vehicleId = vehicleId,
                lineName = lineName,
                latitude = latitude,
                longitude = longitude,
                bearing = null,
                destinationName = null,
                direction = vehicle.direction
            )
        }
    }
}
