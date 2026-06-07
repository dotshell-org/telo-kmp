package com.pelotcl.app.generic.data.repository.api

import com.pelotcl.app.generic.data.models.realtime.alerts.official.TrafficAlert

interface TrafficAlertsRepository {
    suspend fun getTrafficAlerts(): Result<List<TrafficAlert>>
}
