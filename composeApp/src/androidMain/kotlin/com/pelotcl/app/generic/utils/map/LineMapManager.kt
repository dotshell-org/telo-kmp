package com.pelotcl.app.generic.utils.map

import com.pelotcl.app.generic.data.models.geojson.Feature
import com.pelotcl.app.generic.service.TransportServiceProvider
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory

object LineMapManager {

    private val lineRules get() = TransportServiceProvider.getTransportLineRules()

    fun filterMapLines(
        map: MapLibreMap,
        allLines: List<Feature>,
        selectedLineName: String
    ): Int {
        val selectedAliases = when (lineRules.canonicalRouteName(selectedLineName)) {
            "NAV1" -> listOf("NAV1", "NAVI1")
            else -> listOf(selectedLineName.trim().uppercase())
        }

        map.getStyle { style ->
            val layerId = "all-lines-layer"
            val existingLayer = style.getLayer(layerId)

            if (existingLayer != null) {
                val filterExpressions = selectedAliases.map { alias ->
                    Expression.eq(Expression.get("ligne"), alias)
                }.toTypedArray()
                val lineFilter = if (filterExpressions.size == 1) {
                    filterExpressions.first()
                } else {
                    Expression.any(*filterExpressions)
                }
                (existingLayer as? LineLayer)?.setFilter(lineFilter)
            }

            allLines.forEach { feature ->
                val ligne = feature.properties.lineName
                val codeTrace = feature.properties.traceCode
                val individualLayerId = "layer-${ligne}-${codeTrace}"

                style.getLayer(individualLayerId)?.let { layer ->
                    val shouldBeVisible =
                        lineRules.canonicalRouteName(ligne) == lineRules.canonicalRouteName(selectedLineName)
                    layer.setProperties(
                        PropertyFactory.visibility(if (shouldBeVisible) "visible" else "none")
                    )
                }
            }
        }

        return allLines.count {
            lineRules.canonicalRouteName(it.properties.lineName) == lineRules.canonicalRouteName(selectedLineName)
        }
    }
}
