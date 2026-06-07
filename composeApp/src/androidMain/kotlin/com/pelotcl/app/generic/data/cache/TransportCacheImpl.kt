package com.pelotcl.app.generic.data.cache

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.pelotcl.app.generic.data.config.AppConfigLoader
import com.pelotcl.app.generic.data.models.geojson.Feature
import com.pelotcl.app.generic.data.models.geojson.StopFeature
import com.pelotcl.app.generic.data.offline.sanitizeForSerialization
import com.pelotcl.app.generic.data.offline.sanitizeStopsForSerialization
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Generic implementation of TransportCache driven by config.yml.
 */
class TransportCacheImpl(context: Context) : TransportCache {

    private class LineCacheSlot(
        val fileName: String,
        val timestampKey: String,
        @Volatile var memory: List<Feature>? = null,
        @Volatile var timestamp: Long = 0L,
        val mutex: Mutex = Mutex()
    )

    private val cacheConfig = AppConfigLoader.loadConfig(context).cache
    private val cacheValidityDuration = TimeUnit.HOURS.toMillis(cacheConfig.validityHours)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val prefs = context.getSharedPreferences("transport_cache_meta", Context.MODE_PRIVATE)
    private val stopsMutex = Mutex()
    private val cacheDir = File(context.cacheDir, "transport_data").also { it.mkdirs() }

    private val metroSlot = LineCacheSlot(FILE_METRO_LINES, KEY_METRO_LINES_TIMESTAMP)
    private val tramSlot = LineCacheSlot(FILE_TRAM_LINES, KEY_TRAM_LINES_TIMESTAMP)
    private val busSlot = LineCacheSlot(FILE_BUS_LINES, KEY_BUS_LINES_TIMESTAMP)

    private val lineSlots: List<LineCacheSlot> = buildList {
        add(metroSlot)
        add(tramSlot)
        if (cacheConfig.cacheBusLines) add(busSlot)
    }

    @Volatile
    private var stopsCache: List<StopFeature>? = null

    @Volatile
    private var stopsTimestamp: Long = 0L

    companion object {
        private const val TAG = "TransportCache"

        private const val KEY_METRO_LINES_TIMESTAMP = "metro_lines_timestamp"
        private const val KEY_TRAM_LINES_TIMESTAMP = "tram_lines_timestamp"
        private const val KEY_BUS_LINES_TIMESTAMP = "bus_lines_timestamp"
        private const val KEY_STOPS_TIMESTAMP = "stops_timestamp"

        private const val FILE_METRO_LINES = "metro_lines.json.gz"
        private const val FILE_TRAM_LINES = "tram_lines.json.gz"
        private const val FILE_BUS_LINES = "bus_lines.json.gz"
        private const val FILE_STOPS = "stops.json.gz"
    }

    override fun hasAnyCachedData(): Boolean {
        if (lineSlots.any { it.memory != null } || stopsCache != null) return true

        val hasLineCache = lineSlots.any { prefs.getLong(it.timestampKey, 0) > 0 }
        val hasStopsCache = prefs.getLong(KEY_STOPS_TIMESTAMP, 0) > 0
        return hasLineCache || hasStopsCache
    }

    override fun needsCacheRefresh(): Boolean {
        if (!hasAnyCachedData()) return true

        val now = System.currentTimeMillis()
        val lineExpired = lineSlots.any { slot ->
            val timestamp = prefs.getLong(slot.timestampKey, 0)
            timestamp > 0 && (now - timestamp) >= cacheValidityDuration
        }
        val stopsExpired = prefs.getLong(KEY_STOPS_TIMESTAMP, 0) > 0 &&
            (now - prefs.getLong(KEY_STOPS_TIMESTAMP, 0)) >= cacheValidityDuration

        return lineExpired || stopsExpired
    }

    private fun isTimestampValid(timestamp: Long): Boolean {
        return (System.currentTimeMillis() - timestamp) < cacheValidityDuration
    }

    private suspend inline fun <reified T> writeToCompressedFile(fileName: String, data: T) =
        withContext(Dispatchers.IO) {
            try {
                if ((data as? List<*>)?.isEmpty() == true) {
                    Log.w(TAG, "Attempted to write empty data to $fileName, skipping")
                    return@withContext
                }
                val file = File(cacheDir, fileName)
                val jsonString = json.encodeToString(data)
                if (jsonString.isBlank()) {
                    Log.e(TAG, "Serialization produced blank JSON for $fileName")
                    return@withContext
                }
                GZIPOutputStream(FileOutputStream(file).buffered()).use { gzip ->
                    gzip.write(jsonString.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to $fileName", e)
            }
        }

    private suspend inline fun <reified T> readFromCompressedFile(fileName: String): T? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, fileName)
                if (!file.exists()) return@withContext null

                val jsonString = GZIPInputStream(FileInputStream(file).buffered()).use { gzip ->
                    gzip.bufferedReader(Charsets.UTF_8).readText()
                }
                if (jsonString.isBlank()) {
                    Log.w(TAG, "Cache file $fileName is blank, deleting")
                    file.delete()
                    return@withContext null
                }
                json.decodeFromString<T>(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Error reading from $fileName", e)
                runCatching { File(cacheDir, fileName).delete() }
                null
            }
        }

