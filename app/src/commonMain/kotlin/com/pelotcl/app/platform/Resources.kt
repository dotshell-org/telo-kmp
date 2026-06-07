package com.pelotcl.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

expect class DrawableProvider(context: PlatformContext) {
    @Composable
    fun getPainter(name: String): Painter
    fun hasDrawable(name: String): Boolean
}

expect class StringProvider(context: PlatformContext) {
    operator fun get(name: String): String
}
