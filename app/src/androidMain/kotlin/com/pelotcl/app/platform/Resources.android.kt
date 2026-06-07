package com.pelotcl.app.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

actual class DrawableProvider actual constructor(context: PlatformContext) {
    private val res = context.resources
    private val pkg = context.packageName

    @Composable
    actual fun getPainter(name: String): Painter {
        return painterResource(id = resolveId(name))
    }

    actual fun hasDrawable(name: String): Boolean {
        return resolveId(name) != 0
    }

    private fun resolveId(name: String): Int {
        return res.getIdentifier(name, "drawable", pkg)
    }
}

actual class StringProvider actual constructor(context: PlatformContext) {
    private val res = context.resources
    private val pkg = context.packageName

    actual operator fun get(name: String): String {
        val id = res.getIdentifier(name, "string", pkg)
        return res.getString(id)
    }
}
