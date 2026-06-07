package com.pelotcl.app.platform

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual class PlatformContext

actual fun createHttpClientEngine(): HttpClientEngineFactory<*> = Darwin
