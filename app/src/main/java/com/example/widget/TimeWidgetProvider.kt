package com.example.widget

import android.app.PendingIntent
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "MyClass Time" widget: a vertical pill clock — the hour stacked over the
 * minutes with a small AM/PM underneath, on a fully transparent background
 * with a solid pill outline. Both rows are live TextClocks that tick on
 * their own, so the provider only needs to re-evaluate the adaptive colour
 * scheme and keep the launch tap working.
 *
 * The text/outline colour follows the wallpaper: the system's wallpaper
 * colour hints (the same source Material You uses) tell us whether dark text
 * is legible, so over light wallpapers the outline and text switch to a dark
 * tone and stay white over dark/colourful wallpapers.
 */
class TimeWidgetProvider : AppWidgetProvider() {

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
            val lightWallpaper = wallpaperSupportsDarkText(context)
            val textColor = if (lightWallpaper) 0xFF1F1F1F.toInt() else Color.WHITE
            val frameRes = if (lightWallpaper) {
                R.drawable.widget_time_frame_dark
            } else {
                R.drawable.widget_time_frame_light
            }

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_time).apply {
                    setOnClickPendingIntent(R.id.widget_time_root, pendingIntent)
                    // Swap the outline drawable (View.setBackgroundResource is remotable).
                    setInt(R.id.widget_time_pill, "setBackgroundResource", frameRes)
                    setTextColor(R.id.widget_time_hour, textColor)
                    setTextColor(R.id.widget_time_minute, textColor)
                    setTextColor(R.id.widget_time_ampm, textColor)
                }
                manager.updateAppWidget(widgetId, views)
            }
        }

        /**
         * True when dark text is legible over the current system wallpaper.
         * Reads the wallpaper colour hints via WallpaperManager — the same
         * signal the launcher uses for Material You — which needs no special
         * permission. Falls back to "dark wallpaper" (white text) whenever
         * the hints are unavailable.
         */
        private fun wallpaperSupportsDarkText(context: Context): Boolean = try {
            val colors = WallpaperManager.getInstance(context)
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            if (colors != null) {
                (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
