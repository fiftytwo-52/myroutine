package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Central helper that pushes fresh data into all four home-screen widgets.
 * Called on widget onUpdate, from the routine alert service's 30-second loop,
 * from the 30-minute WidgetAlarmReceiver, and after any holiday/event/exam
 * save or delete so the widgets never show stale names.
 */
object WidgetRefresh {

    fun refreshAll(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = AppWidgetManager.getInstance(app)

                ClassesWidgetProvider.pushUpdate(
                    app, manager, idsOf(app, manager, ClassesWidgetProvider::class.java)
                )
                EventsWidgetProvider.pushUpdate(
                    app, manager, idsOf(app, manager, EventsWidgetProvider::class.java)
                )
                HolidaysWidgetProvider.pushUpdate(
                    app, manager, idsOf(app, manager, HolidaysWidgetProvider::class.java)
                )
                ExamsWidgetProvider.pushUpdate(
                    app, manager, idsOf(app, manager, ExamsWidgetProvider::class.java)
                )
            } catch (e: Exception) {
                android.util.Log.e("WidgetRefresh", "Failed to refresh widgets: ${e.message}")
            }
        }
    }

    private fun idsOf(context: Context, manager: AppWidgetManager, provider: Class<*>): IntArray =
        manager.getAppWidgetIds(ComponentName(context, provider))

    /**
     * Shared renderer for the three "next three items with dates" widgets
     * (Events, Holidays, Exams). Shows an empty-state message when the list
     * has no rows and wires a tap-anywhere launch intent.
     */
    fun renderList(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray,
        title: String,
        emptyMessage: String,
        items: List<WidgetData.ListItem>
    ) {
        val launchIntent = android.content.Intent(context, com.example.MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, launchIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val titleViews = intArrayOf(R.id.row_title_1, R.id.row_title_2, R.id.row_title_3)
        val dateViews = intArrayOf(R.id.row_date_1, R.id.row_date_2, R.id.row_date_3)
        val rowViews = intArrayOf(R.id.widget_row_1, R.id.widget_row_2, R.id.widget_row_3)

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_list).apply {
                setTextViewText(R.id.widget_header, title)
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                for (index in 0..2) {
                    val item = items.getOrNull(index)
                    setViewVisibility(rowViews[index], if (item != null) android.view.View.VISIBLE else android.view.View.GONE)
                    if (item != null) {
                        setTextViewText(titleViews[index], item.title)
                        setTextViewText(dateViews[index], item.date)
                    }
                }
                setViewVisibility(
                    R.id.widget_empty,
                    if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                )
                if (items.isEmpty()) {
                    setTextViewText(R.id.widget_empty, emptyMessage)
                }
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
