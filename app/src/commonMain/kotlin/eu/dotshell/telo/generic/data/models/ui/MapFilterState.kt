package eu.dotshell.telo.generic.data.models.ui

import eu.dotshell.telo.generic.ui.screens.plan.LineInfo
import eu.dotshell.telo.generic.ui.viewmodel.TransportLinesUiState
import eu.dotshell.telo.generic.ui.viewmodel.TransportStopsUiState

/**
 * Data class to hold map filter state for snapshotFlow.
 * Used to batch state changes and avoid excessive recompositions.
 */
data class MapFilterState(
    val sheetContentState: SheetContentState?,
    val selectedLine: LineInfo?,
    val uiState: TransportLinesUiState,
    val stopsUiState: TransportStopsUiState
)
