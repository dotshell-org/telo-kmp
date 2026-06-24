package eu.dotshell.pelo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import eu.dotshell.pelo.generic.data.models.geojson.StopFeature
import eu.dotshell.pelo.generic.data.repository.itinerary.itinerary.RaptorRepository
import eu.dotshell.pelo.generic.service.NavigationModeForegroundService
import eu.dotshell.pelo.generic.service.NavigationModeStateStore
import eu.dotshell.pelo.generic.ui.components.favorites.AddFavoriteDialog
import eu.dotshell.pelo.generic.ui.components.favorites.FavoritesBar
import eu.dotshell.pelo.generic.ui.components.search.TransportSearchBar
import eu.dotshell.pelo.generic.data.models.search.TransportSearchContent
import eu.dotshell.pelo.generic.data.models.search.StationSearchResult
import eu.dotshell.pelo.generic.data.repository.offline.search.SearchHistoryItem
import eu.dotshell.pelo.generic.data.repository.offline.search.SearchType
import eu.dotshell.pelo.generic.data.repository.offline.mapstyle.MapStyleCompat
import eu.dotshell.pelo.generic.ui.screens.onboarding.NotificationPermissionGate
import eu.dotshell.pelo.generic.ui.screens.settings.about.AboutScreen
import eu.dotshell.pelo.generic.ui.screens.settings.about.ContactScreen
import eu.dotshell.pelo.generic.ui.screens.settings.about.CreditsScreen
import eu.dotshell.pelo.generic.ui.screens.settings.about.LegalScreen

import eu.dotshell.pelo.generic.ui.screens.settings.ItinerarySettingsScreen
import eu.dotshell.pelo.generic.ui.screens.settings.OfflineSettingsScreen
import eu.dotshell.pelo.generic.ui.screens.settings.SettingsScreen
import eu.dotshell.pelo.generic.ui.screens.settings.TelemetrySettingsScreen
import eu.dotshell.pelo.generic.ui.theme.PeloTheme
import eu.dotshell.pelo.generic.ui.theme.AccentColor
import eu.dotshell.pelo.generic.ui.viewmodel.TransportViewModel
import eu.dotshell.pelo.generic.ui.viewmodel.TransportStopsUiState
import eu.dotshell.pelo.generic.data.repository.offline.SchedulesRepository
import eu.dotshell.pelo.generic.ui.screens.Destination
import eu.dotshell.pelo.generic.ui.theme.PrimaryColor
import eu.dotshell.pelo.generic.ui.theme.SecondaryColor
import eu.dotshell.pelo.generic.data.cache.TransportCacheImpl
import eu.dotshell.pelo.generic.utils.location.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

class MainActivity : ComponentActivity() {

    // Application-level coroutine scope for early background work
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isNavigationModeEnabled = false
    private var hasAppliedFirstNavigationCallback = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore lockscreen behavior when navigation is still active in foreground service.
        if (NavigationModeStateStore.isNavigationActive(this)) {
            isNavigationModeEnabled = true
            setNavigationLockScreenBehavior(true)
        }

        // Preload disk cache and binary schedule data in parallel BEFORE UI (critical for first render)
        // Retrofit initialization is deferred to after setContent
        appScope.launch {
            try {
                // Parallel cache and schedule-data warmup - these are needed for initial UI
                val cacheJob = launch {
                    val cache = TransportCacheImpl(applicationContext)
                    cache.preloadFromDisk()
                }
                val schedulesJob = launch {
                    val schedulesRepo =
                        SchedulesRepository.getInstance(applicationContext)
                    schedulesRepo.warmupDatabase()
                }
                // Wait for cache and schedule data warmup (critical for UI)
                cacheJob.join()
                schedulesJob.join()
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Startup preload failed: ${e.message}")
            }
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            CompositionLocalProvider(eu.dotshell.pelo.platform.LocalPlatformContext provides this@MainActivity) {
                NotificationPermissionGate {
                    App(
                        onNavigationModeChanged = { active ->
                            if (!hasAppliedFirstNavigationCallback) {
                                hasAppliedFirstNavigationCallback = true
                                // Ignore initial Compose state restoration mismatch when service is still active.
                                if (isNavigationModeEnabled && !active) return@App
                            }
                            if (active == isNavigationModeEnabled) return@App
                            isNavigationModeEnabled = active
                            if (active) {
                                startNavigationForegroundService()
                            } else {
                                stopNavigationForegroundService()
                            }
                            setNavigationLockScreenBehavior(active)
                        }
                    )
                }
            }
        }

        // Deferred initialization - run AFTER setContent to not block first frame
        // These are not needed for initial UI display
        appScope.launch {
            // TransportServiceProvider + RetrofitInstance are initialized in PeloApplication.onCreate
            // so background workers and repositories can run before the first activity starts.

            // Preload Raptor library in background (only needed for itinerary calculations)
            // yield() gives the UI thread priority without an arbitrary delay
            kotlinx.coroutines.yield()
            try {
                val raptorRepo =
                    RaptorRepository.getInstance(
                        applicationContext
                    )
                raptorRepo.initialize()
                raptorRepo.preloadJourneyCache()
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Raptor preload failed: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Keep navigation service running when activity/task is closed.
        if (!isNavigationModeEnabled) {
            setNavigationLockScreenBehavior(false)
        }
    }

    private fun startNavigationForegroundService() {
        val serviceIntent = Intent(this, NavigationModeForegroundService::class.java).apply {
            action = NavigationModeForegroundService.ACTION_START
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopNavigationForegroundService() {
        val serviceIntent = Intent(this, NavigationModeForegroundService::class.java).apply {
            action = NavigationModeForegroundService.ACTION_STOP
        }
        startService(serviceIntent)
    }

    private fun setNavigationLockScreenBehavior(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(enabled)
            setTurnScreenOn(enabled)
            if (enabled) {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
        } else {
            @Suppress("DEPRECATION")
            if (enabled) {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
        }
    }
}

