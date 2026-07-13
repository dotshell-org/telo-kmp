package eu.dotshell.telo.generic.data.repository.itinerary.itinerary

import eu.dotshell.telo.platform.PlatformContext
import eu.dotshell.telo.platform.Settings

/**
 * Repository for managing itinerary routing preferences.
 * Stores user preferences for route filtering (BN navette lines).
 * Multiplatform: uses [Settings] abstraction instead of SharedPreferences.
 */
class ItineraryPreferencesRepository(context: PlatformContext) {
    private val settings = Settings(context, "telo_itinerary_prefs")

    private val keyEnableNavetteLines = "enable_navette_lines"

    /**
     * Check if the BN navette lines (Vauban ↔ Milhaud) should be included in routing.
     * Default: true (enabled)
     */
    fun isNavetteLinesEnabled(): Boolean = isOptionEnabled(keyEnableNavetteLines, true)

    /**
     * Generic getter for itinerary options.
     */
    fun isOptionEnabled(key: String, defaultValue: Boolean): Boolean {
        if (!settings.contains(key)) {
            settings.putBoolean(key, defaultValue)
        }
        return settings.getBoolean(key, defaultValue)
    }

    /**
     * Generic setter for itinerary options.
     */
    fun setOptionEnabled(key: String, enabled: Boolean) {
        settings.putBoolean(key, enabled)
    }
}