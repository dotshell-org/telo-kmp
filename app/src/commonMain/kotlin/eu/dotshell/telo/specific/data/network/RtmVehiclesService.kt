package eu.dotshell.telo.specific.data.network

import eu.dotshell.telo.generic.data.config.RulesData
import eu.dotshell.telo.generic.data.config.TransportConfigData
import eu.dotshell.telo.generic.data.models.realtime.vehiclepositions.SimpleVehiclePosition
import eu.dotshell.telo.generic.data.network.VehiclePositionsService
import eu.dotshell.telo.platform.Log
import eu.dotshell.telo.platform.createHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

private const val TAG = "RtmVehicles"

/**
 * Live vehicle positions from the public webservice backing RTM's own
 * interactive map (carte-interactive.rtm.fr). Unlike the SIRI hub of La
 * Métropole Mobilité — whose VehicleMonitoring cache is never fed — this
 * endpoint serves fresh positions and accepts many lines per request
 * (';'-separated internal ids).
 *
 * Polling mirrors the behaviour of RTM's own web client: a tiny
 * /Vehicles/LastUpdate probe on every tick, with the full /Vehicles fetch
 * only when the feed actually changed. The upstream feed itself only
 * refreshes once per minute (measured: ticks at hh:mm:42), so a 5 s probe
 * — the same rate as RTM's own site — just minimizes how late we catch
 * each minute-tick; it costs 13 bytes per probe.
 */
class RtmVehiclesService(
    private val config: TransportConfigData,
    private val rules: RulesData
) : VehiclePositionsService {

    override fun getVehiclePositionsStreamUrl(): String = config.vehiclePositionsStreamUrl

    private val httpClient = HttpClient(createHttpClientEngine())

    private val baseUrl = config.vehiclePositionsStreamUrl.trimEnd('/')

    private val idByName: Map<String, String> =
        config.realtimeLineIds.entries.associate { it.key.trim().uppercase() to it.value }
    private val nameById: Map<String, String> =
        config.realtimeLineIds.entries.associate { it.value to it.key }

    override fun streamAllVehiclePositions(): Flow<Result<List<SimpleVehiclePosition>>> =
        pollingFlow(lineIdsFor(rules.strongLines), POLL_INTERVAL_MS)

    override fun streamVehiclePositionsByLine(lineName: String): Flow<Result<List<SimpleVehiclePosition>>> =
        pollingFlow(lineIdsFor(listOf(lineName)), POLL_INTERVAL_MS)

    override fun streamStrongLinesVehiclePositions(): Flow<Result<List<SimpleVehiclePosition>>> =
        streamAllVehiclePositions()

    private fun lineIdsFor(lineNames: List<String>): List<String> =
        lineNames.mapNotNull { name ->
            idByName[name.trim().uppercase()].also {
                if (it == null) Log.w(TAG, "No realtime line id configured for '$name'")
            }
        }

    private fun pollingFlow(
        lineIds: List<String>,
        intervalMs: Long
    ): Flow<Result<List<SimpleVehiclePosition>>> = flow {
        if (lineIds.isEmpty()) {
            emit(Result.success(emptyList()))
            return@flow
        }
        val linesParam = lineIds.joinToString(";") { "RTM:LNE:$it" }
        var lastUpdateSeen: String? = null

        while (currentCoroutineContext().isActive) {
            val outcome = runCatching {
                // 13-byte change marker — skip the full fetch when nothing moved
                val lastUpdate = fetchText("$baseUrl/siri/Vehicles/LastUpdate").trim()
                if (lastUpdate == lastUpdateSeen) {
                    null
                } else {
                    val body = fetchText("$baseUrl/siri/Vehicles?lines=$linesParam")
                    val positions = RtmVehiclesParser.parse(body, nameById)
                    lastUpdateSeen = lastUpdate
                    positions
                }
            }
            outcome.fold(
                onSuccess = { positions -> if (positions != null) emit(Result.success(positions)) },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "Vehicles poll failed: ${error.message}")
                    emit(Result.failure(error))
                }
            )
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun fetchText(url: String): String {
        val response = httpClient.get(url)
        val body = response.bodyAsText()
        if (response.status.value != 200) {
            throw IllegalStateException("RTM vehicles HTTP ${response.status.value} on $url")
        }
        return body
    }

    companion object {
        private const val POLL_INTERVAL_MS = 5_000L
    }
}
