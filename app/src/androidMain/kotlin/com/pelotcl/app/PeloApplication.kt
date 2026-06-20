package com.pelotcl.app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.pelotcl.app.generic.data.cache.journey.JourneyCache
import com.pelotcl.app.generic.data.config.AppConfigLoader
import com.pelotcl.app.generic.data.repository.itinerary.itinerary.RaptorRepository
import com.pelotcl.app.generic.data.repository.offline.SchedulesRepository
import com.pelotcl.app.generic.data.telemetry.TelemetryService
import com.pelotcl.app.generic.service.TransportServiceProvider
import com.pelotcl.app.platform.BackgroundScheduler

class PeloApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "PeloApplication"
    }

    // On-demand WorkManager initialization (replaces automatic ContentProvider init)
    // This defers non-critical startup work until WorkManager is first accessed.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "PeloApplication onCreate()")
        
        // Verify Raptor assets are available at startup
        verifyRaptorAssets()
        
        TransportServiceProvider.initialize(this)
        BackgroundScheduler(this).ensureTrafficAlertsScheduled()
        initializeTelemetry()
    }

    private fun initializeTelemetry() {
        try {
            val telemetryConfig = AppConfigLoader.getConfig().telemetry ?: return
            TelemetryService.initialize(this, telemetryConfig)
        } catch (e: Exception) {
            Log.w(TAG, "Telemetry init skipped", e)
        }
    }

    /**
     * Verify that all required Raptor assets are present
     * Logs critical errors if assets are missing
     */
    private fun verifyRaptorAssets() {
        try {
            val raptorRepository = RaptorRepository.getInstance(this)
            val assetsAvailable = raptorRepository.checkAssetsAvailable()
            
            if (!assetsAvailable) {
                Log.e(TAG, "CRITICAL: Raptor assets are missing!")
                Log.e(TAG, "This will cause bus stops to disappear from search and map functionality.")
                Log.e(TAG, "Required assets: holidays.json, raptor/stops_*.bin, raptor/routes_*.bin")
                Log.e(TAG, "Please try: File > Invalidate Caches / Restart in Android Studio")
                Log.e(TAG, "Then clean build: ./gradlew clean assembleDebug")
            } else {
                Log.i(TAG, "All Raptor assets verified successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Raptor assets: ${e.message}", e)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND) {
            Log.i(TAG, "onTrimMemory level=$level, trimming caches")
            SchedulesRepository.trimCaches(level)
            try {
                JourneyCache.getInstance(this).trimMemory(level)
            } catch (_: Exception) {
                // JourneyCache may not be initialized yet
            }
        }
    }
}
