package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.entity.ClassEntity
import java.util.Calendar

object NotificationScheduler {

    private const val TAG = "NotificationScheduler"

    fun scheduleAlarms(context: Context, classes: List<ClassEntity>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. First, cancel any alarms for all possible classes to clear stale configurations
        classes.forEach { classEntity ->
            cancelAlarm(context, alarmManager, classEntity.id, "START")
            cancelAlarm(context, alarmManager, classEntity.id, "END")
        }

        // 2. Schedule starting alarms and ending alarms (2 mins before ending)
        classes.forEach { classEntity ->
            try {
                // START alarm
                val startMillis = getNextOccurrence(classEntity.dayOfWeek, classEntity.startTime)
                scheduleExactAlarm(context, alarmManager, classEntity, startMillis, "START")

                // END alarm (2 minutes before endTime)
                val endBufferTime = subtractMinutes(classEntity.endTime, 2)
                val endMillis = getNextOccurrence(classEntity.dayOfWeek, endBufferTime)
                scheduleExactAlarm(context, alarmManager, classEntity, endMillis, "END")
                
                Log.d(TAG, "Scheduled alarms for class ID ${classEntity.id}: START at ${classEntity.startTime}, END buffer at $endBufferTime")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarm: ${e.message}")
            }
        }
    }

    private fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        classEntity: ClassEntity,
        triggerMillis: Long,
        alarmType: String
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("class_id", classEntity.id)
            putExtra("class_name", classEntity.name)
            putExtra("subject", classEntity.subject)
            putExtra("room_number", classEntity.roomNumber)
            putExtra("alarm_type", alarmType)
        }

        val requestCode = if (alarmType == "START") classEntity.id * 10 else (classEntity.id * 10) + 1
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback for systems that restrict alarm scheduling
            try {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } catch (e2: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun cancelAlarm(
        context: Context,
        alarmManager: AlarmManager,
        classId: Int,
        alarmType: String
    ) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val requestCode = if (alarmType == "START") classId * 10 else (classId * 10) + 1
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun getNextOccurrence(dayOfWeek: Int, timeString: String): Long {
        val parts = timeString.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            val targetDay = when (dayOfWeek) {
                1 -> Calendar.MONDAY
                2 -> Calendar.TUESDAY
                3 -> Calendar.WEDNESDAY
                4 -> Calendar.THURSDAY
                5 -> Calendar.FRIDAY
                6 -> Calendar.SATURDAY
                7 -> Calendar.SUNDAY
                else -> Calendar.MONDAY
            }
            
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Adjust Calendar Day
            val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)
            var daysDifference = targetDay - currentDayOfWeek
            if (daysDifference < 0 || (daysDifference == 0 && timeInMillis <= System.currentTimeMillis())) {
                daysDifference += 7
            }
            add(Calendar.DAY_OF_YEAR, daysDifference)
        }

        return calendar.timeInMillis
    }

    private fun subtractMinutes(timeString: String, minutes: Int): String {
        return try {
            val parts = timeString.split(":")
            var h = parts[0].toInt()
            var m = parts[1].toInt()
            m -= minutes
            if (m < 0) {
                m += 60
                h -= 1
                if (h < 0) {
                    h = 23
                }
            }
            String.format("%02d:%02d", h, m)
        } catch (e: Exception) {
            timeString
        }
    }
}
