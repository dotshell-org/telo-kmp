package com.pelotcl.app.platform

import com.pelotcl.app.generic.data.config.LineColorsData

actual fun provideLineColors(): LineColorsData {
    throw UnsupportedOperationException("Config not available on iOS")
}
