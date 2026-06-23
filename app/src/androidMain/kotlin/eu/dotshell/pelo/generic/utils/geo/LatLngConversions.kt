package eu.dotshell.pelo.generic.utils.geo

import eu.dotshell.pelo.generic.utils.location.GeoPoint
import org.maplibre.android.geometry.LatLng

/**
 * Bridges the Android MapLibre SDK's [LatLng] and the SDK-neutral [GeoPoint] used by the
 * shared geometry/map code. Temporary: the conversions disappear once the imperative
 * MapLibre SDK code is replaced by the declarative MapCanvas.
 */
fun LatLng.toGeoPoint(): GeoPoint = GeoPoint(latitude = latitude, longitude = longitude)

fun GeoPoint.toLatLng(): LatLng = LatLng(latitude, longitude)
