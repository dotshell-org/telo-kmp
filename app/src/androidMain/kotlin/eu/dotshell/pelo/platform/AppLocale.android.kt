package eu.dotshell.pelo.platform

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Overrides the configuration locale that Compose Resources reads, so the chosen language takes
 * effect immediately (no restart). An empty tag leaves the system language untouched.
 */
@Composable
actual fun ProvideAppLocale(languageTag: String, content: @Composable () -> Unit) {
    if (languageTag.isEmpty()) {
        content()
        return
    }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val (localizedConfig, localizedContext) = remember(languageTag, configuration) {
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)
        val config = Configuration(configuration).apply { setLocale(locale) }
        config to context.createConfigurationContext(config)
    }
    key(languageTag) {
        CompositionLocalProvider(
            LocalConfiguration provides localizedConfig,
            LocalContext provides localizedContext,
        ) {
            content()
        }
    }
}
