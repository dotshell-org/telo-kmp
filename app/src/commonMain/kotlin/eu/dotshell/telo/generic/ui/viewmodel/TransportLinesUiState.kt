package eu.dotshell.telo.generic.ui.viewmodel

import eu.dotshell.telo.generic.data.models.geojson.Feature

/**
 * Etats pour les lignes de transport (Compatibilite PlanScreen)
 */
sealed class TransportLinesUiState {
    data object Loading : TransportLinesUiState()
    data class Success(val lines: List<Feature>) : TransportLinesUiState()
    data class PartialSuccess(val lines: List<Feature>) : TransportLinesUiState()
    data class Error(val message: String) : TransportLinesUiState()
}
