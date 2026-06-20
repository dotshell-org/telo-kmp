@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.pelotcl.app.platform

import platform.BackgroundTasks.*
import platform.Foundation.*

/**
 * iOS implementation using BGTaskScheduler.
 */
actual class BackgroundScheduler actual constructor(context: PlatformContext) {

    actual fun scheduleTelemetryUpload(delaySeconds: Long) {
        val request = BGAppRefreshTaskRequest("com.pelotcl.app.telemetryUpload")
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(delaySeconds.toDouble())
        
        val success = BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        if (success) {
            Log.i(TAG, "scheduleTelemetryUpload($delaySeconds) submitted")
        } else {
            Log.e(TAG, "Failed to submit telemetry upload task")
        }
    }

    actual fun cancelTelemetryUpload() {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier("com.pelotcl.app.telemetryUpload")
        Log.i(TAG, "cancelTelemetryUpload() called")
    }

    actual fun ensureTrafficAlertsScheduled() {
        val request = BGAppRefreshTaskRequest("com.pelotcl.app.trafficAlerts")
        
        val success = BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        if (success) {
            Log.i(TAG, "ensureTrafficAlertsScheduled() submitted")
        } else {
            Log.e(TAG, "Failed to submit traffic alerts task")
        }
    }

    private companion object {
        const val TAG = "BackgroundScheduler"
    }
}
