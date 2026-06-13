package com.pelotcl.app.platform

import com.pelotcl.app.generic.data.config.AppConfigLoader
import com.pelotcl.app.generic.data.config.LineColorsData
import com.pelotcl.app.generic.data.network.mapstyle.MapStyleConfig
import com.pelotcl.app.generic.data.network.transport.TransportLineRules
import com.pelotcl.app.generic.service.TransportServiceProvider

// Same as the Android actuals: everything is loaded from config.json via the common
// AppConfigLoader / TransportServiceProvider (initialized at startup). The previous iOS
// stubs threw "Config not available on iOS".
actual fun provideLineColors(): LineColorsData =
    AppConfigLoader.getConfig().lineColors

actual fun provideTransportLineRules(): TransportLineRules =
    TransportServiceProvider.getTransportLineRules()

actual fun provideMapStyleConfig(): MapStyleConfig =
    TransportServiceProvider.getMapStyleConfig()
