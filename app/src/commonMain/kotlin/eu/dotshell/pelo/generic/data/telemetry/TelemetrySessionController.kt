package eu.dotshell.pelo.generic.data.telemetry

import eu.dotshell.pelo.platform.BackgroundScheduler
import kotlin.concurrent.Volatile

/**
 * Platform-agnostic session debounce orchestration for telemetry.
 *
 * A platform observer (Android: `ProcessLifecycleOwner` via `TelemetryService`) forwards
 * app-wide foreground/background transitions here:
 *  - [onForeground] opens a session and cancels any pending close-triggered upload — this is
 *    the "user came back within the debounce window" case.
 *  - [onBackground] closes the session and schedules a debounced upload via [BackgroundScheduler].
 *
 * WorkManager itself has no "cancel on foreground" hook, hence the explicit
 * [BackgroundScheduler.cancelTelemetryUpload] from [onForeground].
 */
class TelemetrySessionController(
    private val scheduler: BackgroundScheduler,
    private val debounceSeconds: Long
) {

    @Volatile
    private var activeSessionId: String? = null

    fun onForeground() {
        scheduler.cancelTelemetryUpload()
        activeSessionId = TelemetryEmitter.openSession()
    }

    fun onBackground() {
        val sessionId = activeSessionId ?: return
        TelemetryEmitter.closeSession(sessionId)
        activeSessionId = null
        scheduler.scheduleTelemetryUpload(debounceSeconds.coerceAtLeast(0))
    }
}
