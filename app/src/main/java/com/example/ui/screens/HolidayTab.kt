package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.HolidayEntity
import com.example.data.entity.TeacherNoteEntity
import com.example.ui.ClassFlowViewModel
import com.example.ui.epochDayToDateString
import com.example.ui.formatEpochDay
import com.example.ui.localDateToEpochDay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HolidaysConfigTab(viewModel: ClassFlowViewModel) {
    val weeklyHolidays by viewModel.weeklyHolidays.collectAsStateWithLifecycle(emptySet())
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Weekly Recurring Holidays",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Select days that are universally off every week.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayNames.forEach { dayName ->
                    val isSelected = weeklyHolidays.contains(dayName)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { viewModel.toggleWeeklyHoliday(dayName) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayName,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Specific Scheduled Holidays",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Select a date or range to add a specific event break (e.g. Dashain).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            SettingsHolidayCalendar(viewModel = viewModel, weeklyOff = weeklyHolidays)
        }
    }
}

@Composable
fun SettingsHolidayCalendar(
    viewModel: ClassFlowViewModel,
    weeklyOff: Set<String>
) {
    val holidays by viewModel.allHolidays.collectAsStateWithLifecycle(initialValue = emptyList())
    val datedEvents by viewModel.allDatedEvents.collectAsStateWithLifecycle(emptyList())
    
    val sdfStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedStartDate by remember { mutableStateOf<Calendar?>(null) }
    var selectedEndDate by remember { mutableStateOf<Calendar?>(null) }
    var showHolidayNameDialog by remember { mutableStateOf(false) }
    var holidayToDelete by remember { mutableStateOf<HolidayEntity?>(null) }
    var holidayTitle by remember { mutableStateOf("") }

    val monthName = remember(calendarMonth) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        sdf.format(calendarMonth.time)
    }

    val daysInMonth = remember(calendarMonth) {
        val list = mutableListOf<Calendar?>()
        val temp = calendarMonth.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK)
        val maxDays = temp.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1 until firstDayOfWeek) {
            list.add(null)
        }
        for (d in 1..maxDays) {
            val cell = temp.clone() as Calendar
            cell.set(Calendar.DAY_OF_MONTH, d)
            list.add(cell)
        }
        list
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            calendarMonth = Calendar.getInstance()
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("TODAY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = {
                            val prev = calendarMonth.clone() as Calendar
                            prev.add(Calendar.MONTH, -1)
                            calendarMonth = prev
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "Prev", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            val next = calendarMonth.clone() as Calendar
                            next.add(Calendar.MONTH, 1)
                            calendarMonth = next
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, "Next", modifier = Modifier.size(18.dp))
                    }
                }
            }

            val weekRow = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                weekRow.forEach { char ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val chunks = daysInMonth.chunked(7)
                chunks.forEach { weekDays ->
                    val paddedWeekDays = weekDays.toMutableList()
                    while (paddedWeekDays.size < 7) {
                        paddedWeekDays.add(null)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        paddedWeekDays.forEachIndexed { index, cellCal ->
                            val isWeeklyOff = weeklyOff.contains(weekRow[index])
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cellCal == null) {
                                    Box(modifier = Modifier.size(36.dp))
                                } else {
                                    val cellDateStr = sdfStr.format(cellCal.time)
                                    val isSelectedStart = selectedStartDate != null && sdfStr.format(selectedStartDate!!.time) == cellDateStr
                                    val isSelectedEnd = selectedEndDate != null && sdfStr.format(selectedEndDate!!.time) == cellDateStr
                                    val isBetweenRange = selectedStartDate != null && selectedEndDate != null &&
                                            cellCal.after(selectedStartDate) && cellCal.before(selectedEndDate)
                                    
                                    val matchedHoliday = holidays.find { cellDateStr >= it.startDate && cellDateStr <= it.endDate }
                                    val isManualHoliday = matchedHoliday != null
                                    
                                    val isHoliday = isManualHoliday || isWeeklyOff
                                    
                                    // Dated calendar events are highlighted in the secondary tone.
                                    val cellEpochDay = remember(cellDateStr) { localDateToEpochDay(cellDateStr) }
                                    val isEventDay = !isHoliday &&
                                            cellEpochDay != null && datedEvents.any { it.eventEpochDay == cellEpochDay }
                                    
                                    val isToday = remember(cellDateStr) {
                                        val now = Calendar.getInstance()
                                        sdfStr.format(now.time) == cellDateStr
                                    }

                                    val cellColor = when {
                                        isSelectedStart || isSelectedEnd -> MaterialTheme.colorScheme.primary
                                        isBetweenRange -> MaterialTheme.colorScheme.primaryContainer
                                        isHoliday -> MaterialTheme.colorScheme.errorContainer
                                        isEventDay -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> Color.Transparent
                                    }

                                    val textCol = when {
                                        isSelectedStart || isSelectedEnd -> MaterialTheme.colorScheme.onPrimary
                                        isBetweenRange -> MaterialTheme.colorScheme.onPrimaryContainer
                                        isHoliday -> MaterialTheme.colorScheme.error
                                        isEventDay -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cellColor)
                                            .then(
                                                if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                else Modifier
                                            )
                                            .clickable {
                                                if (selectedStartDate == null || (selectedStartDate != null && selectedEndDate != null)) {
                                                    selectedStartDate = cellCal
                                                    selectedEndDate = null
                                                } else {
                                                    if (cellCal.before(selectedStartDate)) {
                                                        selectedStartDate = cellCal
                                                    } else {
                                                        selectedEndDate = cellCal
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cellCal.get(Calendar.DAY_OF_MONTH).toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isHoliday || isEventDay || isSelectedStart || isSelectedEnd) FontWeight.Bold else FontWeight.Normal,
                                            color = textCol
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedStartDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val startLabel = sdfStr.format(selectedStartDate!!.time)
                    val endLabel = selectedEndDate?.let { sdfStr.format(it.time) } ?: startLabel
                    Text(
                        text = if (selectedEndDate == null) "Selected: $startLabel" else "Range: $startLabel to $endLabel",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Button(
                        onClick = {
                            holidayTitle = ""
                            showHolidayNameDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Add Holiday", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (holidays.isNotEmpty() || datedEvents.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Text(
                    text = "Scheduled Holidays List:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    holidays.forEach { hol ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = hol.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "${hol.startDate} to ${hol.endDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(
                                onClick = { holidayToDelete = hol },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Holiday",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHolidayNameDialog) {
        val startLabel = sdfStr.format(selectedStartDate!!.time)
        val endLabel = selectedEndDate?.let { sdfStr.format(it.time) } ?: startLabel
        
        AlertDialog(
            onDismissRequest = { showHolidayNameDialog = false },
            title = { Text("Register Official School Holiday", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a label/description for the holiday break (from $startLabel to $endLabel):", fontSize = 12.sp)
                    OutlinedTextField(
                        value = holidayTitle,
                        onValueChange = { holidayTitle = it },
                        placeholder = { Text("e.g. Dashain Break, Summer Recess") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (holidayTitle.isNotBlank()) {
                            viewModel.saveHoliday(holidayTitle, startLabel, endLabel)
                            selectedStartDate = null
                            selectedEndDate = null
                            showHolidayNameDialog = false
                        }
                    },
                    enabled = holidayTitle.isNotBlank()
                ) {
                    Text("Save Holiday")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHolidayNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    holidayToDelete?.let { hol ->
        AlertDialog(
            onDismissRequest = { holidayToDelete = null },
            title = { Text("Delete Holiday", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Remove \"${hol.title}\" (${hol.startDate} to ${hol.endDate}) from the holiday calendar? " +
                        "Routine classes will resume on these dates.",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHoliday(hol)
                        holidayToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { holidayToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ViewOnlyHolidayCalendar(
    viewModel: ClassFlowViewModel,
    weeklyOff: Set<String>
) {
    val holidays by viewModel.allHolidays.collectAsStateWithLifecycle(initialValue = emptyList())
    val datedEvents by viewModel.allDatedEvents.collectAsStateWithLifecycle(emptyList())
    
    val sdfStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }

    // Holidays / Events list toggle + search inside the calendar card.
    var listMode by remember { mutableStateOf("Holidays") }
    var calendarSearch by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf<Long?>(null) }
    var editingEvent by remember { mutableStateOf<TeacherNoteEntity?>(null) }
    var eventDateEpochDay by remember { mutableStateOf<Long?>(null) }
    var showEventDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<TeacherNoteEntity?>(null) }

    val filteredHolidays = remember(holidays, calendarSearch) {
        if (calendarSearch.isBlank()) holidays
        else holidays.filter { it.title.contains(calendarSearch, ignoreCase = true) }
    }
    val filteredEvents = remember(datedEvents, calendarSearch) {
        if (calendarSearch.isBlank()) datedEvents
        else datedEvents.filter {
            it.title.contains(calendarSearch, ignoreCase = true) ||
                    it.content.contains(calendarSearch, ignoreCase = true)
        }
    }

    val monthName = remember(calendarMonth) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        sdf.format(calendarMonth.time)
    }

    val daysInMonth = remember(calendarMonth) {
        val list = mutableListOf<Calendar?>()
        val temp = calendarMonth.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK)
        val maxDays = temp.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1 until firstDayOfWeek) {
            list.add(null)
        }
        for (d in 1..maxDays) {
            val cell = temp.clone() as Calendar
            cell.set(Calendar.DAY_OF_MONTH, d)
            list.add(cell)
        }
        list
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            calendarMonth = Calendar.getInstance()
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("TODAY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = {
                            val prev = calendarMonth.clone() as Calendar
                            prev.add(Calendar.MONTH, -1)
                            calendarMonth = prev
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "Prev", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            val next = calendarMonth.clone() as Calendar
                            next.add(Calendar.MONTH, 1)
                            calendarMonth = next
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, "Next", modifier = Modifier.size(18.dp))
                    }
                }
            }

            val weekRow = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                weekRow.forEach { char ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val chunks = daysInMonth.chunked(7)
                chunks.forEach { weekDays ->
                    val paddedWeekDays = weekDays.toMutableList()
                    while (paddedWeekDays.size < 7) {
                        paddedWeekDays.add(null)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        paddedWeekDays.forEachIndexed { index, cellCal ->
                            val isWeeklyOff = weeklyOff.contains(weekRow[index])
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cellCal == null) {
                                    Box(modifier = Modifier.size(36.dp))
                                } else {
                                    val cellDateStr = sdfStr.format(cellCal.time)
                                    
                                    val matchedHoliday = holidays.find { cellDateStr >= it.startDate && cellDateStr <= it.endDate }
                                    val isManualHoliday = matchedHoliday != null
                                    
                                    val isHoliday = isManualHoliday || isWeeklyOff
                                    
                                    val cellEpochDay = remember(cellDateStr) { localDateToEpochDay(cellDateStr) }
                                    val dayEvents = remember(cellEpochDay, datedEvents) {
                                        if (cellEpochDay == null) emptyList()
                                        else datedEvents.filter { it.eventEpochDay == cellEpochDay }
                                    }
                                    val isEventDay = !isHoliday && dayEvents.isNotEmpty()
                                    
                                    val isToday = remember(cellDateStr) {
                                        val now = Calendar.getInstance()
                                        sdfStr.format(now.time) == cellDateStr
                                    }

                                    val cellColor = when {
                                        isHoliday -> MaterialTheme.colorScheme.errorContainer
                                        isEventDay -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> Color.Transparent
                                    }

                                    val textCol = when {
                                        isHoliday -> MaterialTheme.colorScheme.error
                                        isEventDay -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cellColor)
                                            .then(
                                                if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                else Modifier
                                            )
                                            .clickable {
                                                // Tap a day to view its events or add a new one.
                                                if (dayEvents.isNotEmpty()) {
                                                    selectedDay = cellEpochDay
                                                } else if (cellEpochDay != null) {
                                                    eventDateEpochDay = cellEpochDay
                                                    editingEvent = null
                                                    showEventDialog = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cellCal.get(Calendar.DAY_OF_MONTH).toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isHoliday || isEventDay) FontWeight.Bold else FontWeight.Normal,
                                            color = textCol
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Holidays / Events toggle with live counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = listMode == "Holidays",
                    onClick = { listMode = "Holidays" },
                    label = { Text("Holidays (${holidays.size})") }
                )
                FilterChip(
                    selected = listMode == "Events",
                    onClick = { listMode = "Events" },
                    label = { Text("Events (${datedEvents.size})") }
                )
            }

            // Search bar scoped to the active list mode
            OutlinedTextField(
                value = calendarSearch,
                onValueChange = { calendarSearch = it },
                placeholder = {
                    Text(if (listMode == "Holidays") "Search holidays..." else "Search events...")
                },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (listMode == "Holidays") {
                if (filteredHolidays.isEmpty()) {
                    Text(
                        text = if (calendarSearch.isBlank()) "No holidays scheduled." else "No holidays match \"$calendarSearch\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredHolidays.forEach { hol ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = hol.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "${hol.startDate} to ${hol.endDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (filteredEvents.isEmpty()) {
                    Text(
                        text = if (calendarSearch.isBlank()) "No events yet — tap a calendar day to add one." else "No events match \"$calendarSearch\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredEvents.forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = formatEpochDay(event.eventEpochDay ?: 0L),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    if (event.content.isNotBlank()) {
                                        Text(
                                            text = event.content,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingEvent = event
                                            eventDateEpochDay = event.eventEpochDay
                                            showEventDialog = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Event",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { eventToDelete = event },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Event",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog shown when a day with existing events is tapped.
    selectedDay?.let { epochDay ->
        val dayEvents = datedEvents.filter { it.eventEpochDay == epochDay }
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = { Text("Events on ${formatEpochDay(epochDay)}", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dayEvents.isEmpty()) {
                        Text("No events on this day.")
                    }
                    dayEvents.forEach { event ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(event.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                if (event.content.isNotBlank()) {
                                    Text(
                                        event.content,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        selectedDay = null
                                        editingEvent = event
                                        eventDateEpochDay = event.eventEpochDay
                                        showEventDialog = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Edit Event", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        selectedDay = null
                                        eventToDelete = event
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Delete Event", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDay = null
                        editingEvent = null
                        eventDateEpochDay = epochDay
                        showEventDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Event")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDay = null }) { Text("Close") }
            }
        )
    }

    if (showEventDialog) {
        EventAddEditDialog(
            existing = editingEvent,
            initialEpochDay = eventDateEpochDay,
            onDismiss = { showEventDialog = false },
            onSave = { id, title, content, epochDay ->
                viewModel.saveDatedEvent(
                    id = id,
                    title = title,
                    content = content,
                    eventEpochDay = epochDay
                )
                showEventDialog = false
            }
        )
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete '${event.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(event)
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun EventAddEditDialog(
    existing: TeacherNoteEntity?,
    initialEpochDay: Long?,
    onDismiss: () -> Unit,
    onSave: (id: Int, title: String, content: String, epochDay: Long) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var content by remember { mutableStateOf(existing?.content ?: "") }
    var dateText by remember {
        mutableStateOf(existing?.eventEpochDay?.let { epochDayToDateString(it) } ?: initialEpochDay?.let { epochDayToDateString(it) } ?: "")
    }

    val parsedEpochDay = localDateToEpochDay(dateText)
    val valid = title.isNotBlank() && parsedEpochDay != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Calendar Event" else "Edit Calendar Event") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (yyyy-mm-dd) *") },
                    supportingText = {
                        Text(
                            if (parsedEpochDay != null) formatEpochDay(parsedEpochDay) else "Use format 2025-04-13",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    isError = dateText.isNotBlank() && parsedEpochDay == null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val epochDay = parsedEpochDay
                    if (epochDay != null && title.isNotBlank()) {
                        onSave(existing?.id ?: 0, title.trim(), content.trim(), epochDay)
                    }
                },
                enabled = valid
            ) {
                Text("Save Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
