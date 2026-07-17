package eu.dotshell.telo

import eu.dotshell.telo.generic.data.network.geocoding.PhotonResponse
import eu.dotshell.telo.generic.data.repository.geocoding.isNearToulon
import eu.dotshell.telo.generic.data.repository.geocoding.mapPhotonFeatures
import eu.dotshell.telo.generic.data.repository.geocoding.photonFeatureToResult
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Photon response mapping: GeoJSON [lon, lat] ordering, label/detail building,
 * region filtering and deduplication — on fixture JSON, no HTTP involved.
 */
class GeocodingRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    // A realistic Photon payload: a POI, a house-number address, an unusable feature,
    // an out-of-region hit (Paris) and a duplicate of the POI.
    private val fixture = """
    {
      "type": "FeatureCollection",
      "features": [
        {
          "type": "Feature",
          "geometry": { "type": "Point", "coordinates": [5.9928, 43.1362] },
          "properties": { "name": "Carrefour Grand Var", "street": "Avenue de l'Université",
                          "postcode": "83160", "city": "La Valette-du-Var", "osm_key": "shop", "osm_value": "supermarket" }
        },
        {
          "type": "Feature",
          "geometry": { "type": "Point", "coordinates": [5.9308, 43.1245] },
          "properties": { "housenumber": "12", "street": "Rue Jean Jaurès",
                          "postcode": "83000", "city": "Toulon", "osm_key": "place", "osm_value": "house" }
        },
        {
          "type": "Feature",
          "geometry": { "type": "Point", "coordinates": [5.93, 43.12] },
          "properties": { "postcode": "83000", "city": "Toulon" }
        },
        {
          "type": "Feature",
          "geometry": { "type": "Point", "coordinates": [2.2945, 48.8584] },
          "properties": { "name": "Tour Eiffel", "city": "Paris" }
        },
        {
          "type": "Feature",
          "geometry": { "type": "Point", "coordinates": [5.9928, 43.1362] },
          "properties": { "name": "Carrefour Grand Var", "street": "Avenue de l'Université",
                          "postcode": "83160", "city": "La Valette-du-Var" }
        }
      ]
    }
    """.trimIndent()

    @Test
    fun mapsFixtureFilteringAndDeduplicating() {
        val response = json.decodeFromString<PhotonResponse>(fixture)
        assertEquals(5, response.features.size)

        val results = mapPhotonFeatures(response.features, limit = 6)

        // Unusable + Paris + duplicate dropped, Photon order preserved
        assertEquals(2, results.size)

        val poi = results[0]
        assertEquals("Carrefour Grand Var", poi.label)
        assertEquals("Avenue de l'Université, 83160 La Valette-du-Var", poi.detail)
        // GeoJSON order is [lon, lat]
        assertEquals(43.1362, poi.lat, 1e-9)
        assertEquals(5.9928, poi.lon, 1e-9)

        val address = results[1]
        assertEquals("12 Rue Jean Jaurès", address.label)
        assertEquals("83000 Toulon", address.detail)
    }

    @Test
    fun limitIsApplied() {
        val response = json.decodeFromString<PhotonResponse>(fixture)
        val results = mapPhotonFeatures(response.features, limit = 1)
        assertEquals(1, results.size)
        assertEquals("Carrefour Grand Var", results[0].label)
    }

    @Test
    fun featureWithoutNameOrStreetIsDropped() {
        val response = json.decodeFromString<PhotonResponse>(fixture)
        assertNull(photonFeatureToResult(response.features[2]))
    }

    @Test
    fun featureWithoutCoordinatesIsDropped() {
        val broken = json.decodeFromString<PhotonResponse>(
            """{"features":[{"geometry":{"coordinates":[]},"properties":{"name":"Nowhere"}}]}"""
        )
        assertNull(photonFeatureToResult(broken.features[0]))
    }

    @Test
    fun regionFilterKeepsToulonAreaOnly() {
        assertTrue("Toulon center", isNearToulon(43.1242, 5.9280))
        assertTrue("Hyères (~16 km)", isNearToulon(43.1206, 6.1286))
        assertFalse("Paris (~700 km)", isNearToulon(48.8584, 2.2945))
        assertFalse("Lyon (~300 km)", isNearToulon(45.7640, 4.8357))
    }
}
