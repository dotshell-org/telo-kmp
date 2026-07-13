package eu.dotshell.telo.specific.data.network

import eu.dotshell.telo.generic.data.config.TransportConfigData
import eu.dotshell.telo.generic.data.models.realtime.vehiclepositions.SimpleVehiclePosition
import eu.dotshell.telo.generic.data.network.VehiclePositionsService
import eu.dotshell.telo.platform.Log
import eu.dotshell.telo.platform.createHttpClientEngine
import eu.dotshell.telo.specific.data.network.gtfsrt.GtfsRtVehicle
import eu.dotshell.telo.specific.data.network.gtfsrt.GtfsRtVehicleParser
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

private const val TAG = "MistralVehicles"

/**
 * Live vehicle positions from the official Réseau Mistral GTFS-RT feed
 * (transport.data.gouv.fr open data, Licence Ouverte v2.0) — a single
 * unauthenticated protobuf download carrying every vehicle of the network.
 *
 * The feed is fetched whole on every poll (it is small, ~2-10 KiB) and
 * decoded with [GtfsRtVehicleParser]; positions are only emitted when the
 * feed header timestamp actually changed. Line filtering happens client
 * side by mapping the feed's `trip.route_id` back to the commercial line
 * name through `config.realtimeLineIds` (short name → GTFS route_id).
 */
class MistralVehiclesService(
    private val config: TransportConfigData
) : VehiclePositionsService {

    override fun getVehiclePositionsStreamUrl(): String = config.vehiclePositionsStreamUrl

    private val httpClient = HttpClient(createHttpClientEngine())

    /** GTFS route_id → commercial name, plus a leading-zero-insensitive index. */
    private val nameByRouteId: Map<String, String> =
        config.realtimeLineIds.entries.associate { it.value to it.key }
    private val nameByStrippedRouteId: Map<String, String> =
        config.realtimeLineIds.entries.associate { it.value.trimStart('0').ifEmpty { "0" } to it.key }

    private val warnedRouteIds = mutableSetOf<String>()

    override fun streamAllVehiclePositions(): Flow<Result<List<SimpleVehiclePosition>>> =
        pollingFlow(lineFilter = null)

    override fun streamVehiclePositionsByLine(lineName: String): Flow<Result<List<SimpleVehiclePosition>>> =
        pollingFlow(lineFilter = lineName.trim().uppercase())

    override fun streamStrongLinesVehiclePositions(): Flow<Result<List<SimpleVehiclePosition>>> =
        streamAllVehiclePositions()

    private fun pollingFlow(lineFilter: String?): Flow<Result<List<SimpleVehiclePosition>>> = flow {
        var lastTimestamp: Long? = null

        while (currentCoroutineContext().isActive) {
            val outcome = runCatching {
                val response = httpClient.get(config.vehiclePositionsStreamUrl)
                if (response.status.value != 200) {
                    throw IllegalStateException("Mistral GTFS-RT HTTP ${response.status.value}")
                }
                val feed = GtfsRtVehicleParser.parse(response.readRawBytes())
                if (feed.headerTimestamp != null && feed.headerTimestamp == lastTimestamp) {
                    null // nothing moved upstream — skip the emission entirely
                } else {
                    lastTimestamp = feed.headerTimestamp
                    feed.vehicles
                        .mapNotNull { it.toSimplePosition() }
                        .let { all ->
                            if (lineFilter == null) all
                            else all.filter { it.lineName.uppercase() == lineFilter }
                        }
                }
            }
            outcome.fold(
                onSuccess = { positions -> if (positions != null) emit(Result.success(positions)) },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "GTFS-RT poll failed: ${error.message}")
                    emit(Result.failure(error))
                }
            )
            delay(POLL_INTERVAL_MS)
        }
    }.flowOn(Dispatchers.Default)

    private fun GtfsRtVehicle.toSimplePosition(): SimpleVehiclePosition? {
        val routeId = routeId ?: return null
        val lineName = nameByRouteId[routeId]
            ?: nameByStrippedRouteId[routeId.trimStart('0').ifEmpty { "0" }]
            ?: run {
                if (warnedRouteIds.add(routeId)) {
                    Log.w(TAG, "GTFS-RT vehicle on unknown route_id '$routeId' ignored")
                }
                return null
            }
        return SimpleVehiclePosition(
            vehicleId = vehicleId ?: vehicleLabel ?: entityId,
            lineName = lineName,
            latitude = latitude,
            longitude = longitude,
            bearing = bearing,
            destinationName = null,
            // GTFS direction_id is 0/1; the interpolator's baseline convention is "1"/"2".
            direction = directionId?.let { (it + 1).toString() }
        )
    }

    companion object {
        // The upstream feed regenerates every ~10 s (measured; constant ~28 s
        // pipeline latency, ~12 KiB payload) — poll at the same rate and let
        // the header-timestamp check absorb the misalignment.
        private const val POLL_INTERVAL_MS = 10_000L
    }
}
