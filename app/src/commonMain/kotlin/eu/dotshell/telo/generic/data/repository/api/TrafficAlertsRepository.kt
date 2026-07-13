package eu.dotshell.telo.generic.data.repository.api

import eu.dotshell.telo.generic.data.models.realtime.alerts.official.TrafficAlert

interface TrafficAlertsRepository {
    suspend fun getTrafficAlerts(): Result<List<TrafficAlert>>
}
