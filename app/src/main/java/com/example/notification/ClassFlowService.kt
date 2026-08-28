package com.example.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.entity.ClassEntity
import com.example.widget.WidgetAlarmReceiver
import com.example.widget.WidgetRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Foreground service that runs ONLY inside the day's class window
 * (first start - 2 minutes .. last end). While the window is active it:
 *  - keeps a persistent "Classes 9:30 AM – 3:30 PM" notification with NOW/NEXT detail,
 *  - fires 2-minute-before reminders plus start/end alerts,
 *  - refreshes all four home-screen widgets every 30 seconds.
 *
 * Outside the window it schedules an exact alarm that restarts the service at the
 * next window's start (scanning up to 30 days ahead, skipping holidays and weekly
 * holidays), arms the 30-minute widget-refresh alarm, and stops itself.
 */
class ClassFlowService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CHANNEL_ID = "classflow_persistent"
    private val ALERT_CHANNEL_ID = "classflow_alerts"
    private val NOTIFICATION_ID = 888

    private var notifiedStartClasses = mutableSetOf<Int>()
    private var notifiedEndClasses = mutableSetOf<Int>()
    private var notifiedUpcomingClasses = mutableSetOf<Int>()

    /** One day's class window: from (earliest start - 2 min) to (latest end). */
    private data class ClassWindow(
        val date: LocalDate,
        val startMin: Int, // window start, already includes the 2-minute head start
        val endMin: Int,
        val classes: List<ClassEntity>
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createPersistentNotification("Checking class schedule...")
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            try {
                runWindowCycle()
            } catch (e: Exception) {
                android.util.Log.e("ClassFlowService", "Window cycle failed: ${e.message}")
                shutdown()
            }
        }

        return START_STICKY
    }

    private suspend fun runWindowCycle() {
        val today = LocalDate.now()
        val nowMin = nowMinutesOfDay()
        val window = loadWindow(today)

        when {
            // Today is a holiday / has no classes / the window already ended.
            window == null || nowMin > window.endMin -> {
                scheduleNextWindowRestart(startingFrom = if (window == null) today else today.plusDays(1))
                shutdown()
            }
            // Window has not started yet — restart exactly when it opens.
            nowMin < window.startMin -> {
                scheduleRestartAt(window.date, window.startMin)
                shutdown()
            }
            // We are inside the window — take over widget refreshes and run the loop.
            else -> {
                WidgetAlarmReceiver.cancel(this)
                runInsideWindowLoop()
                scheduleNextWindowRestart(startingFrom = LocalDate.now().plusDays(1))
                shutdown()
            }
        }
    }

    private suspend fun runInsideWindowLoop() {
        while (true) {
            val window = loadWindow(LocalDate.now()) ?: return
            val currentMin = nowMinutesOfDay()
            if (currentMin > window.endMin) return

            // Keep all four widgets fresh while school is in session.
            WidgetRefresh.refreshAll(this)

            // Persistent notification: window title + NOW/NEXT detail.
            updateNotification(
                title = "Classes ${formatTimeAmPm(window.classes.minOf { it.startTimeMinutes })} – " +
                    formatTimeAmPm(window.classes.maxOf { it.endTimeMinutes }),
                content = buildStatusDetail(window.classes, currentMin)
            )

            // 2-min / start / end reminders.
            window.classes.forEach { classEntity ->
                val id = classEntity.id
                if (currentMin == classEntity.startTimeMinutes - 2 && !notifiedUpcomingClasses.contains(id)) {
                    sendAlertNotification("Class in 2 mins", "${classEntity.name} is starting in 2 minutes.", id * 10 + 1)
                    notifiedUpcomingClasses.add(id)
                }
                if (currentMin == classEntity.startTimeMinutes && !notifiedStartClasses.contains(id)) {
                    sendAlertNotification("Class Started", "${classEntity.name} has started.", id * 10 + 2)
                    notifiedStartClasses.add(id)
                }
                if (currentMin == classEntity.endTimeMinutes && !notifiedEndClasses.contains(id)) {
                    sendAlertNotification("Class Ended", "${classEntity.name} has ended.", id * 10 + 3)
                    notifiedEndClasses.add(id)
                }
            }

            delay(30_000) // 30-second cadence
        }
    }

    private fun buildStatusDetail(classes: List<ClassEntity>, currentMin: Int): String {
        val currentClass = classes.find { it.startTimeMinutes <= currentMin && it.endTimeMinutes > currentMin }
        val nextClass = classes.filter { it.startTimeMinutes > currentMin }.minByOrNull { it.startTimeMinutes }
        return when {
            currentClass != null -> "NOW: ${currentClass.name} (${currentClass.subject}) in ${currentClass.roomNumber}"
            nextClass != null -> "NEXT: ${nextClass.name} at ${nextClass.startTime}"
            else -> "Classes wrapping up..."
        }
    }

    // ------------------------------------------------------------------
    // Window / holiday logic
    // ------------------------------------------------------------------

    /**
     * Loads the class window for [date], or null when the date is a saved
     * holiday, a weekly holiday, or simply has no classes.
     */
    private suspend fun loadWindow(date: LocalDate): ClassWindow? {
        val database = AppDatabase.getDatabase(applicationContext)

        val weeklyOff = com.example.data.datastore.PreferencesManager(applicationContext)
            .weeklyHolidays.first()
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
        if (weeklyOff.any { it.equals(dayName, ignoreCase = true) }) return null

        val dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val holidays = database.holidayDao().getAllHolidays().first()
        if (holidays.any { dateKey >= it.startDate && dateKey <= it.endDate }) return null

        val classes = database.classDao().getAllClasses().first()
            .filter { it.dayOfWeek == date.dayOfWeek.value }
            .sortedBy { it.startTimeMinutes }
        if (classes.isEmpty()) return null

        return ClassWindow(
            date = date,
            startMin = (classes.minOf { it.startTimeMinutes } - 2).coerceAtLeast(0),
            endMin = classes.maxOf { it.endTimeMinutes },
            classes = classes
        )
    }

    /**
     * Scans forward (max 30 days) for the next class window and schedules an
     * exact restart alarm at its start; arms the 30-minute widget alarm either way.
     */
    private suspend fun scheduleNextWindowRestart(startingFrom: LocalDate) {
        var candidate = startingFrom
        repeat(30) {
            val window = loadWindow(candidate)
            if (window != null) {
                val sameDayAndAlreadyPast = window.date == LocalDate.now() &&
                    nowMinutesOfDay() > window.startMin
                if (!sameDayAndAlreadyPast) {
                    scheduleRestartAt(window.date, window.startMin)
                    WidgetAlarmReceiver.arm(this)
                    return
                }
            }
            candidate = candidate.plusDays(1)
        }
        // No class window within 30 days — keep widgets alive via the repeating alarm.
        WidgetAlarmReceiver.arm(this)
    }

    /** Exact alarm that restarts this service when the next window opens. */
    private fun scheduleRestartAt(date: LocalDate, startMin: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, BootReceiver::class.java).apply {
            action = BootReceiver.ACTION_START_SERVICE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_SERVICE_RESTART,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() +
            startMin * 60_000L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, 60_000L, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelServiceRestartAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, BootReceiver::class.java).apply {
            action = BootReceiver.ACTION_START_SERVICE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_SERVICE_RESTART,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun nowMinutesOfDay(): Int {
        val now = LocalDateTime.now()
        return now.hour * 60 + now.minute
    }

    private fun shutdown() {
        cancelServiceRestartAlarm()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    // ------------------------------------------------------------------
    // Notifications
    // ------------------------------------------------------------------

    private fun sendAlertNotification(title: String, text: String, notificationId: Int) {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setCategory(Notification.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createPersistentNotification(content, title)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createPersistentNotification(content: String, title: String = "App is running in school time"): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Persistent Routine Tracker",
                NotificationManager.IMPORTANCE_LOW
            )
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Class Flow Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies on class start, end, and 2 mins before."
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun formatTimeAmPm(minutes: Int): String {
        val hour = minutes / 60
        val minute = minutes % 60
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val REQUEST_CODE_SERVICE_RESTART = 777

        /** Starts the service from any context (used by BootReceiver and ScheduleSync). */
        fun start(context: Context) {
            val intent = Intent(context, ClassFlowService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
