package com.example.ui.screens

import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ClassEntity
import com.example.data.entity.HomeworkEntity
import com.example.data.entity.TeacherProfileEntity
import com.example.ui.ClassFlowViewModel
import com.example.ui.HomeworkReminderState
import com.example.ui.getStartOfTodayMillis
import com.example.ui.todayEpochDay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ClassFlowViewModel,
    onNavigateToClasses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    
    val selectedDay by viewModel.selectedDayOfWeek.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val currentDayOfWeek by viewModel.currentDayOfWeek.collectAsStateWithLifecycle()
    val classesForDay by viewModel.classesForSelectedDay.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val holidays by viewModel.allHolidays.collectAsStateWithLifecycle(initialValue = emptyList())
    val onboardingFinished by viewModel.onboardingFinished.collectAsStateWithLifecycle()
    val teacherProfile by viewModel.teacherProfile.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showOnboarding by remember { mutableStateOf(value = false) }

    LaunchedEffect(onboardingFinished) {
        if (onboardingFinished == false) {
            showOnboarding = true
        } else if (onboardingFinished == true) {
            showOnboarding = false
        }
    }
    
    val dateForDay = remember(selectedDay) {
        val cal = Calendar.getInstance()
        val todayDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val currentDayIdx = when (todayDayOfWeek) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        val diff = selectedDay - currentDayIdx
        cal.add(Calendar.DAY_OF_YEAR, diff)
        cal.time
    }
    
    val classLogsForDay by viewModel.classLogsForSelectedDate.collectAsStateWithLifecycle()
    
    val weeklyHolidays by viewModel.weeklyHolidays.collectAsStateWithLifecycle(emptySet())
    val activeHoliday = remember(dateForDay, holidays, weeklyHolidays) {
        CalendarUtils.getHolidayDetails(dateForDay, holidays, weeklyHolidays)
    }
    
    val homeworkReminders by viewModel.smartHomeworkReminders.collectAsStateWithLifecycle(initialValue = emptyList())
    val silencerEnabled by viewModel.silencerEnabled.collectAsStateWithLifecycle(initialValue = false)
    val teachers by viewModel.allTeachers.collectAsStateWithLifecycle(initialValue = emptyList())
    val allHomework by viewModel.allHomework.collectAsStateWithLifecycle(initialValue = emptyList())
    val allClasses by viewModel.allClasses.collectAsStateWithLifecycle(initialValue = emptyList())

    // Currently selected class to give homework/classwork notes
    var activeInteractionClass by remember { mutableStateOf<ClassEntity?>(null) }
    var inspectingHomework by remember { mutableStateOf<Pair<com.example.data.entity.HomeworkEntity, ClassEntity>?>(null) }
    var editingHomework by remember { mutableStateOf<HomeworkEntity?>(null) }
    
    // Calculated fields
    val currentTotalMin = (currentTime.hour * 60) + currentTime.minute
    
    // Holidays (saved ranges or weekly off-days) suppress the whole day schedule.
    val displayClasses = if (activeHoliday != null) emptyList() else classesForDay

    val activeClass = remember(displayClasses, selectedDay, currentDayOfWeek, currentTime) {
        displayClasses.find {
            selectedDay == currentDayOfWeek && currentTotalMin in it.startTimeMinutes..it.endTimeMinutes
        }
    }

    val selectedDateMillis by viewModel.selectedDateMillis.collectAsStateWithLifecycle()

    val pendingHomeworkForDay = remember(homeworkReminders, selectedDateMillis) {
        val selectedStart = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val selectedEnd = selectedStart + 86400000L

        homeworkReminders.filter { 
            val isCreatedOnSelectedDay = it.homework.createdDateMillis in selectedStart..<selectedEnd
            val isCheckingOnSelectedDay = it.homework.checkingDateMillis != null && it.homework.checkingDateMillis!! in selectedStart..<selectedEnd
            isCreatedOnSelectedDay || isCheckingOnSelectedDay
        }
    }


    if (showOnboarding) {
        OnboardingDialog(viewModel) { showOnboarding = false }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            HeaderSection(viewModel, teacherProfile?.name ?: "Teacher")
        }

        // Horizontal Calendar day selector
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                CalendarRow(
                    selectedDateMillis = viewModel.selectedDateMillis.collectAsStateWithLifecycle().value,
                    onSelectDate = { viewModel.selectDate(it) }
                )
                
                Spacer(Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { viewModel.selectDate(getStartOfTodayMillis()) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Today, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TODAY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        if (activeHoliday != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BeachAccess,
                                contentDescription = "Holiday",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SCHOOL HOLIDAY: ${activeHoliday.title.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            activeHoliday.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "All regular academic classes are fully suspended.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Selected Day info indicator
        item {
            val dayName = if (activeHoliday != null) "Holiday" else getDayName(selectedDay)
            val isToday = selectedDay == currentDayOfWeek
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isToday) "TODAY'S SCHEDULE ($dayName)" else "$dayName'S SCHEDULE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
            }
        }

        // Live Class Card / Quick Silencer component
        item {
            AnimatedContent(
                targetState = activeClass,
                label = "active_class_card"
            ) { liveClass ->
                if (liveClass != null) {
                    val hwPending = allHomework.filter { it.classId == liveClass.id && !it.isCompleted }
                    
                    val daysOfWk = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    val nextClassOcc = allClasses.asSequence()
                        .filter { it.name == liveClass.name && it.id != liveClass.id }
                        .minByOrNull {
                            val cWeekTime = (it.dayOfWeek - 1) * 24 * 60 + it.startTimeMinutes
                            val currWeekTime = (currentDayOfWeek - 1) * 24 * 60 + currentTotalMin
                            var diff = cWeekTime - currWeekTime
                            if (diff <= 0) diff += 7 * 24 * 60
                            diff
                        } ?: allClasses.find { it.id == liveClass.id }
                    
                    val nextText = nextClassOcc?.let {
                        val dName = daysOfWk[it.dayOfWeek - 1]
                        "NEXT ${liveClass.name.uppercase()}: $dName at ${it.startTime}"
                    }

                    LiveClassCard(
                        classEntity = liveClass,
                        silencerEnabled = silencerEnabled,
                        pendingHomework = hwPending,
                        nextClassText = nextText,
                        onToggleSilencer = {
                            viewModel.toggleSilencer(!silencerEnabled)
                            Toast.makeText(
                                context,
                                if (!silencerEnabled) "Device Silenced on schedule" else "Ringer Restored",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onClick = { activeInteractionClass = liveClass }
                    )
                } else {
                    val nextClass = displayClasses.sortedBy { it.startTimeMinutes }.find { it.startTimeMinutes > currentTotalMin }
                    NoActiveClassCard(
                        silencerEnabled = silencerEnabled,
                        nextClass = nextClass,
                        onToggleSilencer = { viewModel.toggleSilencer(!silencerEnabled) }
                    )
                }
            }
        }

        // Prominent Smart Homework Alerts
        if (pendingHomeworkForDay.isNotEmpty()) {
            item {
                Text(
                    text = "PENDING TASK ALERTS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF7D5260),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            items(pendingHomeworkForDay, key = { "homework_${it.homework.id}" }) { reminder ->
                SmartHomeworkCard(
                    reminder = reminder,
                    selectedDateMillis = selectedDateMillis,
                    onClick = { inspectingHomework = Pair(reminder.homework, reminder.classEntity) }
                )
            }
        }

        // All classes scroll list for selected day
        item {
            Text(
                text = "ALL SCHEDULED PERIODS",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        if (displayClasses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Classes Scheduled",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select another day or tap Settings below to build custom repeating sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.clickable { onNavigateToClasses() },
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val sortedClasses = displayClasses.sortedBy { classEntity ->
                val startTimeMin = classEntity.startTimeMinutes
                val endTimeMin = classEntity.endTimeMinutes
                val isToday = selectedDay == currentDayOfWeek
                
                when {
                    !isToday -> 1 // SCHEDULED
                    currentTotalMin in startTimeMin..endTimeMin -> 0 // ACTIVE
                    currentTotalMin < startTimeMin -> 1 // UPCOMING
                    else -> 2 // COMPLETED
                }
            }
            items(sortedClasses, key = { "class_${it.id}" }) { classEntity ->
                val teacher = teachers.find { it.id == classEntity.teacherId }
                val hwPending = allHomework.filter { 
                    val hwClass = allClasses.find { c -> c.id == it.classId }
                    (it.classId == classEntity.id || hwClass?.name == classEntity.name) && !it.isCompleted 
                }
                val statusRecord = classLogsForDay.find { it.classId == classEntity.id }?.status
                ClassScheduleItem(
                    classEntity = classEntity,
                    teacherName = teacher?.name ?: "Vance Daily",
                    currentTimeMinutes = currentTotalMin,
                    isToday = selectedDay == currentDayOfWeek,
                    pendingHomework = hwPending,
                    selectedDateMillis = selectedDateMillis,
                    isHoliday = activeHoliday != null,
                    classStatus = statusRecord,
                    onClick = { activeInteractionClass = classEntity },
                    onInspectHomework = { hw -> inspectingHomework = Pair(hw, classEntity) }
                )
            }
        }
    }

    // Classroom Work & Assignment Management modal
    if (activeInteractionClass != null) {
        val currentStatus = classLogsForDay.find { it.classId == activeInteractionClass!!.id }?.status ?: "Completed"
        ClassroomInteractionDialog(
            viewModel = viewModel,
            classEntity = activeInteractionClass!!,
            initialStatus = currentStatus,
            onDismiss = { activeInteractionClass = null },
            onConfirm = { taskTitle, taskDesc, clNotes, checkDate, imageUri, classStatus ->
                // Save class status
                viewModel.logClassStatus(
                    classId = activeInteractionClass!!.id,
                    dateMillis = viewModel.selectedDateMillis.value,
                    status = classStatus
                )
                
                // Save homework or classwork note if provided
                if (taskTitle.isNotBlank() || taskDesc.isNotBlank() || clNotes.isNotBlank()) {
                    val finalTitle = taskTitle.ifBlank { "Classroom Activity" }
                    val finalDesc = if (taskDesc.isNotBlank()) taskDesc else "Check classwork note"
                    viewModel.saveHomework(
                        classId = activeInteractionClass!!.id,
                        title = finalTitle,
                        description = finalDesc,
                        classworkNote = clNotes,
                        checkingDateMillis = checkDate,
                        imageUri = imageUri
                    )
                }
                activeInteractionClass = null
                Toast.makeText(context, "Session Logged!", Toast.LENGTH_SHORT).show()
            }
        )
    }
    // Homework Inspection Dialog
    if (inspectingHomework != null) {
        HomeworkEvaluationDialog(
            homework = inspectingHomework!!.first,
            classEntity = inspectingHomework!!.second,
            viewModel = viewModel,
            coroutineScope = coroutineScope,
            snackbarHostState = snackbarHostState,
            onDismiss = { inspectingHomework = null },
            onEdit = { 
                editingHomework = inspectingHomework!!.first
                inspectingHomework = null 
            }
        )
    }

    if (editingHomework != null) {
        HomeworkEditDialog(
            homework = editingHomework!!,
            viewModel = viewModel,
            onDismiss = { editingHomework = null },
            allClasses = allClasses
        )
    }
}

@Composable
fun HomeworkEvaluationDialog(
    homework: com.example.data.entity.HomeworkEntity,
    classEntity: ClassEntity,
    viewModel: ClassFlowViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val students by viewModel.getStudentsForClass(classEntity.name).collectAsStateWithLifecycle(emptyList())
    val submissions by viewModel.getSubmissionsForHomework(homework.id).collectAsStateWithLifecycle(emptyList())

    val pastNotDoneCounts = remember { mutableStateMapOf<Int, Int>() }
    LaunchedEffect(students) {
        students.forEach { s ->
            val count = viewModel.getPastNotDoneCount(s.id)
            pastNotDoneCounts[s.id] = count
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Evaluate Homework",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = homework.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (homework.description.isNotBlank()) {
                    Text(
                        text = "HW: ${homework.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!homework.classworkNote.isNullOrBlank()) {
                    Text(
                        text = "CW Note: ${homework.classworkNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(students) { student ->
                        val sub = submissions.find { it.studentId == student.id }
                        val currentStatus = sub?.status ?: "done"
                        val notDoneCount = pastNotDoneCounts[student.id] ?: 0

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Text(
                                student.name,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            if (notDoneCount > 0) {
                                Text(
                                    "🚨 Missing previous homework: $notDoneCount time(s)",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("done", "half", "not done").forEach { status ->
                                    FilterChip(
                                        selected = currentStatus == status,
                                        onClick = { viewModel.saveSubmission(homework.id, student.id, status) },
                                        label = { Text(status, fontSize = 10.sp) },
                                        modifier = Modifier.scale(0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            var showDeleteConfirmation by remember { mutableStateOf(false) }
            Row {
                TextButton(
                    onClick = onEdit
                ) {
                    Text("Edit Task")
                }
                
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Task")
                }
            }

            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text("Delete Homework") },
                    text = { Text("Are you sure you want to delete this homework?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteHomework(homework)
                                showDeleteConfirmation = false
                                onDismiss()
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Homework deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.saveHomework(
                                            id = 0,
                                            classId = homework.classId,
                                            title = homework.title,
                                            description = homework.description,
                                            isCompleted = homework.isCompleted,
                                            notes = homework.notes,
                                            imageUri = homework.imageUri,
                                            checkingDateMillis = homework.checkingDateMillis,
                                            classworkNote = homework.classworkNote
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    )
}

@Composable
fun HeaderSection(viewModel: ClassFlowViewModel, teacherName: String) {
    var showOffsetDialog by remember { mutableStateOf(false) }
    var eventStatusTarget by remember { mutableStateOf<com.example.data.entity.TeacherNoteEntity?>(null) }
    val nepaliDateOffset by viewModel.nepaliDateOffset.collectAsStateWithLifecycle()
    val datedEvents by viewModel.allDatedEvents.collectAsStateWithLifecycle(initialValue = emptyList())

    // Next upcoming calendar event, shown right under the English date line.
    val nextEvent = remember(datedEvents) {
        val today = todayEpochDay()
        datedEvents.filter { it.eventEpochDay != null && it.eventEpochDay >= today }
            .minByOrNull { it.eventEpochDay ?: Long.MAX_VALUE }
    }
    val nextEventDateStr = remember(nextEvent) {
        nextEvent?.let {
            java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d")
                .format(java.time.LocalDate.ofEpochDay(it.eventEpochDay!!))
        }
    }
    
    val today = remember { Date() }
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val nepaliDate = remember(today, nepaliDateOffset) { 
        val cal = Calendar.getInstance()
        cal.time = today
        cal.add(Calendar.DAY_OF_YEAR, nepaliDateOffset)
        CalendarUtils.getNepaliDate(cal.time)
    }
    val englishDateStr = remember(today) { 
        val sdf = java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)
        sdf.format(today)
    }

    if (showOffsetDialog) {
        AlertDialog(
            onDismissRequest = { showOffsetDialog = false },
            title = { Text("Adjust Nepali Date Offset") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Current offset: $nepaliDateOffset days")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(onClick = { viewModel.adjustNepaliDateOffset(nepaliDateOffset - 1) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        IconButton(onClick = { viewModel.adjustNepaliDateOffset(nepaliDateOffset + 1) }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOffsetDialog = false }) { Text("Close") }
            }
        )
    }

    eventStatusTarget?.let { evt ->
        val evtDateStr = if (evt.eventEpochDay != null) {
            java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d").format(java.time.LocalDate.ofEpochDay(evt.eventEpochDay))
        } else ""
        AlertDialog(
            onDismissRequest = { eventStatusTarget = null },
            title = { Text(evt.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Event on $evtDateStr • Current status: ${evt.eventStatus.ifBlank { "Pending" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            viewModel.setEventStatus(evt, "COMPLETED")
                            eventStatusTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark Success / Completed")
                    }
                    Button(
                        onClick = {
                            viewModel.setEventStatus(evt, "CANCELLED")
                            eventStatusTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark Cancelled")
                    }
                    Button(
                        onClick = {
                            viewModel.setEventStatus(evt, "FAILED")
                            eventStatusTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark Failed")
                    }
                    if (evt.eventStatus.isNotBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.setEventStatus(evt, "")
                                eventStatusTarget = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Reset to Pending") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { eventStatusTarget = null }) { Text("Close") }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.40f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$greeting, $teacherName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = englishDateStr,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            nextEvent?.let { evt ->
                val evtStatus = evt.eventStatus
                val settled = evtStatus == "COMPLETED" || evtStatus == "CANCELLED" || evtStatus == "FAILED"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { eventStatusTarget = evt }
                        .padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = when (evtStatus) {
                            "COMPLETED" -> Icons.Default.CheckCircle
                            "CANCELLED", "FAILED" -> Icons.Default.Cancel
                            else -> Icons.Default.Event
                        },
                        contentDescription = "Event status",
                        tint = when {
                            evtStatus == "COMPLETED" -> Color(0xFF2E7D32)
                            settled -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buildString {
                            append("Next event: ${evt.title} • $nextEventDateStr")
                            if (evtStatus.isNotBlank()) {
                                append(" • ")
                                append(evtStatus.lowercase().replaceFirstChar(Char::uppercase))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (settled) TextDecoration.LineThrough else null
                        )
                    )
                }
            }
        }
        // Nepali date badge — a wavy sun-like circle holding the BS month,
        // a heavy day number and a small Nepali year underneath.
        WavyNepaliDateBadge(
            month = nepaliDate.monthName,
            day = com.example.ui.screens.CalendarUtils.toNepaliNumber(nepaliDate.day),
            year = com.example.ui.screens.CalendarUtils.toNepaliNumber(nepaliDate.year),
            onClick = { showOffsetDialog = true }
        )
    }
}
/** Wavy sun-like circle badge showing the Bikram Sambat date with a small year strip. */
@Composable
fun WavyNepaliDateBadge(
    month: String,
    day: String,
    year: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val border = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val content = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = modifier
            .size(98.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = size.minDimension * 0.42f
            val amp = size.minDimension * 0.028f
            val bumps = 7
            val path = Path()
            val steps = 200
            for (i in 0..steps) {
                val angle = (i.toFloat() / steps) * (2f * PI.toFloat())
                val r = baseR + amp * sin(angle * bumps)
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = container)
            drawPath(path, color = border, style = Stroke(width = 3.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = month,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = content
            )
            Text(
                text = day,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = content
                )
            )
            Text(
                text = year,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = content,
                modifier = Modifier
                    .background(border.copy(alpha = 0.18f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
fun CalendarRow(
    selectedDateMillis: Long,
    onSelectDate: (Long) -> Unit
) {
    val dates = remember {
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val list = mutableListOf<Long>()
        today.add(java.util.Calendar.DAY_OF_YEAR, -14)
        repeat(29) {
            list.add(today.timeInMillis)
            today.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = 11)

    LaunchedEffect(selectedDateMillis) {
        val todayStart = getStartOfTodayMillis()
        if (selectedDateMillis == todayStart) {
            listState.animateScrollToItem(11)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().testTag("calendar_days_row"),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(dates) { dateMillis ->
            val isSelected = selectedDateMillis == dateMillis
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
            val dayLabel = java.text.SimpleDateFormat("EEE", java.util.Locale.US).format(cal.time).uppercase()
            val dayNumStr = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
            
            val isRealToday = dateMillis == java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary 
                        else Color.White
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary 
                                else if (isRealToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onSelectDate(dateMillis) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = dayNumStr,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (isRealToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LiveClassCard(
    classEntity: ClassEntity,
    silencerEnabled: Boolean,
    pendingHomework: List<HomeworkEntity>,
    nextClassText: String? = null,
    onToggleSilencer: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Text(
                        text = "LIVE NOW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
                
                Text(
                    text = "${classEntity.startTime} — ${classEntity.endTime}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Column {
                Text(
                    text = classEntity.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Room ${classEntity.roomNumber} • ${classEntity.subject}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            if (pendingHomework.isNotEmpty()) {
                val todayStart = com.example.ui.getStartOfTodayMillis()
                val givenTodayHw = pendingHomework.find { it.createdDateMillis >= todayStart && it.createdDateMillis < todayStart + 86400000L }
                val prevHomework = pendingHomework.filter { it.createdDateMillis < todayStart && (it.checkingDateMillis == null || it.checkingDateMillis < todayStart) }

                if (prevHomework.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFD8E4))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFF31111D), modifier = Modifier.size(16.dp))
                            Column {
                                Text(
                                    text = "PREVIOUS HOMEWORK DUE:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF31111D)
                                )
                                Text(
                                    text = prevHomework.first().title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF31111D)
                                )
                            }
                        }
                    }
                }

                if (givenTodayHw != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2F1))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00695C), modifier = Modifier.size(16.dp))
                            Column {
                                Text(
                                    text = "LOGGED TODAY:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00695C)
                                )
                                Text(
                                    text = givenTodayHw.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF00695C)
                                )
                            }
                        }
                    }
                }
            }

            if (nextClassText != null) {
                Text(
                    text = nextClassText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleSilencer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = if (silencerEnabled) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (silencerEnabled) "Device Silenced" else "Silence Class",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    "Tap to Log Work",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NoActiveClassCard(
    silencerEnabled: Boolean,
    nextClass: ClassEntity? = null,
    onToggleSilencer: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NO ACTIVE PERIOD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Text(
                text = "Study/Prep Time",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            if (nextClass != null) {
                Text(
                    text = "NEXT: ${nextClass.name} at ${nextClass.startTime}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                Text(
                    text = "Prepare homework worksheets and manage lesson journals. Select any scheduled period below to write class notes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-silence classes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = silencerEnabled,
                    onCheckedChange = onToggleSilencer,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
        }
    }
}

@Composable
fun SmartHomeworkCard(
    reminder: HomeworkReminderState,
    selectedDateMillis: Long,
    onClick: () -> Unit
) {
    val selectedStart = java.util.Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val selectedEnd = selectedStart + 86400000L
    
    val isCreatedToday = reminder.homework.createdDateMillis in selectedStart..<selectedEnd
    
    val containerColor = if (isCreatedToday) Color(0xFFE8F5E9) else Color(0xFFFFE082)
    val borderColor = if (isCreatedToday) Color(0xFFC8E6C9) else Color(0xFFFFD54F)
    val contentColor = if (isCreatedToday) Color(0xFF1B5E20) else Color(0xFFE65100)
    val headerText = if (isCreatedToday) "HOMEWORK GIVEN FOR NEXT CLASS" else "HOMEWORK TO CHECK"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$headerText: ${reminder.classEntity.name}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = contentColor,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = reminder.homework.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = reminder.homework.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = contentColor.copy(alpha = 0.8f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!reminder.homework.classworkNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Classwork: ${reminder.homework.classworkNote}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (reminder.homework.imageUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = reminder.homework.imageUri,
                        contentDescription = "Attached Lab Guide",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }
}

/** Strips prefixes like "Class " and returns a compact 4-char badge label such as "1A". */
private fun shortClassName(name: String): String {
    return name
        .trim()
        .removePrefix("Class ")
        .removePrefix("CLASS ")
        .removePrefix("class ")
        .take(4)
}

@Composable
fun ClassScheduleItem(
    classEntity: ClassEntity,
    teacherName: String,
    currentTimeMinutes: Int,
    isToday: Boolean,
    pendingHomework: List<HomeworkEntity>,
    selectedDateMillis: Long,
    isHoliday: Boolean = false,
    classStatus: String? = null,
    onClick: () -> Unit,
    onInspectHomework: (com.example.data.entity.HomeworkEntity) -> Unit = {}
) {
    val startTimeMin = classEntity.startTimeMinutes
    val endTimeMin = classEntity.endTimeMinutes

    val selectedStart = java.util.Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val selectedEnd = selectedStart + 86400000L

    val todayStart = com.example.ui.getStartOfTodayMillis()

    val status = when {
        selectedStart < todayStart -> ClassStatus.COMPLETED
        selectedStart >= todayStart + 86400000L -> ClassStatus.SCHEDULED
        currentTimeMinutes in startTimeMin..endTimeMin -> ClassStatus.ACTIVE
        currentTimeMinutes < startTimeMin -> ClassStatus.UPCOMING
        else -> ClassStatus.COMPLETED
    }

    val itemBg = if (isHoliday) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    } else {
        when (status) {
            ClassStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ClassStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> Color.White
        }
    }

    val borderCol = if (isHoliday) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    } else {
        when (status) {
            ClassStatus.ACTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(itemBg)
            .border(1.dp, borderCol, RoundedCornerShape(24.dp))
            .clickable(enabled = status != ClassStatus.SCHEDULED) { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact circular leading thumbnail showing the class name (e.g. "1A", "10B")
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (status == ClassStatus.ACTIVE) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = shortClassName(classEntity.name),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    ),
                    color = if (status == ClassStatus.ACTIVE) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = classEntity.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isHoliday || status == ClassStatus.COMPLETED) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) 
                                    else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isHoliday || status == ClassStatus.COMPLETED) TextDecoration.LineThrough else null
                        )
                    )
                    
                    if (classStatus != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (classStatus == "Completed") Color(0xFFC8E6C9) else Color(0xFFFFCCBC),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = classStatus.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (classStatus == "Completed") Color(0xFF2E7D32) else Color(0xFFD84315),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${classEntity.startTime} - ${classEntity.endTime} • Room ${classEntity.roomNumber} | Sub: ${classEntity.subject}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
            if (pendingHomework.isNotEmpty() && !isHoliday) {
                Spacer(modifier = Modifier.height(6.dp))
                
                val givenTodayHw = pendingHomework.find { it.createdDateMillis >= selectedStart && it.createdDateMillis < selectedEnd }
                val prevHomework = pendingHomework.filter { it.createdDateMillis < selectedStart && (it.checkingDateMillis == null || it.checkingDateMillis < selectedStart) }

                if (givenTodayHw != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0F2F1))
                            .clickable { onInspectHomework(givenTodayHw) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Homework Given",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00695C)
                        )
                    }
                }
                
                val hwToCheckToday = pendingHomework.filter { 
                    it.checkingDateMillis != null && it.checkingDateMillis >= selectedStart && it.checkingDateMillis < selectedEnd 
                }

                if (hwToCheckToday.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .clickable { onInspectHomework(hwToCheckToday.first()) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Homework to check",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                if (prevHomework.isNotEmpty() && hwToCheckToday.isEmpty() && givenTodayHw == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFD8E4))
                            .clickable { onInspectHomework(prevHomework.first()) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFF31111D), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "HW to Check from prev class: ${prevHomework.first().title}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF31111D)
                            )
                        }
                    }
                }
            }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isHoliday) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        } else if (classStatus != null) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            when (status) {
                                ClassStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                                ClassStatus.UPCOMING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ClassStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
                                ClassStatus.SCHEDULED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isHoliday) {
                        "HOLIDAY"
                    } else if (classStatus != null) {
                        classStatus.uppercase()
                    } else {
                        when (status) {
                            ClassStatus.ACTIVE -> "ACTIVE"
                            ClassStatus.UPCOMING -> "UPCOMING"
                            ClassStatus.COMPLETED -> "COMPLETED"
                            ClassStatus.SCHEDULED -> "SCHEDULED"
                        }
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isHoliday) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else if (classStatus != null) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            when (status) {
                                ClassStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                                ClassStatus.UPCOMING -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        }
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClassroomInteractionDialog(
    viewModel: ClassFlowViewModel,
    classEntity: ClassEntity,
    initialStatus: String = "Completed",
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, clNotes: String, checkingDate: Long, imageUri: String?, classStatus: String) -> Unit,
    allClasses: List<ClassEntity> = emptyList()
) {
    val context = LocalContext.current
    val students by viewModel.getStudentsForClass(classEntity.name).collectAsStateWithLifecycle(emptyList())

    var classStatus by remember { mutableStateOf(initialStatus.ifEmpty { "Completed" }) }
    var classworkNote by remember { mutableStateOf("") }
    var homeworkTitle by remember { mutableStateOf("") }
    var homeworkDesc by remember { mutableStateOf("") }
    var checkingDateOption by remember { mutableIntStateOf(0) } // 0 = Next Class, 1 = Tomorrow, 2 = Custom (3)
    
    var customCheckingDateMillis by remember { mutableStateOf<Long?>(null) }
    
    var attachedImageUri by remember { mutableStateOf<String?>(null) }
    
    // Pick from gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri.toString()
        }
    }
    
    // Take picture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "hw_snap_${System.currentTimeMillis()}.jpg")
                val fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
                fos.close()
                attachedImageUri = Uri.fromFile(file).toString()
                Toast.makeText(context, "Photo captured!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error writing camera image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val checkingDateMillis = remember(checkingDateOption, customCheckingDateMillis, allClasses) {
        val calendar = Calendar.getInstance()
        when (checkingDateOption) {
            0 -> getNextClassMillis(classEntity.name, System.currentTimeMillis(), allClasses)
            1 -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.timeInMillis
            }
            3 -> customCheckingDateMillis ?: (System.currentTimeMillis() + 86400000)
            else -> calendar.timeInMillis + 86400000 * 7
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Classroom Journal: ${classEntity.name}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "CLASS STATUS:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Completed", "Half completed", "Cancelled", "Disturbed").forEach { status ->
                            FilterChip(
                                selected = classStatus == status,
                                onClick = { classStatus = status },
                                label = { Text(status, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                item {
                    Text(
                        text = "GIVE CLASSWORK NOTE:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = classworkNote,
                        onValueChange = { classworkNote = it },
                        placeholder = { Text("What syllabus outline was taught today?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
                
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                
                item {
                    Text(
                        text = "NEW HOMEWORK TASK:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = homeworkTitle,
                        onValueChange = { homeworkTitle = it },
                        label = { Text("Task Title *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = homeworkDesc,
                        onValueChange = { homeworkDesc = it },
                        label = { Text("Homework Guidelines & Exercises *") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                item {
                    Text(
                        text = "Check Deadline (Checking Date):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                    listOf("Next Class" to 0, "Tomorrow" to 1, "Custom" to 3).forEach { (lbl, idx) ->
                        FilterChip(
                            selected = checkingDateOption == idx,
                            onClick = { 
                                if (idx == 3) {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(context, { _, y, m, d ->
                                        customCheckingDateMillis = Calendar.getInstance().apply {
                                            set(y, m, d)
                                        }.timeInMillis
                                        checkingDateOption = 3
                                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                } else {
                                    checkingDateOption = idx 
                                }
                            },
                            label = { Text(lbl, fontSize = 11.sp) }
                        )
                    }
                    }
                    Text(
                        text = "Deadline: ${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(checkingDateMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                item {
                    Text(
                        text = "ATTACH EXERCISE PHOTO / DOCUMENT:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera", fontSize = 10.sp)
                        }
                        
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery", fontSize = 10.sp)
                        }
                    }
                }
                
                if (attachedImageUri != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = attachedImageUri,
                                    contentDescription = "Selected Homework snap",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { attachedImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                item {
                    Text(
                        text = "STUDENT ACTIVITY TRACKER:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (students.isEmpty()) {
                        Text("No students imported for this class.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            students.forEach { student ->
                                var showActivities by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = student.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            TextButton(onClick = { showActivities = !showActivities }) {
                                                Text(if (showActivities) "Hide" else "Log Behavior")
                                            }
                                        }
                                        
                                        if (showActivities) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val activities = listOf(
                                                    "Named by Monitor", "Impressed by Engaging", "Positive Curiosity", "Sent Outside", 
                                                    "Disturbed Class", "CW Not Done", "Ugly Behavior", 
                                                    "Got Prize", "Scoldings", "Went Out (Event)", 
                                                    "Was Sick", "Late Arrival", "No Uniform"
                                                )
                                                activities.forEach { act ->
                                                    AssistChip(
                                                        onClick = {
                                                            viewModel.recordStudentActivity(
                                                                studentId = student.id,
                                                                studentName = student.name,
                                                                className = classEntity.name,
                                                                activityType = act
                                                            )
                                                            Toast.makeText(context, "Logged: $act for ${student.name}", Toast.LENGTH_SHORT).show()
                                                            showActivities = false
                                                        },
                                                        label = { Text(act, fontSize = 9.sp) }
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(homeworkTitle, homeworkDesc, classworkNote, checkingDateMillis, attachedImageUri, classStatus)
                }
            ) {
                Text("Log Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HomeworkEditDialog(
    homework: com.example.data.entity.HomeworkEntity,
    viewModel: ClassFlowViewModel,
    onDismiss: () -> Unit,
    allClasses: List<ClassEntity> = emptyList()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(homework.title) }
    var description by remember { mutableStateOf(homework.description) }
    var classworkNote by remember { mutableStateOf(homework.classworkNote ?: "") }
    var attachedImageUri by remember { mutableStateOf(homework.imageUri) }
    var checkingDateMillis by remember { mutableStateOf(homework.checkingDateMillis) }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri.toString()
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "hw_edit_snap_${System.currentTimeMillis()}.jpg")
                val fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
                fos.close()
                attachedImageUri = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Homework", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Homework Guidelines *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = classworkNote,
                    onValueChange = { classworkNote = it },
                    label = { Text("Associated Classwork Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Check Deadline:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Next Class" to 0, "Tomorrow" to 1, "Custom" to 3).forEach { (lbl, idx) ->
                        AssistChip(
                            onClick = { 
                                val cal = Calendar.getInstance()
                                when (idx) {
                                    0 -> {
                                        val hwClass = allClasses.find { it.id == homework.classId }
                                        checkingDateMillis = if (hwClass != null) {
                                            getNextClassMillis(hwClass.name, System.currentTimeMillis(), allClasses)
                                        } else {
                                            System.currentTimeMillis() + 86400000 * 7
                                        }
                                    }
                                    1 -> {
                                        cal.add(Calendar.DAY_OF_YEAR, 1)
                                        checkingDateMillis = cal.timeInMillis
                                    }
                                    3 -> {
                                        DatePickerDialog(context, { _, y, m, d ->
                                            val c = Calendar.getInstance()
                                            c.set(y, m, d)
                                            checkingDateMillis = c.timeInMillis
                                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                    }
                                }
                            },
                            label = { Text(lbl, fontSize = 11.sp) }
                        )
                    }
                }
                checkingDateMillis?.let {
                    Text("Current Deadline: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))}", 
                         style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                
                Text("Update Photo Attachment:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera", fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery", fontSize = 10.sp)
                    }
                }
                
                if (attachedImageUri != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp))) {
                        coil.compose.AsyncImage(
                            model = attachedImageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { attachedImageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        viewModel.saveHomework(
                            id = homework.id,
                            classId = homework.classId,
                            title = title,
                            description = description,
                            isCompleted = homework.isCompleted,
                            notes = homework.notes,
                            imageUri = attachedImageUri,
                            checkingDateMillis = checkingDateMillis,
                            classworkNote = classworkNote
                        )
                        onDismiss()
                        Toast.makeText(context, "Homework Updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Title and Guidelines are required", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class ClassStatus {
    ACTIVE,
    UPCOMING,
    COMPLETED,
    SCHEDULED
}

fun getDayName(day: Int): String {
    return when (day) {
        1 -> "MONDAY"
        2 -> "TUESDAY"
        3 -> "WEDNESDAY"
        4 -> "THURSDAY"
        5 -> "FRIDAY"
        6 -> "SATURDAY"
        7 -> "SUNDAY"
        else -> ""
    }
}

fun getNextClassMillis(className: String, fromDateMillis: Long, allClasses: List<ClassEntity>): Long {
    val sameNamedClasses = allClasses.filter { it.name == className }
    if (sameNamedClasses.isEmpty()) return fromDateMillis + 86400000 * 7
    
    val classDays = sameNamedClasses.map { it.dayOfWeek }.toSet() // 1=Mon, 7=Sun
    
    val cal = Calendar.getInstance()
    cal.timeInMillis = fromDateMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    
    for (i in 1..14) {
        cal.add(Calendar.DAY_OF_YEAR, i)
        val dayOfWeekStandard = cal.get(Calendar.DAY_OF_WEEK)
        // Convert Calendar day (1=Sun, 2=Mon...) to standard java.time equivalent (1=Mon, 7=Sun)
        val dayOfWeekCustom = if (dayOfWeekStandard == Calendar.SUNDAY) 7 else dayOfWeekStandard - 1
        if (classDays.contains(dayOfWeekCustom)) {
            return cal.timeInMillis
        }
    }
    return fromDateMillis + 86400000 * 7
}
