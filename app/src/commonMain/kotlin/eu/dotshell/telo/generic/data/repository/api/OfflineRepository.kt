package eu.dotshell.telo.generic.data.repository.api

import eu.dotshell.telo.generic.data.models.geojson.Feature
import eu.dotshell.telo.generic.data.models.geojson.StopFeature
import eu.dotshell.telo.generic.data.models.realtime.alerts.official.TrafficAlert

interface OfflineRepository {
    suspend fun loadStops(): List<StopFeature>?
    suspend fun saveStops(stops: List<StopFeature>)
    suspend fun clearStopsCache()
    suspend fun loadAllLines(): List<Feature>
    suspend fun loadTrafficAlerts(): List<TrafficAlert>?
    suspend fun saveTrafficAlerts(alerts: List<TrafficAlert>)
}
