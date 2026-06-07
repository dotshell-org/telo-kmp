package com.pelotcl.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

actual class DrawableProvider actual constructor(context: PlatformContext) {
    @Composable
    actual fun getPainter(name: String): Painter {
        throw UnsupportedOperationException("DrawableProvider not implemented on iOS")
    }

    actual fun hasDrawable(name: String): Boolean {
        throw UnsupportedOperationException("DrawableProvider not implemented on iOS")
    }
}

actual class StringProvider actual constructor(context: PlatformContext) {
    actual operator fun get(name: String): String {
        throw UnsupportedOperationException("StringProvider not implemented on iOS")
    }
}
