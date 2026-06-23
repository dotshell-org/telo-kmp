package eu.dotshell.pelo.specific.data.network

import eu.dotshell.pelo.generic.data.models.geojson.Feature
import eu.dotshell.pelo.generic.data.models.geojson.FeatureCollection
import eu.dotshell.pelo.generic.data.models.geojson.StopCollection
import eu.dotshell.pelo.generic.data.models.lines.MultiLineStringGeometry
import eu.dotshell.pelo.generic.data.models.lines.TransportLineProperties
import eu.dotshell.pelo.generic.data.models.realtime.alerts.community.UserStopAlertsResponse
import eu.dotshell.pelo.generic.data.models.realtime.alerts.official.TrafficAlertsResponse
import eu.dotshell.pelo.generic.data.network.transport.TransportApi
import eu.dotshell.pelo.generic.data.network.transport.TransportLinesQuery
import eu.dotshell.pelo.platform.Log
import eu.dotshell.pelo.platform.createHttpClientEngine
import eu.dotshell.pelo.specific.data.mapper.StopMapper
import eu.dotshell.pelo.specific.data.mapper.TrafficAlertMapper
import eu.dotshell.pelo.specific.data.mapper.TransportLineMapper
import eu.dotshell.pelo.specific.data.model.LyonFeatureCollection
import eu.dotshell.pelo.specific.data.model.LyonStopCollection
import eu.dotshell.pelo.specific.data.model.LyonTrafficAlertsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs

private const val TAG = "LyonKtorClient"

/**
 * Lyon-specific implementation of [TransportApi] using Ktor (KMP-compatible).
 * Replaces Retrofit + OkHttp + Gson. Uses kotlinx.serialization for JSON parsing.
 *
 * Handles:
 * - WFS GeoJSON line geometries (metro, tram, bus, navigone, trambus, RX)
 * - WFS GeoJSON transport stops
 * - Traffic alerts (Pelo API)
 * - User stop alerts (karma-based)
 */
