package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "MyClass Classes" widget: a compact single-row pill showing the ongoing
 * class (left circle, red border) and the upcoming class (right circle,
 * green border) with the class name and start time inside each circle.
 * Respects saved holiday ranges and weekly holidays; opens the app on tap.
 */
class ClassesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                pushUpdate(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        suspend fun pushUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
            val state = WidgetData.loadClassesState(context)

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_classes).apply {
                    setOnClickPendingIntent(R.id.widget_classes_root, pendingIntent)

                    val showCircles = !state.isHoliday && (state.ongoing != null || state.upcoming != null)
                    setViewVisibility(R.id.widget_circles_row, if (showCircles) View.VISIBLE else View.GONE)
                    setViewVisibility(R.id.widget_classes_status, if (showCircles) View.GONE else View.VISIBLE)

                    if (showCircles) {
                        // Left circle — ongoing class (red border)
                        val ongoing = state.ongoing
                        setViewVisibility(R.id.left_circle, if (ongoing != null) View.VISIBLE else View.INVISIBLE)
                        if (ongoing != null) {
                            setTextViewText(R.id.left_name, ongoing.className)
                            setTextViewText(R.id.left_time, ongoing.time)
                        }
                        // Right circle — upcoming class (green border)
                        val upcoming = state.upcoming
                        setViewVisibility(R.id.right_circle, if (upcoming != null) View.VISIBLE else View.INVISIBLE)
                        if (upcoming != null) {
                            setTextViewText(R.id.right_name, upcoming.className)
                            setTextViewText(R.id.right_time, upcoming.time)
                        }
                    } else {
                        val message = when {
                            state.isHoliday -> "🎉 ${state.holidayTitle ?: "Holiday"} — no classes today"
                            else -> "No classes left today"
                        }
                        setTextViewText(R.id.widget_classes_status, message)
                    }
                }
                manager.updateAppWidget(widgetId, views)
            }
        }
    }
}
