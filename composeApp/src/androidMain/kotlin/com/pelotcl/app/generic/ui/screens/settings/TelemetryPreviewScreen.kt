package com.pelotcl.app.generic.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pelotcl.app.generic.data.telemetry.DailyReportState
import com.pelotcl.app.generic.data.telemetry.TelemetryEmitter
import com.pelotcl.app.generic.data.telemetry.TelemetryEvent
import com.pelotcl.app.generic.ui.theme.PrimaryColor
import com.pelotcl.app.generic.ui.theme.SecondaryColor
import kotlinx.serialization.json.Json

/**
 * Transparency dump of the current [DailyReportState], structured for readability:
 *  - top metadata: daily_id, day, network, schema version
 *  - per-kind event counts (so the user can see at a glance what is being collected today)
 *  - the full JSON dump as a fallback for the technically-curious
 *
 * Everything is read from the in-memory state held by [TelemetryEmitter.repository] — no
 * network calls, no disk reads beyond the already-loaded state.
 */
@Composable
fun TelemetryPreviewScreen(
    onBackClick: () -> Unit,
    onSystemBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onSystemBack() }

    var snapshot by remember { mutableStateOf<DailyReportState?>(null) }
    var loading by remember { mutableStateOf(true) }

    val prettyJson = remember {
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
        }
    }

    LaunchedEffect(Unit) {
        snapshot = TelemetryEmitter.repository()?.state?.value
        loading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PrimaryColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 80.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Données collectées aujourd'hui",
                color = SecondaryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Text(
                text = "État exact en mémoire, tel qu'il sera envoyé à la prochaine fermeture débouncée de l'app.",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            when {
                loading -> Box(modifier = Modifier.padding(16.dp)) {
                    CircularProgressIndicator(color = SecondaryColor)
                }
                snapshot == null -> InfoCard(
                    title = "Aucune donnée",
                    body = "Soit le partage de données est désactivé, soit aucun événement n'a encore été collecté."
                )
                else -> {
                    val s = snapshot!!
                    MetadataCard(state = s)
                    Spacer(Modifier.height(12.dp))
                    EventBreakdownCard(events = s.events)
                    Spacer(Modifier.height(12.dp))
                    RawJsonCard(text = prettyJson.encodeToString(DailyReportState.serializer(), s))
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, top = 8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = SecondaryColor
            )
        }
    }
}

@Composable
private fun MetadataCard(state: DailyReportState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Identité de la journée", color = SecondaryColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            MetadataRow("Identifiant journalier", state.dailyId)
            MetadataRow("Jour local", state.day)
            MetadataRow("Réseau", state.networkCode)
            MetadataRow("Version de l'app", state.appVersion)
            MetadataRow("Schéma", "v${state.schemaVersion}")
            MetadataRow("Dernière modification", state.lastModifiedAt)
            MetadataRow("Sessions de la journée", state.sessions.size.toString())
            MetadataRow("Événements en attente", state.events.size.toString())
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = value,
            color = SecondaryColor,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.5f)
        )
    }
}

@Composable
private fun EventBreakdownCard(events: List<TelemetryEvent>) {
    if (events.isEmpty()) {
        InfoCard(
            title = "Aucun événement aujourd'hui",
            body = "L'application n'a encore rien à signaler. Continuez à utiliser Pelo normalement."
        )
        return
    }
    val countsByKind = events.groupingBy { it::class.simpleName ?: "Unknown" }.eachCount()
        .entries.sortedByDescending { it.value }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Répartition des événements", color = SecondaryColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            countsByKind.forEach { (kind, count) ->
                MetadataRow(kindLabel(kind), count.toString())
            }
        }
    }
}

@Composable
private fun RawJsonCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0C)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Payload brut (JSON)",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = text,
                color = Color(0xFFD0D0D5),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = SecondaryColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(body, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

/**
 * Friendly French label for each [TelemetryEvent] subclass — falls back to the class name if
 * we forget to add a translation for a new event type.
 */
private fun kindLabel(simpleName: String): String = when (simpleName) {
    "SessionOpened" -> "Ouvertures de session"
    "SessionClosed" -> "Fermetures de session"
    "SearchStop" -> "Recherches d'arrêt"
    "SearchLine" -> "Recherches de ligne"
    "SearchItinerary" -> "Recherches d'itinéraire"
    "ItineraryCalculated" -> "Itinéraires calculés"
    "ItineraryChosen" -> "Itinéraires choisis"
    "TripCompleted" -> "Trajets effectués"
    "LineClicked" -> "Clics sur lignes"
    "StopClicked" -> "Clics sur arrêts"
    "AlertSubmitted" -> "Alertes signalées"
    "AlertRead" -> "Alertes officielles lues"
    else -> simpleName
}
