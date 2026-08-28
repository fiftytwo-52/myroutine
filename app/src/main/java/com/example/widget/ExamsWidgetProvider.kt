package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "MyClass Exams" widget: the next three exams with their dates,
 * an empty-state message, and an app-launch tap intent.
 */
class ExamsWidgetProvider : AppWidgetProvider() {

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
            val items = WidgetData.loadExams(context)
            WidgetRefresh.renderList(
                context, manager, appWidgetIds,
                title = "MyClass Exams",
                emptyMessage = "No upcoming exams",
                items = items
            )
        }
    }
}
