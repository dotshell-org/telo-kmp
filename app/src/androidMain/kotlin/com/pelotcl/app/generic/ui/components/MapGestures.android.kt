package com.pelotcl.app.generic.ui.components

import org.maplibre.compose.map.GestureOptions

actual fun mapGestureOptions(interactive: Boolean): GestureOptions = GestureOptions(
    isRotateEnabled = false,
    isScrollEnabled = interactive,
    isTiltEnabled = interactive,
    isZoomEnabled = interactive,
    isDoubleTapEnabled = interactive,
    isQuickZoomEnabled = interactive
)
