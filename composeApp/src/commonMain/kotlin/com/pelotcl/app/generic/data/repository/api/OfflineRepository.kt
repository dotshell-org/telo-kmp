package com.pelotcl.app.generic.data.repository.api

import com.pelotcl.app.generic.data.models.geojson.StopFeature

interface OfflineRepository {
    suspend fun loadStops(): List<StopFeature>?
    suspend fun saveStops(stops: List<StopFeature>): Int
    suspend fun clearStopsCache()
}
