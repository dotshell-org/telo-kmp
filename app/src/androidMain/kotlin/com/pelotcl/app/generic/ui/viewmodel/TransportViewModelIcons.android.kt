package com.pelotcl.app.generic.ui.viewmodel

import android.graphics.Bitmap

/**
 * Vestigial vehicle-marker icon-cache hooks.
 *
 * The bitmap cache was abandoned (these always returned null / false / no-op), but
 * [com.pelotcl.app.generic.utils.map.MapStopsManager] (androidMain, slated for the
 * declarative map rewrite) still calls them. They live here as Android-only extensions
 * so the common [TransportViewModel] stays free of `android.graphics.Bitmap`.
 */
fun TransportViewModel.hasAllIcons(iconNames: Collection<String>): Boolean = false

fun TransportViewModel.getIconBitmap(iconName: String): Bitmap? = null

fun TransportViewModel.cacheIconBitmap(iconName: String, bitmap: Bitmap) {
    // No-op: bitmap icon caching is not used.
}
