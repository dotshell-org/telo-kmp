package com.pelotcl.app.generic.data.models.realtime.vehiclepositions
import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName
import com.pelotcl.app.generic.data.models.realtime.vehiclepositions.RefValue

@Serializable
data class VehicleActivity(
    @SerialName("ValidUntilTime")
    val validUntilTime: String?,
    @SerialName("VehicleMonitoringRef")
    val vehicleMonitoringRef: RefValue?,
    @SerialName("MonitoredVehicleJourney")
    val monitoredVehicleJourney: MonitoredVehicleJourney?
)
