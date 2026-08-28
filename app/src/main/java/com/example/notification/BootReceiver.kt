package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms all exact class alarms after a device reboot or app update.
 * Without this, alarms scheduled with AlarmManager are lost when the
 * device restarts and class reminders would never fire again.
 *
 * Also receives the exact alarm fired by ClassFlowService at the start of
 * the next class window and (re)starts the foreground service then.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                ScheduleSync.syncNow(context)
            }
            ACTION_START_SERVICE -> {
                ClassFlowService.start(context)
            }
        }
    }

    companion object {
        /** Fired by ClassFlowService's exact alarm when the next class window opens. */
        const val ACTION_START_SERVICE = "com.example.action.START_CLASSFLOW_SERVICE"
    }
}
