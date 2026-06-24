package eu.dotshell.pelo.generic.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.dotshell.pelo.generic.data.telemetry.DailyReportState
import eu.dotshell.pelo.generic.data.telemetry.TelemetryEvent
import eu.dotshell.pelo.generic.ui.theme.PrimaryColor
import eu.dotshell.pelo.generic.ui.theme.SecondaryColor
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetrySettingsScreen(
    snapshot: DailyReportState?,
    onBackClick: () -> Unit,
    onWipeHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prettyJson = remember {
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Confidentialité",
                        color = SecondaryColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = SecondaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryColor
                )
            )
        },
        containerColor = PrimaryColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Wipe history button at the top
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onWipeHistory),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Supprimer mon historique local",
                            color = Color(0xFFEF4444),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Trajets et favoris (stockés uniquement sur votre appareil)",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF3A3A3C))
            Spacer(Modifier.height(20.dp))

            Text(
                text = "Données collectées aujourd'hui",
                color = SecondaryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Text(
                text = "État exact en mémoire, tel qu'il sera envoyé à la prochaine fermeture de l'app.",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            when (snapshot) {
                null -> InfoCard(
                    title = "Aucune donnée collectée aujourd'hui",
                    body = "Soit le partage de données anonymes est désactivé, soit aucun événement n'a encore été enregistré."
                )
                else -> {
                    MetadataCard(state = snapshot)
                    Spacer(Modifier.height(12.dp))
                    EventBreakdownCard(events = snapshot.events)
                    Spacer(Modifier.height(12.dp))
                    RawJsonCard(text = prettyJson.encodeToString(DailyReportState.serializer(), snapshot))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
