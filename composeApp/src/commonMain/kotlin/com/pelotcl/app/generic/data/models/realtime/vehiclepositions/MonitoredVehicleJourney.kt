package com.pelotcl.app.generic.data.models.realtime.vehiclepositions
import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName
import com.pelotcl.app.generic.data.models.realtime.vehiclepositions.RefValue
import com.pelotcl.app.generic.data.models.TranslatedString

@Serializable
data class MonitoredVehicleJourney(
    @SerialName("LineRef")
    val lineRef: RefValue?,
    @SerialName("DirectionRef")
    val directionRef: RefValue?,
    @SerialName("FramedVehicleJourneyRef")
    val framedVehicleJourneyRef: FramedVehicleJourneyRef?,
    @SerialName("DestinationRef")
    val destinationRef: RefValue?,
    @SerialName("DestinationName")
    val destinationName: List<TranslatedString>?,
    @SerialName("Bearing")
    val bearing: Double?,
    @SerialName("VehicleLocation")
    val vehicleLocation: VehicleLocation?,
    @SerialName("VehicleStatus")
    val vehicleStatus: String?
)
