package com.pelotcl.app.platform

/**
 * Platform context abstraction.
 * On Android this wraps android.content.Context.
 * On iOS this is a no-op object.
 */
expect abstract class PlatformContext

/**
 * Provides the platform-specific HTTP client engine factory.
 */
expect fun createHttpClientEngine(): io.ktor.client.engine.HttpClientEngineFactory<*>
