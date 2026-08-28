package com.example.notification

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.entity.ClassEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object ScheduleSync {
    /**
     * Re-arms all class alarms and starts the routine tracker service.
     * Called on app open and when schedule/holiday data changes to ensure
     * that notifications and background monitoring are up to date.
     */
    fun syncNow(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(appContext)
                val classes = database.classDao().getAllClasses().first()
                sync(appContext, classes)
            } catch (e: Exception) {
                android.util.Log.e("ScheduleSync", "Failed to sync schedules: ${e.message}")
            }
        }
    }

    /**
     * Internal sync logic that can be reused when classes are already available.
     */
    fun sync(context: Context, classes: List<ClassEntity>) {
        // 1. Re-arm the exact alarms for all classes via the scheduler
        NotificationScheduler.scheduleAlarms(context, classes)

        // 2. Start the tracking service. The ClassFlowService decides whether
        //    the class window is active; when it is not, it schedules an exact
        //    restart alarm for the next window (up to 30 days ahead, skipping
        //    holidays) and arms the 30-minute widget refresh alarm instead.
        ClassFlowService.start(context)

        // 3. Push fresh data into the home-screen widgets right away.
        com.example.widget.WidgetRefresh.refreshAll(context)
    }
}
