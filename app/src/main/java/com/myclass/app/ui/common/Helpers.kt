package com.myclass.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.myclass.app.MyClassApplication
import com.myclass.app.data.local.AppDatabase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Convenience accessor for the offline Room database from composables. */
@Composable
fun rememberDb(): AppDatabase {
    val app = LocalContext.current.applicationContext as MyClassApplication
    return app.database
}

object DateUtils {
    val dayNames: List<String> =
        DayOfWeek.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun epochDayToString(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

    fun nowMinutes(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

    fun hhmmToMinutes(hhmm: String): Int =
        try {
            LocalTime.parse(hhmm).let { it.hour * 60 + it.minute }
        } catch (_: Exception) {
            0
        }
}

/** Dynamic status of a class period relative to the current time. */
enum class ClassStatus { ACTIVE, UPCOMING, COMPLETED, SCHEDULED }

fun computeStatus(startTime: String, endTime: String, isToday: Boolean): ClassStatus {
    if (!isToday) return ClassStatus.SCHEDULED
    val now = DateUtils.nowMinutes()
    val start = DateUtils.hhmmToMinutes(startTime)
    val end = DateUtils.hhmmToMinutes(endTime)
    return when {
        now in start until end -> ClassStatus.ACTIVE
        now >= end -> ClassStatus.COMPLETED
        else -> ClassStatus.UPCOMING
    }
}
