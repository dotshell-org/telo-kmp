package eu.dotshell.pelo.generic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.dotshell.pelo.generic.ui.theme.PrimaryColor
import eu.dotshell.pelo.generic.ui.theme.SecondaryColor


@Composable
fun TelemetrySettingsScreen(
    onBackClick: () -> Unit,
    onShowCollectedData: () -> Unit,
    onWipeHistory: () -> Unit,
    onLegalClick: () -> Unit,
    onFaqClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PrimaryColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 80.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Confidentialité",
                color = SecondaryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            TelemetryMenuRow(
                title = "Voir les données collectées aujourd'hui",
                subtitle = "Transparence : ce que partage l'application",
                onClick = onShowCollectedData
            )
            HorizontalDivider(color = Color(0xFF3A3A3C))

            TelemetryMenuRow(
                title = "Supprimer mon historique local",
                subtitle = "Trajets et historique des favoris (non envoyés au serveur)",
                onClick = onWipeHistory
            )
            HorizontalDivider(color = Color(0xFF3A3A3C))

            TelemetryMenuRow(
                title = "Questions fréquentes",
                subtitle = "Qui reçoit les données, ce que ça implique, comment l'arrêter",
                onClick = onFaqClick
            )
            HorizontalDivider(color = Color(0xFF3A3A3C))

            TelemetryMenuRow(
                title = "Mentions légales / CGU",
                onClick = onLegalClick
            )

            Spacer(modifier = Modifier.height(24.dp))
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
private fun TelemetryMenuRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PrimaryColor),
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
                    text = title,
                    color = SecondaryColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = SecondaryColor.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
