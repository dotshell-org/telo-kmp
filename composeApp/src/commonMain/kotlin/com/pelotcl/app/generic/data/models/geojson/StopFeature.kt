package com.pelotcl.app.generic.data.models.geojson

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import com.pelotcl.app.generic.data.models.stops.StopGeometry
import com.pelotcl.app.generic.data.models.stops.StopProperties
import kotlinx.serialization.Serializable

/**
 * Represents a transport stop (GeoJSON Feature)
 */
@Immutable
@Serializable
data class StopFeature(
    val type: String = "Feature",
    val id: String = "",
    val geometry: StopGeometry = StopGeometry(),
    @SerialName("geometry_name")
    val geometryName: String? = null,
    val properties: StopProperties = StopProperties(),
    val bbox: List<Double>? = null
)