    private suspend fun invalidateLineCache(fileName: String, timestampKey: String) {
        withContext(Dispatchers.IO) {
            runCatching { File(cacheDir, fileName).delete() }
        }
        prefs.edit { putLong(timestampKey, 0L) }
    }

    private fun isInvalidLineCache(lines: List<Feature>): Boolean {
        if (lines.isEmpty()) return false
        val validEntries = lines.count {
            it.properties.lineName.isNotBlank() && it.properties.traceCode.isNotBlank()
        }
        return validEntries == 0
    }

    private suspend fun saveLineCache(slot: LineCacheSlot, lines: List<Feature>) {
        slot.memory = lines
        slot.timestamp = System.currentTimeMillis()
        prefs.edit { putLong(slot.timestampKey, slot.timestamp) }
        writeToCompressedFile(slot.fileName, lines.sanitizeForSerialization())
    }

    private suspend fun loadLineCache(slot: LineCacheSlot): List<Feature>? {
        if (slot.memory != null && isTimestampValid(slot.timestamp)) {
            if (isInvalidLineCache(slot.memory.orEmpty())) {
                slot.memory = null
                slot.timestamp = 0L
                invalidateLineCache(slot.fileName, slot.timestampKey)
                return null
            }
            return slot.memory
        }

        val timestamp = prefs.getLong(slot.timestampKey, 0)
        if (isTimestampValid(timestamp)) {
            val lines = readFromCompressedFile<List<Feature>>(slot.fileName)
            if (lines != null) {
                if (isInvalidLineCache(lines)) {
                    invalidateLineCache(slot.fileName, slot.timestampKey)
                    return null
                }
                slot.memory = lines
                slot.timestamp = timestamp
                return lines
            }
        }

        return null
    }

    override suspend fun saveMetroLines(lines: List<Feature>) {
        metroSlot.mutex.withLock { saveLineCache(metroSlot, lines) }
    }

    override suspend fun getMetroLines(): List<Feature>? = metroSlot.mutex.withLock {
        loadLineCache(metroSlot)
    }

    override suspend fun saveTramLines(lines: List<Feature>) {
        tramSlot.mutex.withLock { saveLineCache(tramSlot, lines) }
    }

    override suspend fun getTramLines(): List<Feature>? = tramSlot.mutex.withLock {
        loadLineCache(tramSlot)
    }

    override suspend fun saveBusLines(lines: List<Feature>) {
        if (!cacheConfig.cacheBusLines) return
        busSlot.mutex.withLock { saveLineCache(busSlot, lines) }
    }

    override suspend fun getBusLines(): List<Feature>? = busSlot.mutex.withLock {
        if (!cacheConfig.cacheBusLines) return@withLock null
        loadLineCache(busSlot)
    }

    override suspend fun saveStops(stops: List<StopFeature>) {
        stopsMutex.withLock {
            stopsCache = stops
            stopsTimestamp = System.currentTimeMillis()

            prefs.edit { putLong(KEY_STOPS_TIMESTAMP, stopsTimestamp) }
            writeToCompressedFile(FILE_STOPS, stops.sanitizeStopsForSerialization())
        }
    }

    override suspend fun getStops(): List<StopFeature>? = stopsMutex.withLock {
        if (stopsCache != null && isTimestampValid(stopsTimestamp)) {
            return@withLock stopsCache
        }

        val timestamp = prefs.getLong(KEY_STOPS_TIMESTAMP, 0)
        if (isTimestampValid(timestamp)) {
            val stops = readFromCompressedFile<List<StopFeature>>(FILE_STOPS)
            if (stops != null) {
                stopsCache = stops
                stopsTimestamp = timestamp
                return@withLock stops
            }
        }

        null
    }

    override suspend fun preloadFromDisk() {
        coroutineScope {
            val jobs = lineSlots.map { slot -> async { getLineFromDisk(slot) } } +
                listOf(async { getStops() })
            jobs.awaitAll()
        }
    }

    private suspend fun getLineFromDisk(slot: LineCacheSlot) {
        slot.mutex.withLock { loadLineCache(slot) }
    }
}