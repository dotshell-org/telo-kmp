package com.pelotcl.app.generic.data.repository.api

import com.pelotcl.app.generic.data.models.geojson.FeatureCollection
import com.pelotcl.app.generic.data.models.geojson.Feature

interface TransportRepository {
    suspend fun getAllLines(): Result<FeatureCollection>
    suspend fun getLineByName(lineName: String): Result<List<Feature>>
}
