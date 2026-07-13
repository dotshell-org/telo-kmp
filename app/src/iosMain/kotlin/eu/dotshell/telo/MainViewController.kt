package eu.dotshell.telo

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import eu.dotshell.telo.generic.data.config.AppConfigLoader
import eu.dotshell.telo.generic.data.telemetry.TelemetryService
import eu.dotshell.telo.generic.service.TransportServiceProvider
import eu.dotshell.telo.generic.utils.location.LocationPermissionManager
import eu.dotshell.telo.platform.BackgroundScheduler
import eu.dotshell.telo.platform.LanguageManager
import eu.dotshell.telo.platform.LocalPlatformContext
import eu.dotshell.telo.platform.Log
import eu.dotshell.telo.platform.ProvideAppLocale
import eu.dotshell.telo.platform.PlatformContext
import platform.UIKit.UIViewController

/**
 * iOS no-op platform context. On Android, PlatformContext is android.content.Context; on iOS the
 * platform actuals (FileSystem, Settings, LocationProvider, …) don't need a real context, so a
 * single shared instance is enough. PlatformContext is `abstract` (to match the Android typealias
 * to the abstract android.content.Context), hence this concrete singleton.
 */
object IosPlatformContext : PlatformContext()

/**
 * Compose entry point, exported to Swift as `ComposeAppKt.MainViewController()`. Provides the iOS
 * [PlatformContext] and hosts the shared [App] (commonMain). The iosApp Xcode target wraps this
 * UIViewController in SwiftUI.
 */
fun MainViewController(): UIViewController {
    TransportServiceProvider.initialize(IosPlatformContext)
    try {
        val telemetryConfig = AppConfigLoader.getConfig().telemetry
        if (telemetryConfig != null) {
            TelemetryService.initialize(IosPlatformContext, telemetryConfig)
        }
        if (TransportServiceProvider.getRealtimeConfig().trafficAlertsEnabled) {
            BackgroundScheduler(IosPlatformContext).ensureTrafficAlertsScheduled()
        }
    } catch (e: Exception) {
        Log.w("MainViewController", "Failed to initialize Telemetry: ${e.message}")
    }
    LanguageManager.init(IosPlatformContext)

    return ComposeUIViewController {
        CompositionLocalProvider(LocalPlatformContext provides IosPlatformContext) {
            ProvideAppLocale(LanguageManager.current.tag) {
            App(
                onNavigationModeChanged = { active ->
                    if (active) {
                        // Request always authorization for navigation to enable background updates
                        LocationPermissionManager.requestNavigationPermissions(IosPlatformContext)
                    }
                }
            )
            }
        }
    }
}
