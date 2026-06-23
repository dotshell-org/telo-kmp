package eu.dotshell.pelo.generic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Gray900,
    onPrimary = SecondaryColor,
    primaryContainer = Gray100,
    onPrimaryContainer = Gray900,
    secondary = Gray700,
    onSecondary = SecondaryColor,
    secondaryContainer = Gray200,
    onSecondaryContainer = Gray900,
    background = SecondaryColor,
    onBackground = Gray900,
    surface = SecondaryColor,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    outline = Gray400,
)

private val DarkColorScheme = darkColorScheme(
    primary = Gray100,
    onPrimary = Gray900,
    primaryContainer = Gray800,
    onPrimaryContainer = Gray100,
    secondary = Gray300,
    onSecondary = Gray900,
    secondaryContainer = Gray700,
    onSecondaryContainer = Gray100,
    background = Gray950,
    onBackground = Gray100,
    surface = Gray900,
    onSurface = Gray100,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray300,
    outline = Gray500,
)

@Composable
fun PeloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
