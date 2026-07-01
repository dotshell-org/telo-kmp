package eu.dotshell.pelo.generic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.dotshell.pelo.generic.ui.theme.AccentColor
import eu.dotshell.pelo.generic.ui.theme.PrimaryColor
import eu.dotshell.pelo.generic.ui.theme.SecondaryColor
import eu.dotshell.pelo.platform.AppLanguage
import eu.dotshell.pelo.platform.LanguageManager
import eu.dotshell.pelo.platform.LocalPlatformContext
import eu.dotshell.pelo.platform.StringProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = StringProvider(LocalPlatformContext.current)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings["language_title"],
                        color = SecondaryColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings["back"],
                            tint = SecondaryColor,
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C1C1E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    val languages = AppLanguage.entries
                    languages.forEachIndexed { index, lang ->
                        val label = if (lang == AppLanguage.SYSTEM) {
                            strings["language_system"]
                        } else {
                            lang.nativeName
                        }
                        val isSelected = LanguageManager.current == lang

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    LanguageManager.set(lang)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 0.dp else 2.dp,
                                        color = if (isSelected) Color.Transparent else Color(0xFF8E8E93),
                                        shape = CircleShape
                                    )
                                    .background(
                                        if (isSelected) AccentColor else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = strings["selected"],
                                        tint = SecondaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = label,
                                color = SecondaryColor,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }

                        if (index < languages.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 56.dp)
                                    .height(0.5.dp)
                                    .background(Color(0xFF3A3A3C))
                            )
                        }
                    }
                }
            }
        }
    }
}
