package eu.dotshell.massilia.specific.data.network

import eu.dotshell.massilia.generic.data.config.RulesData
import eu.dotshell.massilia.generic.data.config.TransportConfigData
import eu.dotshell.massilia.generic.data.models.realtime.vehiclepositions.SimpleVehiclePosition
import eu.dotshell.massilia.generic.data.network.VehiclePositionsService
import eu.dotshell.massilia.platform.Log
import eu.dotshell.massilia.platform.createHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock

private const val TAG = "RtmSiriVM"

/**
 * Vehicle positions backed by the SIRI 2.0 SOAP endpoint of La Métropole
 * Mobilité (https://siri.lametropolemobilite.fr/RTM, RequestorRef PAN-VM).
 *
 * Unlike the former SSE mirror, this server only answers ONE line per
 * GetVehicleMonitoring request and the whole endpoint is rate-limited to
 * 10-20 requests/min, so the streams are implemented by polling:
 * - per-line live: the requested line every [SINGLE_LINE_POLL_MS];
 * - global live: a round-robin sweep over the strong lines, [GLOBAL_BATCH_SIZE]
 *   lines every [GLOBAL_BATCH_INTERVAL_MS], merged into one vehicle map with
 *   staleness eviction.
 */
class RtmSiriVehiclePositionsService(
    private val config: TransportConfigData,
    private val rules: RulesData
) : VehiclePositionsService {

    override fun getVehiclePositionsStreamUrl(): String = config.vehiclePositionsStreamUrl

    private val httpClient = HttpClient(createHttpClientEngine())

    override fun streamAllVehiclePositions(): Flow<Result<List<SimpleVehiclePosition>>> = flow {
        val sweepLines = rules.strongLines
        val positionsByVehicle = LinkedHashMap<String, SimpleVehiclePosition>()
        val lastSeenByVehicle = HashMap<String, Long>()
        var cursor = 0

        while (currentCoroutineContext().isActive && sweepLines.isNotEmpty()) {
            var batchError: Throwable? = null
            var batchSucceeded = false

            repeat(GLOBAL_BATCH_SIZE) { indexInBatch ->
                val line = sweepLines[cursor % sweepLines.size]
                cursor++
                runCatching { fetchLinePositions(line) }
                    .onSuccess { fetched ->
                        batchSucceeded = true
                        val now = Clock.System.now().toEpochMilliseconds()
                        // Fresh snapshot for this line: drop its previous vehicles first.
                        val iterator = positionsByVehicle.entries.iterator()
                        while (iterator.hasNext()) {
                            val entry = iterator.next()
                            if (entry.value.lineName.equals(line, ignoreCase = true)) {
                                iterator.remove()
                                lastSeenByVehicle.remove(entry.key)
                            }
                        }
                        fetched.forEach { position ->
                            positionsByVehicle[position.vehicleId] = position
                            lastSeenByVehicle[position.vehicleId] = now
                        }
                    }
                    .onFailure { error ->
                        batchError = error
                        Log.w(TAG, "VM poll failed for $line: ${error.message}")
                    }
                if (indexInBatch < GLOBAL_BATCH_SIZE - 1) delay(INTRA_BATCH_DELAY_MS)
            }

            // Evict vehicles from lines whose polls kept failing.
            val cutoff = Clock.System.now().toEpochMilliseconds() - STALE_VEHICLE_MS
            val staleIds = lastSeenByVehicle.filterValues { it < cutoff }.keys.toList()
            staleIds.forEach { id ->
                positionsByVehicle.remove(id)
                lastSeenByVehicle.remove(id)
            }

            if (batchSucceeded) {
                emit(Result.success(positionsByVehicle.values.toList()))
            } else {
                batchError?.let { emit(Result.failure(it)) }
            }

            delay(GLOBAL_BATCH_INTERVAL_MS)
        }
    }.flowOn(Dispatchers.Default)

    override fun streamVehiclePositionsByLine(lineName: String): Flow<Result<List<SimpleVehiclePosition>>> = flow {
        while (currentCoroutineContext().isActive) {
            runCatching { fetchLinePositions(lineName) }
                .onSuccess { emit(Result.success(it)) }
                .onFailure { error ->
                    Log.w(TAG, "VM poll failed for $lineName: ${error.message}")
                    emit(Result.failure(error))
                }
            delay(SINGLE_LINE_POLL_MS)
        }
    }.flowOn(Dispatchers.Default)

    override fun streamStrongLinesVehiclePositions(): Flow<Result<List<SimpleVehiclePosition>>> =
        streamAllVehiclePositions()

    private suspend fun fetchLinePositions(lineName: String): List<SimpleVehiclePosition> {
        val response = httpClient.post(config.vehiclePositionsStreamUrl) {
            contentType(ContentType.Text.Xml)
            header("SOAPAction", "GetVehicleMonitoring")
            setBody(buildVehicleMonitoringRequest(lineName))
        }
        val body = response.bodyAsText()
        if (response.status.value != 200) {
            val detail = SiriVehicleMonitoringParser.faultText(body) ?: body.take(200)
            throw IllegalStateException("SIRI VM HTTP ${response.status.value}: $detail")
        }
        SiriVehicleMonitoringParser.faultText(body)?.let { throw IllegalStateException("SIRI VM error: $it") }
        return SiriVehicleMonitoringParser.parse(body)
    }

    private fun buildVehicleMonitoringRequest(lineName: String): String {
        val timestamp = Clock.System.now().toString()
        val messageId = "Massilia:Message::${Clock.System.now().toEpochMilliseconds()}:LOC"
        val lineRef = "RTM:Line::${escapeXml(lineName.trim().uppercase())}:LOC"
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Body>" +
            "<sw:GetVehicleMonitoring xmlns:sw=\"http://wsdl.siri.org.uk\" xmlns:siri=\"http://www.siri.org.uk/siri\">" +
            "<ServiceRequestInfo>" +
            "<siri:RequestTimestamp>$timestamp</siri:RequestTimestamp>" +
            "<siri:RequestorRef>$REQUESTOR_REF</siri:RequestorRef>" +
            "<siri:MessageIdentifier>$messageId</siri:MessageIdentifier>" +
            "</ServiceRequestInfo>" +
            "<Request>" +
            "<siri:RequestTimestamp>$timestamp</siri:RequestTimestamp>" +
            "<siri:MessageIdentifier>$messageId</siri:MessageIdentifier>" +
            "<siri:LineRef>$lineRef</siri:LineRef>" +
            "</Request>" +
            "<RequestExtension/>" +
            "</sw:GetVehicleMonitoring>" +
            "</S:Body></S:Envelope>"
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    companion object {
        // Participant code with the VM service enabled on the RTM endpoint
        // (the documented "open-data" ref has no service contract there).
        private const val REQUESTOR_REF = "PAN-VM"

        // Whole-endpoint rate limit is 10-20 req/min; 3 lines every 15 s
        // keeps the global sweep at ~12 req/min with headroom for bursts.
        private const val GLOBAL_BATCH_SIZE = 3
        private const val GLOBAL_BATCH_INTERVAL_MS = 15_000L
        private const val INTRA_BATCH_DELAY_MS = 500L
        private const val SINGLE_LINE_POLL_MS = 10_000L
        private const val STALE_VEHICLE_MS = 180_000L
    }
}
