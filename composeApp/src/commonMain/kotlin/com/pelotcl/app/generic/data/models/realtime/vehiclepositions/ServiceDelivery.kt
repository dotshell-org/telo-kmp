package com.pelotcl.app.generic.data.models.realtime.vehiclepositions
import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName

@Serializable
data class ServiceDelivery(
    @SerialName("ResponseTimestamp")
    val responseTimestamp: String?,
    @SerialName("ProducerRef")
    val producerRef: RefValue?,
    @SerialName("ResponseMessageIdentifier")
    val responseMessageIdentifier: RefValue?,
    @SerialName("MoreData")
    val moreData: Boolean?,
    @SerialName("VehicleMonitoringDelivery")
    val vehicleMonitoringDelivery: List<VehicleMonitoringDelivery>?
)
