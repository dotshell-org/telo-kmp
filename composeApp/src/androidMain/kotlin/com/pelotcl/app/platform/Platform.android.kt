package com.pelotcl.app.platform

import android.content.Context
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual typealias PlatformContext = Context

actual fun createHttpClientEngine(): HttpClientEngineFactory<*> = OkHttp
