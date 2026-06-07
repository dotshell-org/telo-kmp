package com.pelotcl.app.generic.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.pelotcl.app.generic.data.config.TelemetryFaqEntryData
import com.pelotcl.app.generic.ui.theme.PrimaryColor
import com.pelotcl.app.generic.ui.theme.SecondaryColor

/**
 * Static FAQ screen rendered from the `telemetry.disclosure.faq` block of `config.yml`. Driven
 * entirely by configuration so a different white-labeled network can ship its own copy
 * without code changes.
 */
@Composable
fun TelemetryFaqScreen(
    entries: List<TelemetryFaqEntryData>,
    onBackClick: () -> Unit,
    onSystemBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onSystemBack() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PrimaryColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Questions fréquentes",
                color = SecondaryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Tout ce que vous voulez savoir sur ce qui est collecté, à qui ça va, et comment l'arrêter.",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (entries.isEmpty()) {
                Text(
                    text = "Aucune entrée configurée pour le moment.",
                    color = SecondaryColor,
                    fontSize = 14.sp
                )
            }

            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(
                        color = Color(0xFF3A3A3C),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                Text(
                    text = entry.question,
                    color = SecondaryColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = entry.answer.trim(),
                    color = SecondaryColor.copy(alpha = 0.88f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(48.dp))
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
