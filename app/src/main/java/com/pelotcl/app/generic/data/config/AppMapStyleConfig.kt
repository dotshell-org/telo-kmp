package com.pelotcl.app.generic.data.config

import com.pelotcl.app.generic.data.network.mapstyle.MapStyleCategory
import com.pelotcl.app.generic.data.network.mapstyle.MapStyleConfig
import com.pelotcl.app.generic.data.network.mapstyle.MapStyleData

class AppMapStyleConfig(private val data: MapStylesData) : MapStyleConfig {
    override fun getStandardMapStyles(): List<MapStyleData> {
        return data.standard.map { entry ->
            MapStyleData(
                key = entry.key,
                displayName = entry.displayName,
                styleUrl = entry.styleUrl,
                category = MapStyleCategory.STANDARD
            )
        }
    }

    override fun getSatelliteMapStyle(): MapStyleData {
        return MapStyleData(
            key = data.satellite.key,
            displayName = data.satellite.displayName,
            styleUrl = data.satellite.styleUrl,
            category = MapStyleCategory.SATELLITE
        )
    }

    override fun getMapStyleByKey(key: String): MapStyleData? {
        val standard = getStandardMapStyles()
        val satellite = getSatelliteMapStyle()
        return (standard + satellite).find { it.key == key }
    }

    override fun getDefaultMapStyle(): MapStyleData {
        val standard = getStandardMapStyles()
        return standard.firstOrNull { it.key == data.defaultKey } ?: standard.first()
    }
}
