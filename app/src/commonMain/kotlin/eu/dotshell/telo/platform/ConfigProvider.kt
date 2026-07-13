package eu.dotshell.telo.platform

import eu.dotshell.telo.generic.data.config.LineColorsData
import eu.dotshell.telo.generic.data.network.mapstyle.MapStyleConfig
import eu.dotshell.telo.generic.data.network.transport.TransportLineRules

expect fun provideLineColors(): LineColorsData
expect fun provideTransportLineRules(): TransportLineRules
expect fun provideMapStyleConfig(): MapStyleConfig
