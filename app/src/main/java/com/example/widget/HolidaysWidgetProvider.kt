package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "MyClass Holidays" widget: the next three saved holiday ranges
 * with their dates, an empty-state message, and an app-launch tap intent.
 */
class HolidaysWidgetProvider : AppWidgetProvider() {

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
            val items = WidgetData.loadHolidays(context)
            WidgetRefresh.renderList(
                context, manager, appWidgetIds,
                title = "MyClass Holidays",
                emptyMessage = "No upcoming holidays",
                items = items
            )
        }
    }
}
