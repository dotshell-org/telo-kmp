package com.pelotcl.app.generic.data.telemetry

import com.pelotcl.app.generic.data.local_history.LocalHistoryStorage

/**
 * iOS stub: telemetry is not yet wired on iOS (no WorkManager equivalent).
 * Emitting is a no-op and there is no local-history backing store.
 */
actual object TelemetryEmitter {
    actual fun emit(event: TelemetryEvent) {
        // no-op on iOS for now
    }

    actual fun localHistory(): LocalHistoryStorage? = null
}
