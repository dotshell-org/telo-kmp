package com.pelotcl.app.generic.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Interface for the transport theme
 * Each city must provide its own implementation
 */
interface TransportTheme {

    /**
     * Color for metro lines
     */
    val metroLineColor: Color

    /**
     * Color for tram lines
     */
    val tramLineColor: Color

    /**
     * Color for bus lines
     */
    val busLineColor: Color

    /**
     * Error color
     */
    val errorColor: Color

    /**
     * Success color
     */
    val successColor: Color

    /**
     * Warning color
     */
    val warningColor: Color

    /**
     * Disruption color
     */
    val disruptionColor: Color

    /**
     * Color used for clickable links (defaults to #4285F4 when no config provided)
     */
    val linkColor: Color

    /**
     * Applies the theme to the composition
     */
    @Composable
    fun ApplyTheme(content: @Composable () -> Unit)
}