package com.example.widget

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.entity.ClassEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Loads and shapes the data shown on the four home-screen widgets.
 * All queries run on Dispatchers.IO; call sites wrap them in coroutines.
 */
object WidgetData {

    data class ClassCell(
        val className: String,
        val time: String
    )

    data class ClassesState(
        val isHoliday: Boolean,
        val holidayTitle: String?,
        val ongoing: ClassCell?,
        val upcoming: ClassCell?
    )

    data class ListItem(
        val title: String,
        val date: String
    )

    private val shortDayFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

    /** Today's day name in the 3-letter form used by the weekly-holidays preference ("Mon"). */
    fun dayName(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)

    /**
     * Computes the ongoing and upcoming class for the classes widget,
     * honouring saved holiday ranges and weekly holidays.
     */
    suspend fun loadClassesState(context: Context): ClassesState = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val today = LocalDate.now()
        val todayKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val weeklyOff = com.example.data.datastore.PreferencesManager(context).weeklyHolidays.first()
        val holidays = database.holidayDao().getAllHolidays().first()

        // Holiday check identical to CalendarUtils.getHolidayDetails
        if (weeklyOff.any { it.equals(dayName(today), ignoreCase = true) }) {
            return@withContext ClassesState(
                isHoliday = true,
                holidayTitle = "Weekly Holiday",
                ongoing = null,
                upcoming = null
            )
        }
        val savedHoliday = holidays.find { todayKey >= it.startDate && todayKey <= it.endDate }
        if (savedHoliday != null) {
            return@withContext ClassesState(
                isHoliday = true,
                holidayTitle = savedHoliday.title.ifBlank { "Holiday" },
                ongoing = null,
                upcoming = null
            )
        }

        val nowMinutes = today.atTime(java.time.LocalTime.now()).let {
            it.hour * 60 + it.minute
        }
        val todayClasses = database.classDao().getAllClasses().first()
            .filter { it.dayOfWeek == today.dayOfWeek.value }
            .sortedBy { it.startTimeMinutes }

        val ongoing = todayClasses.find { it.startTimeMinutes <= nowMinutes && it.endTimeMinutes > nowMinutes }
        val upcoming = todayClasses.firstOrNull { it.startTimeMinutes > nowMinutes }

        ClassesState(
            isHoliday = false,
            holidayTitle = null,
            ongoing = ongoing?.let { ClassCell(it.name, formatTimeAmPm(it.startTime)) },
            upcoming = upcoming?.let { ClassCell(it.name, formatTimeAmPm(it.startTime)) }
        )
    }

    /** Next three dated events (notes with an event date attached). */
    suspend fun loadEvents(context: Context): List<ListItem> = withContext(Dispatchers.IO) {
        val todayEpoch = LocalDate.now().toEpochDay()
        AppDatabase.getDatabase(context).teacherNoteDao().getAllNotes().first()
            .filter { it.eventEpochDay != null && it.eventEpochDay >= todayEpoch }
            .sortedBy { it.eventEpochDay }
            .take(3)
            .map { note ->
                val date = LocalDate.ofEpochDay(note.eventEpochDay ?: todayEpoch)
                ListItem(
                    title = note.title.ifBlank { "Event" },
                    date = date.format(shortDayFormat)
                )
            }
    }

    /** Next three holiday ranges that have not fully ended yet. */
    suspend fun loadHolidays(context: Context): List<ListItem> = withContext(Dispatchers.IO) {
        val todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        AppDatabase.getDatabase(context).holidayDao().getAllHolidays().first()
            .filter { it.endDate >= todayKey }
            .sortedBy { it.startDate }
            .take(3)
            .map { holiday ->
                ListItem(
                    title = holiday.title.ifBlank { "Holiday" },
                    date = holiday.startDate
                )
            }
    }

    /** Next three exams sorted by date. */
    suspend fun loadExams(context: Context): List<ListItem> = withContext(Dispatchers.IO) {
        val todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        AppDatabase.getDatabase(context).examDao().getAllExams().first()
            .filter { it.dateString >= todayKey }
            .sortedBy { it.dateString }
            .take(3)
            .map { exam ->
                ListItem(
                    title = exam.name.ifBlank { "Exam" },
                    date = exam.dateString
                )
            }
    }

    /** "14:30" -> "2:30 PM" */
    private fun formatTimeAmPm(time: String): String = try {
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts.getOrNull(1) ?: "0"
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "$displayHour:$minute $amPm"
    } catch (e: Exception) {
        time
    }
}
