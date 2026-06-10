package com.pelotcl.app.generic.data.telemetry

import com.pelotcl.app.generic.data.local_history.LocalHistoryStorage

/**
 * Process-wide telemetry entry point shared across platforms.
 *
 * commonMain only needs the two members declared here ([emit] for fire-and-forget
 * events, [localHistory] for the local-only audit log). Each platform's `actual`
 * provides the full implementation (Android wires WorkManager/opt-in/storage; iOS
 * is currently a no-op stub).
 */
expect object TelemetryEmitter {
    fun emit(event: TelemetryEvent)
    fun localHistory(): LocalHistoryStorage?
}
