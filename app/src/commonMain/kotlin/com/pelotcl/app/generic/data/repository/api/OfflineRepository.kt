package com.pelotcl.app.generic.data.repository.api

import com.pelotcl.app.generic.data.models.geojson.Feature
import com.pelotcl.app.generic.data.models.geojson.StopFeature
import com.pelotcl.app.generic.data.models.realtime.alerts.official.TrafficAlert

interface OfflineRepository {
    suspend fun loadStops(): List<StopFeature>?
    suspend fun saveStops(stops: List<StopFeature>)
    suspend fun clearStopsCache()
    suspend fun loadAllLines(): List<Feature>
    suspend fun loadTrafficAlerts(): List<TrafficAlert>?
    suspend fun saveTrafficAlerts(alerts: List<TrafficAlert>)
}
