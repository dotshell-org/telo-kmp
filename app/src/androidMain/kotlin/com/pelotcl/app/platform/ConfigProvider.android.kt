package com.pelotcl.app.platform

import com.pelotcl.app.generic.data.config.AppConfigLoader
import com.pelotcl.app.generic.data.config.LineColorsData

actual fun provideLineColors(): LineColorsData {
    return AppConfigLoader.getConfig().lineColors
}
