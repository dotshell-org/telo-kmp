package com.pelotcl.app.generic.data.config

import com.google.gson.JsonObject
import com.pelotcl.app.generic.data.GsonProvider
import com.pelotcl.app.generic.data.models.realtime.vehiclepositions.SiriData
import com.pelotcl.app.generic.data.models.realtime.vehiclepositions.SimpleVehiclePosition
import com.pelotcl.app.generic.data.models.realtime.vehiclepositions.VehicleActivity
import com.pelotcl.app.generic.data.models.realtime.vehiclepositions.VehiclePositionsResponse
import com.pelotcl.app.generic.data.network.VehiclePositionsService
import com.pelotcl.app.generic.utils.network.DotshellRequestLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

class AppVehiclePositionsService(
    private val config: TransportConfigData,
    private val rules: RulesData
) : VehiclePositionsService {

    override fun getVehiclePositionsStreamUrl(): String = config.vehiclePositionsStreamUrl

    private val gson = GsonProvider.instance
    private val lineRefPattern: Regex? = runCatching {
        Regex(config.vehiclePositionsLineRefPattern)
    }.getOrNull()

    companion object {
        private val streamClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor(DotshellRequestLogger.interceptor("sse"))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }
    
    override fun streamAllVehiclePositions(): Flow<Result<List<SimpleVehiclePosition>>> {
        return createVehiclePositionsFlow()
    }
    
    override fun streamVehiclePositionsByLine(lineName: String): Flow<Result<List<SimpleVehiclePosition>>> {
        return streamAllVehiclePositions().map { result ->
            result.map { positions ->
                positions.filter { position ->
                    position.lineName.equals(lineName, ignoreCase = true)
                }
            }
        }
    }
    
    override fun streamStrongLinesVehiclePositions(): Flow<Result<List<SimpleVehiclePosition>>> {
        val strongLines = rules.strongLines.toSet()
        
        return streamAllVehiclePositions().map { result ->
            result.map { positions ->
                positions.filter { position ->
                    strongLines.contains(position.lineName)
                }
            }
        }
    }
    
    private fun createVehiclePositionsFlow(): Flow<Result<List<SimpleVehiclePosition>>> {
        return callbackFlow {
            val request = Request.Builder()
                .url(getVehiclePositionsStreamUrl())
                .header("Accept", "text/event-stream")
                .build()

            val listener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    android.util.Log.i("DotshellRequest", "[SSE] Connection established")
                }

                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    if (type == "heartbeat") return
                    runCatching { parsePositionsEventData(data) }
                        .onSuccess { positions ->
                            if (positions.isNotEmpty()) {
                                trySend(Result.success(positions))
                            }
                        }
                        .onFailure { trySend(Result.failure(it)) }
                }

                override fun onClosed(eventSource: EventSource) {
                    close()
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    close(t ?: Exception("Vehicle positions SSE connection closed"))
                }
            }

            val eventSource = EventSources.createFactory(streamClient).newEventSource(request, listener)
            awaitClose { eventSource.cancel() }
        }.retryWhen { cause, attempt ->
            if (attempt >= 10) return@retryWhen false
            val backoff = (1_000L * (1L shl attempt.coerceAtMost(5).toInt())).coerceAtMost(30_000L)
            delay(backoff)
            true
        }
    }
    
    private fun parsePositionsEventData(data: String): List<SimpleVehiclePosition> {
        val eventJson = gson.fromJson(data, JsonObject::class.java)
        val payloadElement = eventJson.get("payload") ?: eventJson

        runCatching {
            gson.fromJson(payloadElement, VehiclePositionsResponse::class.java)
        }.getOrNull()?.let { parsed ->
            if (parsed.success) {
                val fromResponse = extractPositionsFromSiriData(parsed.data)
                if (fromResponse.isNotEmpty()) return fromResponse
            }
        }

        runCatching {
            gson.fromJson(payloadElement, SiriData::class.java)
        }.getOrNull()?.let { siriData ->
            val fromSiri = extractPositionsFromSiriData(siriData)
            if (fromSiri.isNotEmpty()) return fromSiri
        }

        return emptyList()
    }
    
    private fun extractPositionsFromSiriData(data: SiriData?): List<SimpleVehiclePosition> {
        val activities = data?.siri?.serviceDelivery?.vehicleMonitoringDelivery
            ?.flatMap { it.vehicleActivity ?: emptyList() }
            ?: emptyList()
        return activities.mapNotNull { activity ->
            val journey = activity.monitoredVehicleJourney ?: return@mapNotNull null
            val location = journey.vehicleLocation ?: return@mapNotNull null
            val lat = location.latitude ?: return@mapNotNull null
            val lon = location.longitude ?: return@mapNotNull null
            val lineRef = journey.lineRef?.value ?: return@mapNotNull null
            val vehicleId = activity.vehicleMonitoringRef?.value ?: return@mapNotNull null
            val lineName = extractLineNameFromRef(lineRef).trim()
            if (lineName.isBlank()) return@mapNotNull null
            if (!isValidLineName(lineName)) return@mapNotNull null

            SimpleVehiclePosition(
                vehicleId = vehicleId,
                lineName = lineName,
                latitude = lat,
                longitude = lon,
                bearing = journey.bearing,
                destinationName = journey.destinationName?.firstOrNull()?.value,
                direction = journey.directionRef?.value
            )
        }
    }
    
    private fun extractLineNameFromRef(lineRef: String): String {
        lineRefPattern?.find(lineRef)?.value?.let { return it }
        val parts = lineRef.split("::")
        if (parts.size >= 2) {
            val afterDoubleDots = parts[1]
            val colonIndex = afterDoubleDots.indexOf(":")
            if (colonIndex > 0) return afterDoubleDots.take(colonIndex)
            return afterDoubleDots
        }
        return ""
    }

    private fun isValidLineName(lineName: String): Boolean {
        val upper = lineName.trim().uppercase()
        if (upper.isBlank()) return false
        if (rules.strongLines.any { it.equals(upper, ignoreCase = true) }) return true
        return rules.lineNameRegexes.any { regex -> upper.matches(Regex(regex)) }
    }
}
