package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val classId = intent.getIntExtra("class_id", 0)
        val className = intent.getStringExtra("class_name") ?: "Class"
        val subject = intent.getStringExtra("subject") ?: ""
        val roomNumber = intent.getStringExtra("room_number") ?: ""
        val alarmType = intent.getStringExtra("alarm_type") ?: "START" // "START" or "END"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "classflow_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MyClass Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Class starting and finishing notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            classId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val text: String
        if (alarmType == "START") {
            title = "Class Starting Now! 🔔"
            text = "Your $className class ($subject) is starting now in Room $roomNumber."
        } else {
            title = "Class Ending in 2 Minutes! ⏳"
            text = "Your $className class ($subject) in Room $roomNumber is finishing in 2 minutes. Plan wrapping up!"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (alarmType == "START") classId * 2 else (classId * 2) + 1
        notificationManager.notify(notificationId, notification)
    }
}