class LyonKtorClient(private val baseUrl: String) : TransportApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val httpClient = HttpClient(createHttpClientEngine()) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.NONE
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d(TAG, message)
                }
            }
        }
    }

    // ─── TransportApi ────────────────────────────────────────────────────────

    override suspend fun getLines(query: TransportLinesQuery): FeatureCollection {
        return when (query) {
            is TransportLinesQuery.StrongLines -> fetchStrongLines()
            is TransportLinesQuery.LineByName  -> fetchLineByName(query.lineName)
            is TransportLinesQuery.BusPage     -> fetchBusPage(query.startIndex, query.count)
        }
    }

    override suspend fun getTransportStops(): StopCollection {
        val lyon = wfsGet<LyonStopCollection>(
            baseUrl = baseUrl,
            typename = TYPENAME_STOPS,
            srsName = SRSNAME_4171,
            count = COUNT_STOPS
        )
        Log.i(TAG, "WFS stops response: ${lyon.features.size} features")
        return StopMapper.mapToGeneric(lyon)
    }

    override suspend fun getTrafficAlerts(): TrafficAlertsResponse {
        val lyon = httpClient.get("${TRAFFIC_ALERTS_BASE_URL}pelo/v1/traffic/alerts")
            .body<LyonTrafficAlertsResponse>()
        return TrafficAlertMapper.mapResponseToGeneric(lyon)
    }

    // ─── User stop alerts (not part of TransportApi, called directly) ─────────

    suspend fun getUserStopAlerts(stopIds: List<String>): UserStopAlertsResponse {
        if (stopIds.isEmpty()) return emptyMap()
        val timestampMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        return httpClient.get("${TRAFFIC_ALERTS_BASE_URL}pelo/v1/users-alerts/stops") {
            header(HttpHeaders.CacheControl, "no-cache")
            header("Pragma", "no-cache")
            stopIds.forEach { parameter("stopIds", it) }
            parameter("_ts", timestampMs)
        }.body()
    }

    // ─── Line fetching helpers ────────────────────────────────────────────────

    private suspend fun fetchStrongLines(): FeatureCollection = supervisorScope {
        val metroDeferred = async { fetchLines(TYPENAME_METRO, SRSNAME_4171, COUNT_METRO_TRAM_NAVIGONE) }
        val tramDeferred  = async { fetchLines(TYPENAME_TRAM,  SRSNAME_4171, COUNT_METRO_TRAM_NAVIGONE) }
        val rxDeferred    = async { fetchRhonexpressFeatures() }

        val allFeatures = metroDeferred.await() + tramDeferred.await() + rxDeferred.await()
        FeatureCollection(type = "FeatureCollection", features = allFeatures)
    }

    private suspend fun fetchLineByName(lineName: String): FeatureCollection {
        val rules = eu.dotshell.pelo.generic.service.TransportServiceProvider.getTransportLineRules()
        val normalized = normalizeToken(lineName)
        val isMetroRequest    = normalized in listOf("a", "b", "c", "d")
        val isTramRequest     = normalized.startsWith("t") && normalized.length <= 3 && !normalized.startsWith("tb")
        val isNavigoneRequest = normalized == "vaporetto" || normalized == "navigone" || rules.isNavigoneLine(lineName)

        val features: List<Feature> = when {
            lineName == "RX" -> fetchRhonexpressFeatures()

            isMetroRequest -> fetchLines(TYPENAME_METRO, SRSNAME_4171, COUNT_METRO_TRAM_NAVIGONE)
                .filter { normalizeToken(it.properties.lineName) == normalized }

            isTramRequest -> fetchLines(TYPENAME_TRAM, SRSNAME_4171, COUNT_METRO_TRAM_NAVIGONE)
                .filter { normalizeToken(it.properties.lineName) == normalized }

            isNavigoneRequest -> {
                fetchLines(TYPENAME_NAVIGONE, SRSNAME_4171, COUNT_METRO_TRAM_NAVIGONE)
                    .filter { feat ->
                        val featName = feat.properties.lineName
                        normalized == "navigone" || normalized == "vaporetto" ||
                        rules.normalizeForComparison(featName) == rules.normalizeForComparison(lineName)
                    }
            }

            else -> {
                val canonical = rules.canonicalRouteName(lineName)
                val escapedAlias = normalizeToken(canonical).replace("'", "''")
                val cqlFilter = "ligne = '$escapedAlias'"
                fetchBusLinesByFilter(cqlFilter, BUS_LINE_BY_NAME_COUNT)
            }
        }

        val unique = features
            .groupBy { it.properties.traceCode }
            .map { (_, group) -> group.first() }

        return FeatureCollection(
            type = "FeatureCollection",
            features = unique,
            totalFeatures = unique.size,
            numberMatched = unique.size,
            numberReturned = unique.size
        )
    }

    private suspend fun fetchBusPage(startIndex: Int, count: Int): FeatureCollection {
        return FeatureCollection(
            type = "FeatureCollection",
            features = fetchLines(TYPENAME_BUS, SRSNAME_4171, count, startIndex = startIndex)
        )
    }

    // ─── WFS GeoJSON helpers ──────────────────────────────────────────────────

    private suspend fun fetchLines(
        typename: String,
        srsName: String,
        count: Int,
        startIndex: Int = 0,
        cqlFilter: String? = null
    ): List<Feature> {
        val lyon = wfsGet<LyonFeatureCollection>(baseUrl, typename, srsName, count, startIndex, cqlFilter)
        return TransportLineMapper.mapToGeneric(lyon).features
    }

    private suspend fun fetchBusLinesByFilter(cqlFilter: String, count: Int): List<Feature> {
        val lyon = wfsGet<LyonFeatureCollection>(baseUrl, TYPENAME_BUS, SRSNAME_4171, count, cqlFilter = cqlFilter)
        return TransportLineMapper.mapToGeneric(lyon).features
    }

    private suspend fun fetchRhonexpressFeatures(): List<Feature> {
        // Rhônexpress uses a different typename + SRSNAME and its JSON structure
        // may not match the standard LyonFeatureCollection, so we parse it manually.
        return try {
            val raw = httpClient.get("$baseUrl/geoserver/sytral/ows") {
                parameter("SERVICE", SERVICE)
                parameter("VERSION", VERSION)
                parameter("request", REQUEST)
                parameter("typename", TYPENAME_RX_LINES)
                parameter("outputFormat", OUTPUT_FORMAT)
                parameter("SRSNAME", SRSNAME_4326)
                parameter("startIndex", START_INDEX)
                parameter("sortby", SORT_BY)
                parameter("count", COUNT_RX_LINES)
            }.body<JsonObject>()
            mapRxJsonToFeatures(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch Rhônexpress: ${e.message}")
            emptyList()
        }
    }

    /** Generic WFS GET → deserialize into [T]. */
    private suspend inline fun <reified T> wfsGet(
        baseUrl: String,
        typename: String,
        srsName: String,
        count: Int,
        startIndex: Int = 0,
        cqlFilter: String? = null
    ): T {
        return httpClient.get("$baseUrl/geoserver/sytral/ows") {
            parameter("SERVICE", SERVICE)
            parameter("VERSION", VERSION)
            parameter("request", REQUEST)
            parameter("typename", typename)
            parameter("outputFormat", OUTPUT_FORMAT)
            parameter("SRSNAME", srsName)
            parameter("startIndex", startIndex)
            parameter("sortby", SORT_BY)
            parameter("count", count)
            if (cqlFilter != null) parameter("cql_filter", cqlFilter)
        }.body()
    }

    // ─── Rhônexpress JSON parsing (manual, handles non-standard shapes) ───────

    private fun mapRxJsonToFeatures(json: JsonObject): List<Feature> {
        val featuresArray: JsonArray = when {
            json.containsKey("features")      -> json["features"]!!.jsonArray
            json.containsKey("featureMember") -> json["featureMember"]!!.jsonArray
            json.containsKey("featureMembers")-> json["featureMembers"]!!.jsonArray
            else -> return emptyList()
        }

        val result = mutableListOf<Feature>()

        for (featureElement in featuresArray) {
            val featureObject = featureElement.jsonObject
            val id = featureObject["id"]?.jsonPrimitive?.content ?: "rx-${kotlin.random.Random.nextLong()}"

            val properties = featureObject["properties"]?.jsonObject ?: featureObject
            val gid = properties["gid"]?.jsonPrimitive?.runCatching { int }?.getOrNull()
                ?: properties["GID"]?.jsonPrimitive?.runCatching { int }?.getOrNull()
                ?: abs(id.hashCode())

            val geometryObject = (featureObject["geometry"]
                ?: featureObject["the_geom"]
                ?: featureObject["geom"])?.jsonObject ?: continue

            val coordinatesElement = geometryObject["coordinates"] ?: continue
            val coordinates = parseCoordinatesAsMultiLines(coordinatesElement) ?: continue

            result.add(
                Feature(
                    type = "Feature",
                    id = "rx_$id",
                    multiLineStringGeometry = MultiLineStringGeometry(
                        type = "MultiLineString",
                        coordinates = coordinates
                    ),
                    geometryName = null,
                    properties = TransportLineProperties(
                        lineName = "RX",
                        traceCode = "RX-$gid",
                        lineId = "RX",
                        traceType = "",
                        traceName = "Rhônexpress",
                        direction = "ALLER",
                        origin = "Gare Part-Dieu Villette",
                        destination = "Aéroport St Exupéry -RX",
                        originName = "Gare Part-Dieu Villette",
                        destinationName = "Aéroport St Exupéry -RX",
                        transportType = "TRAM",
                        startDate = "",
                        endDate = null,
                        lineTypeCode = "TRAM",
                        lineTypeName = "Tramway",
                        lastUpdate = "",
                        lastUpdateFme = "",
                        gid = gid,
                        color = "#E30613"
                    ),
                    bbox = null
                )
            )
        }
        return result
    }

    private fun parsePosition(pos: JsonElement): List<Double>? {
        val arr = runCatching { pos.jsonArray }.getOrNull() ?: return null
        if (arr.size < 2) return null
        return runCatching {
            listOf(arr[0].jsonPrimitive.double, arr[1].jsonPrimitive.double)
        }.getOrNull()
    }

    private fun parseCoordinatesAsMultiLines(coords: JsonElement): List<List<List<Double>>>? {
        val outer = runCatching { coords.jsonArray }.getOrNull() ?: return null
        if (outer.isEmpty()) return null

        val first = runCatching { outer[0].jsonArray }.getOrNull() ?: return null
        if (first.isEmpty()) return null
        val firstInner = runCatching { first[0].jsonArray }.getOrNull()
        val isMultiLine = firstInner != null

        return if (isMultiLine) {
            val lines = outer.mapNotNull { lineEl ->
                val lineArr = runCatching { lineEl.jsonArray }.getOrNull() ?: return@mapNotNull null
                val points = lineArr.mapNotNull { parsePosition(it) }
                if (points.isEmpty()) null else points
            }
            if (lines.isEmpty()) null else lines
        } else {
            val points = outer.mapNotNull { parsePosition(it) }
            if (points.isEmpty()) null else listOf(points)
        }
    }

    // ─── Text normalization ───────────────────────────────────────────────────

    private fun normalizeToken(token: String): String =
        token.lowercase().trim().let { s ->
            // Remove accents via basic ASCII folding (KMP-safe, no java.text.Normalizer)
            buildString {
                for (c in s) {
                    append(ACCENT_MAP[c] ?: c)
                }
            }
        }

    companion object {
        private const val TRAFFIC_ALERTS_BASE_URL = "https://api.dotshell.eu/"

        private const val SERVICE = "WFS"
        private const val VERSION = "2.0.0"
        private const val REQUEST = "GetFeature"
        private const val OUTPUT_FORMAT = "application/json"
        private const val SORT_BY = "gid"
        private const val START_INDEX = 0

        private const val SRSNAME_4171 = "EPSG:4171"
        private const val SRSNAME_4326 = "EPSG:4326"

        private const val COUNT_METRO_TRAM_NAVIGONE = 1000
        private const val COUNT_STOPS = 10000

        private const val TYPENAME_METRO    = "sytral:tcl_sytral.tcllignemf_2_0_0"
        private const val TYPENAME_TRAM     = "sytral:tcl_sytral.tcllignetram_2_0_0"
        private const val TYPENAME_BUS      = "sytral:tcl_sytral.tcllignebus_2_0_0"
        private const val TYPENAME_NAVIGONE = "sytral:tcl_sytral.tcllignefluv"
        private const val TYPENAME_STOPS    = "sytral:tcl_sytral.tclarret"
        private const val TYPENAME_RX_LINES = "sytral:rx_rhonexpress.rxligne_2_0_0"

        private const val COUNT_RX_LINES = 1000
        private const val BUS_LINE_BY_NAME_COUNT = 200

        /** Simple accent folding map — avoids java.text.Normalizer (not in KMP stdlib). */
        private val ACCENT_MAP = mapOf(
            'à' to 'a', 'â' to 'a', 'ä' to 'a', 'á' to 'a', 'ã' to 'a',
            'è' to 'e', 'ê' to 'e', 'ë' to 'e', 'é' to 'e',
            'î' to 'i', 'ï' to 'i', 'í' to 'i', 'ì' to 'i',
            'ô' to 'o', 'ö' to 'o', 'ó' to 'o', 'ò' to 'o', 'õ' to 'o',
            'ù' to 'u', 'û' to 'u', 'ü' to 'u', 'ú' to 'u',
            'ç' to 'c', 'ñ' to 'n'
        )
    }
}
