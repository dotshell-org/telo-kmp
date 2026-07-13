package eu.dotshell.telo.platform

import androidx.compose.runtime.compositionLocalOf

val LocalPlatformContext = compositionLocalOf<PlatformContext> {
    error("PlatformContext not provided")
}
