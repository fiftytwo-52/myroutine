package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Fires every 30 minutes while the routine alert service is NOT running its
 * class-window loop (i.e. outside school hours), refreshing all four widgets.
 * The service cancels this repeating alarm when its 30-second loop takes over.
 */
class WidgetAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        WidgetRefresh.refreshAll(context)
    }

    companion object {
        private const val REQUEST_CODE = 4242
        private const val INTERVAL_MINUTES = 30L

        fun arm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = pendingIntent(context)
            val intervalMillis = INTERVAL_MINUTES * 60 * 1000
            val firstTrigger = System.currentTimeMillis() + intervalMillis
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC, firstTrigger, pendingIntent
                    )
                    return
                }
            }
            alarmManager.setRepeating(
                AlarmManager.RTC,
                firstTrigger,
                intervalMillis,
                pendingIntent
            )
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent(context))
        }

        private fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WidgetAlarmReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
