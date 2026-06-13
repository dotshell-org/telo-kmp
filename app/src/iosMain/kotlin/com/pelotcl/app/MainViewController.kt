package com.pelotcl.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.pelotcl.app.generic.data.models.geojson.FeatureCollection
import com.pelotcl.app.generic.data.models.geojson.StopCollection
import com.pelotcl.app.generic.data.repository.offline.mapstyle.MapStyleCompat
import com.pelotcl.app.generic.service.TransportServiceProvider
import com.pelotcl.app.generic.ui.components.MapCanvas
import com.pelotcl.app.generic.ui.theme.PeloTheme
import com.pelotcl.app.generic.ui.viewmodel.TransportLinesUiState
import com.pelotcl.app.generic.ui.viewmodel.TransportStopsUiState
import com.pelotcl.app.generic.ui.viewmodel.TransportViewModel
import com.pelotcl.app.platform.LocalPlatformContext
import com.pelotcl.app.platform.Log
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
 * Shared root composable.
 *
 * §9.2: a full-screen [MapCanvas] fed with real transport lines/stops from the shared
 * [TransportViewModel]. This also exercises the iOS platform actuals at runtime (config +
 * asset loading via FileSystem, Ktor-Darwin networking). `TransportServiceProvider.initialize`
 * replaces the Android `PeloApplication.onCreate` bootstrap (there is no Application on iOS).
 *
 * The full PlanScreen UI (search, sheets, navigation) is promoted here once §9.3–§9.5 free
 * PlanScreen from androidMain.
 */
@Composable
fun App() {
    PeloTheme {
        val viewModel = remember {
            try {
                TransportServiceProvider.initialize(IosPlatformContext)
                TransportViewModel(IosPlatformContext)
            } catch (t: Throwable) {
                Log.e("iosApp", "Transport data init failed: ${t.message}")
                null
            }
        }

        if (viewModel != null) {
            MapWithData(viewModel)
        } else {
            // Data bootstrap failed (e.g. assets not found): still show the base map.
            MapCanvas(
                modifier = Modifier.fillMaxSize(),
                styleUrl = MapStyleCompat.POSITRON.styleUrl,
            )
        }
    }
}

@Composable
private fun MapWithData(viewModel: TransportViewModel) {
    val linesState by viewModel.uiState.collectAsState()
    val stopsState by viewModel.stopsUiState.collectAsState()

    val lines = when (val s = linesState) {
        is TransportLinesUiState.Success -> s.lines
        is TransportLinesUiState.PartialSuccess -> s.lines
        else -> null
    }
    val stops = (stopsState as? TransportStopsUiState.Success)?.stops

    MapCanvas(
        modifier = Modifier.fillMaxSize(),
        styleUrl = MapStyleCompat.POSITRON.styleUrl,
        initialLatitude = 45.75,
        initialLongitude = 4.85,
        initialZoom = 12.0,
        lines = lines?.let { FeatureCollection(features = it) },
        stops = stops?.let { StopCollection(features = it) },
    )
}
