package eu.dotshell.pelo.generic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import eu.dotshell.pelo.generic.data.config.ThemeData
import eu.dotshell.pelo.platform.parseComposeColor

private val BaseTypography = Typography()

private val AppTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(color = PrimaryColor),
    displayMedium = BaseTypography.displayMedium.copy(color = PrimaryColor),
    displaySmall = BaseTypography.displaySmall.copy(color = PrimaryColor),
    headlineLarge = BaseTypography.headlineLarge.copy(color = PrimaryColor),
    headlineMedium = BaseTypography.headlineMedium.copy(color = PrimaryColor),
    headlineSmall = BaseTypography.headlineSmall.copy(color = PrimaryColor),
    titleLarge = BaseTypography.titleLarge.copy(color = PrimaryColor),
    titleMedium = BaseTypography.titleMedium.copy(color = PrimaryColor),
    titleSmall = BaseTypography.titleSmall.copy(color = PrimaryColor),
    bodyLarge = BaseTypography.bodyLarge.copy(color = PrimaryColor),
    bodyMedium = BaseTypography.bodyMedium.copy(color = PrimaryColor),
    bodySmall = BaseTypography.bodySmall.copy(color = PrimaryColor),
    labelLarge = BaseTypography.labelLarge.copy(color = PrimaryColor),
    labelMedium = BaseTypography.labelMedium.copy(color = PrimaryColor),
    labelSmall = BaseTypography.labelSmall.copy(color = PrimaryColor)
)

class GenericTransportTheme(private val data: ThemeData? = null) : TransportTheme {
    override val metroLineColor: Color = parseComposeColor(data?.metroLineColor, Color(0xFFFF0000))
    override val tramLineColor: Color = parseComposeColor(data?.tramLineColor, Color(0xFF00FF00))
    override val busLineColor: Color = parseComposeColor(data?.busLineColor, Color(0xFF0000FF))
    override val errorColor: Color = parseComposeColor(data?.errorColor, Color(0xFFFF0000))
    override val successColor: Color = parseComposeColor(data?.successColor, Color(0xFF00FF00))
    override val warningColor: Color = parseComposeColor(data?.warningColor, Color(0xFFFFFF00))
    override val disruptionColor: Color = parseComposeColor(data?.disruptionColor, Color(0xFFFFA500))
    override val linkColor: Color = parseComposeColor(data?.linkColor, Color(0xFF4285F4))

    @Composable
    override fun ApplyTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = PrimaryColor,
                secondary = SecondaryColor,
                tertiary = AccentColor,
                background = SecondaryColor,
                surface = SecondaryColor,
                onPrimary = SecondaryColor,
                onSecondary = SecondaryColor,
                onBackground = PrimaryColor,
                onSurface = PrimaryColor,
                error = errorColor,
                onError = SecondaryColor
            ),
            typography = AppTypography,
            content = content
        )
    }
}
