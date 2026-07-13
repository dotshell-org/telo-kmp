package eu.dotshell.telo.generic.ui.viewmodel

import eu.dotshell.telo.generic.data.models.realtime.alerts.official.TrafficAlert

/**
 * Etats pour les alertes trafic
 */
sealed class TrafficAlertsState {
    data object Loading : TrafficAlertsState()
    data class Success(val alerts: List<TrafficAlert>) : TrafficAlertsState()
    data class Error(val message: String) : TrafficAlertsState()
}
