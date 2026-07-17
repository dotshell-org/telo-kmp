package eu.dotshell.telo.generic.data.models.search

import androidx.compose.runtime.Immutable

/**
 * A geocoded address or point of interest, usable as an itinerary endpoint.
 *
 * @param label Main display line (POI name or "12 Rue de la République")
 * @param detail Secondary display line ("Rue du Dr Bouchut, 83000 Toulon"), null when redundant
 */
@Immutable
data class AddressSearchResult(
    val label: String,
    val detail: String?,
    val lat: Double,
    val lon: Double
)
