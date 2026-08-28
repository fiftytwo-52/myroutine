package com.example.ui.screens

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object CalendarUtils {

    // Start of BS 2081 is Gregorian April 13, 2024
    private val epochGregorian: Long by lazy {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.APRIL, 13, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    private const val epochBSYear = 2081
    private const val epochBSMonth = 1 // Baisakh
    private const val epochBSDay = 1

    // Month lengths for BS years 2081 to 2085
    private val bsMonths = mapOf(
        2081 to listOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2082 to listOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 31), // Added 1 day to Chaitra
        2083 to listOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2084 to listOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2085 to listOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30)
    )

    private val nepaliWeeks = listOf("आइतबार", "सोमबार", "मङ्गलबार", "बुधबार", "बिहीबार", "शुक्रबार", "शनिबार")
    private val englishWeeks = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    private val nepaliMonths = listOf(
        "वैशाख", "जेठ", "असार", "साउन", "भदौ", "असोज",
        "कात्तिक", "मंसिर", "पुस", "माघ", "फागुन", "चैत"
    )

    fun toNepaliNumber(number: Int): String {
        val nepaliDigits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
        return number.toString().map {
            if (it.isDigit()) nepaliDigits[it - '0'] else it
        }.joinToString("")
    }

    fun toNepaliNumber(numberStr: String): String {
        val nepaliDigits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
        return numberStr.map {
            if (it.isDigit()) nepaliDigits[it - '0'] else it
        }.joinToString("")
    }

    data class NepaliDate(
        val year: Int,
        val month: Int, // 1-indexed
        val day: Int,
        val monthName: String,
        val dayOfWeekNameNepali: String,
        val dayOfWeekNameEnglish: String
    ) {
        fun formattedBS(): String = "${toNepaliNumber(year)} $monthName ${toNepaliNumber(day)}, $dayOfWeekNameNepali"
        fun formattedBSShort(): String = "${toNepaliNumber(year)}-${toNepaliNumber(String.format("%02d", month))}-${toNepaliNumber(String.format("%02d", day))}"
    }

    fun getNepaliDate(date: Date): NepaliDate {
        val cal = Calendar.getInstance().apply { time = date }
        val dayOfWeekIndex = cal.get(Calendar.DAY_OF_WEEK) - 1 // Sunday is 1 -> index 0

        val targetCal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = targetCal.timeInMillis - epochGregorian
        val diffDays = Math.round(diffMillis / (1000.0 * 60 * 60 * 24)).toInt()

        var currentDays = diffDays
        var year = epochBSYear
        var month = epochBSMonth
        var day = epochBSDay

        if (currentDays >= 0) {
            // Forward in time
            while (true) {
                val monthsList = bsMonths[year] ?: listOf(30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30)
                val daysInMonth = monthsList[month - 1]
                if (currentDays >= daysInMonth) {
                    currentDays -= daysInMonth
                    month++
                    if (month > 12) {
                        month = 1
                        year++
                    }
                } else {
                    day += currentDays
                    break
                }
            }
        } else {
            // Backward in time
            currentDays = abs(currentDays)
            while (currentDays > 0) {
                month--
                if (month < 1) {
                    month = 12
                    year--
                }
                val monthsList = bsMonths[year] ?: listOf(30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30)
                val daysInMonth = monthsList[month - 1]
                if (currentDays >= daysInMonth) {
                    currentDays -= daysInMonth
                } else {
                    day = daysInMonth - currentDays + 1
                    break
                }
            }
        }

        val monthName = nepaliMonths.getOrElse(month - 1) { "महिना" }
        val dayOfWeekNameNepali = nepaliWeeks.getOrElse(dayOfWeekIndex) { "" }
        val dayOfWeekNameEnglish = englishWeeks.getOrElse(dayOfWeekIndex) { "" }

        return NepaliDate(
            year = year,
            month = month,
            day = day,
            monthName = monthName,
            dayOfWeekNameNepali = dayOfWeekNameNepali,
            dayOfWeekNameEnglish = dayOfWeekNameEnglish
        )
    }

    data class HolidayMatch(
        val title: String,
        val description: String
    )

    /**
     * Checks if a given Date falls into any holiday ranges or weekly off days
     */
    fun getHolidayDetails(date: Date, holidays: List<com.example.data.entity.HolidayEntity>, weeklyOff: Set<String> = emptySet()): HolidayMatch? {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val targetStr = sdf.format(date)
        
        val cal = Calendar.getInstance().apply { time = date }
        val dayOfWeekIndex = cal.get(Calendar.DAY_OF_WEEK) - 1
        val dayName = englishWeeks.getOrElse(dayOfWeekIndex) { "" }.take(3)
        if (weeklyOff.contains(dayName)) {
            return HolidayMatch(
                title = "Weekly Off ($dayName)",
                description = "This is a regular weekly scheduled off-day."
            )
        }

        val manualHoliday = holidays.find { holiday ->
            targetStr >= holiday.startDate && targetStr <= holiday.endDate
        }
        if (manualHoliday != null) {
            return HolidayMatch(
                title = manualHoliday.title,
                description = "This date falls under the holiday period scheduled from ${manualHoliday.startDate} to ${manualHoliday.endDate}."
            )
        }
        return null
    }
}
