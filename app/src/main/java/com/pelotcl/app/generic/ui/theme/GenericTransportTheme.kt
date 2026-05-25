package com.pelotcl.app.generic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pelotcl.app.generic.data.config.ThemeData

class GenericTransportTheme(private val data: ThemeData? = null) : TransportTheme {
    override val metroLineColor: Color = Color(android.graphics.Color.parseColor(data?.metroLineColor ?: "#FF0000"))
    override val tramLineColor: Color = Color(android.graphics.Color.parseColor(data?.tramLineColor ?: "#00FF00"))
    override val busLineColor: Color = Color(android.graphics.Color.parseColor(data?.busLineColor ?: "#0000FF"))
    override val errorColor: Color = Color(android.graphics.Color.parseColor(data?.errorColor ?: "#FF0000"))
    override val successColor: Color = Color(android.graphics.Color.parseColor(data?.successColor ?: "#00FF00"))
    override val warningColor: Color = Color(android.graphics.Color.parseColor(data?.warningColor ?: "#FFFF00"))
    override val disruptionColor: Color = Color(android.graphics.Color.parseColor(data?.disruptionColor ?: "#FFA500"))

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
            typography = Typography(
                displayLarge = Typography().displayLarge.copy(color = PrimaryColor),
                displayMedium = Typography().displayMedium.copy(color = PrimaryColor),
                displaySmall = Typography().displaySmall.copy(color = PrimaryColor),
                headlineLarge = Typography().headlineLarge.copy(color = PrimaryColor),
                headlineMedium = Typography().headlineMedium.copy(color = PrimaryColor),
                headlineSmall = Typography().headlineSmall.copy(color = PrimaryColor),
                titleLarge = Typography().titleLarge.copy(color = PrimaryColor),
                titleMedium = Typography().titleMedium.copy(color = PrimaryColor),
                titleSmall = Typography().titleSmall.copy(color = PrimaryColor),
                bodyLarge = Typography().bodyLarge.copy(color = PrimaryColor),
                bodyMedium = Typography().bodyMedium.copy(color = PrimaryColor),
                bodySmall = Typography().bodySmall.copy(color = PrimaryColor),
                labelLarge = Typography().labelLarge.copy(color = PrimaryColor),
                labelMedium = Typography().labelMedium.copy(color = PrimaryColor),
                labelSmall = Typography().labelSmall.copy(color = PrimaryColor)
            ),
            content = content
        )
    }
}
