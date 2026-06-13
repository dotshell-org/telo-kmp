package com.pelotcl.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.pelotcl.app.generic.data.repository.offline.mapstyle.MapStyleCompat
import com.pelotcl.app.generic.ui.components.MapCanvas
import com.pelotcl.app.generic.ui.theme.PeloTheme
import com.pelotcl.app.platform.LocalPlatformContext
import com.pelotcl.app.platform.PlatformContext
import platform.UIKit.UIViewController

/**
 * iOS no-op platform context. On Android, PlatformContext is android.content.Context;
 * on iOS the platform actuals (FileSystem, Settings, …) don't need a real context, so a
 * single shared instance is enough. PlatformContext is `abstract` (to match the Android
 * typealias to the abstract android.content.Context), hence this concrete singleton.
 */
object IosPlatformContext : PlatformContext()

/**
 * Compose entry point, exported to Swift as `ComposeAppKt.MainViewController()`.
 * The iosApp Xcode target wraps this UIViewController in SwiftUI.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    CompositionLocalProvider(LocalPlatformContext provides IosPlatformContext) {
        App()
    }
}

/**
 * Shared root composable. Minimal for now (proves the framework + ComposeUIViewController +
 * common UI/theme + a platform actual all work on-device). The full shared UI (PlanScreen,
 * navigation) lands once the map swap (§9) frees PlanScreen from androidMain.
 */
@Composable
fun App() {
    PeloTheme {
        // §9.1 — prove the declarative maplibre-compose map renders on iOS (Metal).
        // Real transport data (lines/stops) + the full PlanScreen UI come next.
        MapCanvas(
            modifier = Modifier.fillMaxSize(),
            styleUrl = MapStyleCompat.POSITRON.styleUrl,
            initialLatitude = 45.75,
            initialLongitude = 4.85,
            initialZoom = 12.0,
        )
    }
}
