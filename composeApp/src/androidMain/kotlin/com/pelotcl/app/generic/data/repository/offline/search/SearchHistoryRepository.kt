package com.pelotcl.app.generic.data.repository.offline.search

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.pelotcl.app.generic.data.GsonProvider
import androidx.core.content.edit
import com.pelotcl.app.generic.data.telemetry.TelemetryEmitter
import com.pelotcl.app.generic.data.telemetry.TelemetryEvent
import java.time.Instant
import java.util.UUID

/**
 * Repository for managing search history using SharedPreferences.
 * Stores recent searches for quick access.
 */
class SearchHistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("pelo_search_history", Context.MODE_PRIVATE)
    private val gson = GsonProvider.instance

    companion object {
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val MAX_HISTORY_SIZE = 10
    }

    /**
     * Get the search history ordered by most recent first
     */
    fun getSearchHistory(): List<SearchHistoryItem> {
        val json = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SearchHistoryItem>>() {}.type
            gson.fromJson<List<SearchHistoryItem>>(json, type)
                .sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Add a search item to history. If the item already exists, it updates its timestamp.
     * Keeps only the most recent MAX_HISTORY_SIZE items.
     */
    fun addToHistory(item: SearchHistoryItem) {
        val history = getSearchHistory().toMutableList()

        // Remove existing entry with same query and type (case-insensitive)
        history.removeAll {
            it.query.equals(item.query, ignoreCase = true) && it.type == item.type
        }

        // Add new item at the beginning
        history.add(0, item.copy(timestamp = System.currentTimeMillis()))

        // Keep only MAX_HISTORY_SIZE items
        val trimmedHistory = history.take(MAX_HISTORY_SIZE)

        // Save to preferences
        val json = gson.toJson(trimmedHistory)
        prefs.edit { putString(KEY_SEARCH_HISTORY, json) }

        emitTelemetry(item)
    }

    private fun emitTelemetry(item: SearchHistoryItem) {
        // The query is the canonical stop name or line id selected by the user from a result.
        // It is *not* free text — it always matches a known GTFS resource, so we can ship it.
        val now = Instant.now().toString()
        val event = when (item.type) {
            SearchType.STOP -> TelemetryEvent.SearchStop(
                eventId = UUID.randomUUID().toString(),
                at = now,
                stopId = item.query
            )
            SearchType.LINE -> TelemetryEvent.SearchLine(
                eventId = UUID.randomUUID().toString(),
                at = now,
                lineId = item.query
            )
        }
        TelemetryEmitter.emit(event)
    }

    /**
     * Remove a specific item from history
     */
    fun removeFromHistory(query: String, type: SearchType) {
        val history = getSearchHistory().toMutableList()
        history.removeAll {
            it.query.equals(query, ignoreCase = true) && it.type == type
        }
        val json = gson.toJson(history)
        prefs.edit { putString(KEY_SEARCH_HISTORY, json) }
    }

}
