package com.pelotcl.app.generic.data.cache

import com.pelotcl.app.generic.data.models.realtime.alerts.official.TrafficAlert
import com.pelotcl.app.platform.Log
import com.pelotcl.app.platform.PlatformContext
import com.pelotcl.app.platform.Settings
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Cache for traffic alerts data using [Settings].
 * Multiplatform: removed android.content.Context, SharedPreferences, Gson.
 */
class TrafficAlertsCache(context: PlatformContext) {

    companion object {
        private const val TAG = "TrafficAlertsCache"
        private const val CACHE_FILE_NAME = "traffic_alerts_cache"
        private const val ALERTS_CACHE_KEY = "traffic_alerts"
        private const val TIMESTAMP_CACHE_KEY = "traffic_alerts_timestamp"
        private const val TIMESTAMP_MILLIS_KEY = "traffic_alerts_timestamp_millis"
    }

    private val settings = Settings(context, CACHE_FILE_NAME)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Saves traffic alerts to cache.
     */
    fun saveTrafficAlerts(alerts: List<TrafficAlert>, timestamp: String) {
        try {
            val alertsJson = json.encodeToString(alerts)
            settings.putString(ALERTS_CACHE_KEY, alertsJson)
            settings.putString(TIMESTAMP_CACHE_KEY, timestamp)
            settings.putLong(TIMESTAMP_MILLIS_KEY, Clock.System.now().toEpochMilliseconds())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving traffic alerts to cache: ${e.message}")
        }
    }

    /**
     * Gets traffic alerts from cache.
     */
    fun getTrafficAlerts(): Pair<List<TrafficAlert>, String>? {
        return try {
            val alertsJson = settings.getString(ALERTS_CACHE_KEY, "").takeIf { it.isNotBlank() }
            val timestamp = settings.getString(TIMESTAMP_CACHE_KEY, "").takeIf { it.isNotBlank() }

            if (alertsJson != null && timestamp != null) {
                val alerts = json.decodeFromString<List<TrafficAlert>>(alertsJson)
                Pair(alerts, timestamp)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading traffic alerts from cache: ${e.message}")
            null
        }
    }
}