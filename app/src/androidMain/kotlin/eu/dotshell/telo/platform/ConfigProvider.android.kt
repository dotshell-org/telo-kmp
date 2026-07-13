package eu.dotshell.telo.platform

import eu.dotshell.telo.generic.data.config.AppConfigLoader
import eu.dotshell.telo.generic.data.config.LineColorsData
import eu.dotshell.telo.generic.data.network.mapstyle.MapStyleConfig
import eu.dotshell.telo.generic.data.network.transport.TransportLineRules
import eu.dotshell.telo.generic.service.TransportServiceProvider

actual fun provideLineColors(): LineColorsData {
    return AppConfigLoader.getConfig().lineColors
}

actual fun provideTransportLineRules(): TransportLineRules {
    return TransportServiceProvider.getTransportLineRules()
}

actual fun provideMapStyleConfig(): MapStyleConfig {
    return TransportServiceProvider.getMapStyleConfig()
}
