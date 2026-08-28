package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.data.entity.ClassEntity
import com.example.data.entity.TeacherProfileEntity
import com.example.data.entity.ManagedClassEntity
import com.example.ui.ClassFlowViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ClassFlowViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedSettingGroup by remember { mutableStateOf<Int?>(null) }
    
    // Fetch state from ViewModel
    val teachers by viewModel.allTeachers.collectAsStateWithLifecycle(initialValue = emptyList())
    val classes by viewModel.allClasses.collectAsStateWithLifecycle(initialValue = emptyList())
    val managedClasses by viewModel.allManagedClasses.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val schoolStartTime by viewModel.schoolStartTime.collectAsStateWithLifecycle(initialValue = "08:00")
    val firstPeriodTime by viewModel.firstPeriodTime.collectAsStateWithLifecycle(initialValue = "08:30")
    val secondPeriodTime by viewModel.secondPeriodTime.collectAsStateWithLifecycle(initialValue = "09:20")
    val silencerEnabled by viewModel.silencerEnabled.collectAsStateWithLifecycle(initialValue = false)
    
    // Find first teacher profile representing the user
    val activeProfile = teachers.firstOrNull() ?: TeacherProfileEntity(
        id = 1,
        name = "Teacher",
        email = "teacher.edu",
        officeLocation = "Address-4, address",
        subjectSpecialty = "Classroom Teacher"
    )
    
    // Editing states
    var showClassDialog by remember { mutableStateOf(false) }
    var selectedClassForEdit by remember { mutableStateOf<ClassEntity?>(null) }
    var classToDelete by remember { mutableStateOf<ClassEntity?>(null) }
    var activeAddDay by remember { mutableIntStateOf(1) }
    
    var showProfileDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedSettingGroup == null) "Settings" else {
                            when (selectedSettingGroup) {
                                0 -> "Profile & Automation"
                                1 -> "Routine Setup"
                                2 -> "Data Backup"
                                3 -> "Subjects"
                                4 -> "Holidays"
                                else -> "Settings"
                            }
                        },
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    if (selectedSettingGroup != null) {
                        IconButton(onClick = { selectedSettingGroup = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            
            if (selectedSettingGroup == null) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SettingsMenuItem(
                            title = "Profile & Automation",
                            subtitle = "Update details, automation & timings",
                            icon = Icons.Default.AccountBox,
                            onClick = { selectedSettingGroup = 0 }
                        )
                    }
                    item {
                        SettingsMenuItem(
                            title = "Routine Setup",
                            subtitle = "Manage your weekly class timetable",
                            icon = Icons.Default.CalendarMonth,
                            onClick = { selectedSettingGroup = 1 }
                        )
                    }
                    item {
                        SettingsMenuItem(
                            title = "Subjects",
                            subtitle = "Manage taught subjects & classes",
                            icon = Icons.Default.Class,
                            onClick = { selectedSettingGroup = 3 }
                        )
                    }
                    item {
                        SettingsMenuItem(
                            title = "Holidays",
                            subtitle = "Set weekly off days & public holidays",
                            icon = Icons.Default.BeachAccess,
                            onClick = { selectedSettingGroup = 4 }
                        )
                    }
                    item {
                        SettingsMenuItem(
                            title = "Data Backup",
                            subtitle = "Export JSON or wipe internal data",
                            icon = Icons.Default.CloudSync,
                            onClick = { selectedSettingGroup = 2 }
                        )
                    }
                    item {
                        DeveloperInfoCard()
                    }
                }
            } else {
                AnimatedContent(
                    targetState = selectedSettingGroup,
                    label = "settings_tab_animation",
                    modifier = Modifier.weight(1f)
                ) { tabIdx ->
                    when (tabIdx) {
                        0 -> ProfileAndTimesTab(
                            profile = activeProfile,
                            schoolStartTime = schoolStartTime,
                            firstPeriodTime = firstPeriodTime,
                            secondPeriodTime = secondPeriodTime,
                            silencerEnabled = silencerEnabled,
                            onUpdateStartTime = { viewModel.updateSchoolStartTime(it) },
                            onUpdateFirstPeriod = { viewModel.updateFirstPeriodTime(it) },
                            onUpdateSecondPeriod = { viewModel.updateSecondPeriodTime(it) },
                            onToggleSilencer = { viewModel.toggleSilencer(it) },
                            onEditProfileClick = { showProfileDialog = true }
                        )
                        1 -> RoutineSetupTab(
                            classes = classes,
                            teachers = teachers,
                            onAddClassClick = { day ->
                                selectedClassForEdit = null
                                activeAddDay = day
                                showClassDialog = true
                            },
                            onEditClassClick = { classEntity ->
                                selectedClassForEdit = classEntity
                                activeAddDay = classEntity.dayOfWeek
                                showClassDialog = true
                            },
                            onDeleteClassClick = { classEntity ->
                                classToDelete = classEntity
                            },
                            onPrepopulateTemplate = { day ->
                                viewModel.prepopulateDayTemplate(day)
                                Toast.makeText(context, "Standard template loaded for this day!", Toast.LENGTH_SHORT).show()
                            },
                            onCopyRoutineClick = { day ->
                                viewModel.copyRoutineToAllDays(day)
                                Toast.makeText(context, "Routine copied to all days!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        2 -> DataBackupTab(viewModel = viewModel)
                        3 -> ManagedClassesTab(
                             viewModel = viewModel,
                             coroutineScope = coroutineScope,
                             snackbarHostState = snackbarHostState
                        )
                        4 -> HolidaysConfigTab(viewModel = viewModel)
                    }
                }
            }
        }
    }

    classToDelete?.let { classEntity ->
        SimpleConfirmationDialog(
            title = "Delete Class Period",
            text = "Are you sure you want to delete this ${classEntity.name} period?",
            onConfirm = {
                classToDelete = null
                viewModel.deleteClass(classEntity)
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Routine period deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.saveClass(
                            id = 0,
                            name = classEntity.name,
                            subject = classEntity.subject,
                            roomNumber = classEntity.roomNumber,
                            teacherId = classEntity.teacherId,
                            dayOfWeek = classEntity.dayOfWeek,
                            startTime = classEntity.startTime,
                            endTime = classEntity.endTime
                        )
                    }
                }
            },
            onDismiss = { classToDelete = null }
        )
    }

    if (showProfileDialog) {
        TeacherProfileDialog(
            profile = activeProfile,
            onDismiss = { showProfileDialog = false },
            onConfirm = { name, email, office, specialty, dob ->
                viewModel.saveTeacher(
                    id = activeProfile.id,
                    name = name,
                    email = email,
                    officeLocation = office,
                    subjectSpecialty = specialty,
                    dob = dob
                )
                showProfileDialog = false
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showClassDialog) {
        ClassEditSetupDialog(
            classEntity = selectedClassForEdit,
            defaultDayOfWeek = activeAddDay,
            teachers = teachers,
            managedClasses = managedClasses,
            onDismiss = { showClassDialog = false },
            onConfirm = { id, name, subject, room, teacherId, day, start, end ->
                viewModel.saveClass(
                    id = id,
                    name = name,
                    subject = subject,
                    roomNumber = room,
                    teacherId = teacherId,
                    dayOfWeek = day,
                    startTime = start,
                    endTime = end
                )
                showClassDialog = false
                Toast.makeText(context, "Weekly Routine Period Saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SettingsMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfileAndTimesTab(
    profile: TeacherProfileEntity,
    schoolStartTime: String,
    firstPeriodTime: String,
    secondPeriodTime: String,
    silencerEnabled: Boolean,
    onUpdateStartTime: (String) -> Unit = {},
    onUpdateFirstPeriod: (String) -> Unit = {},
    onUpdateSecondPeriod: (String) -> Unit = {},
    onToggleSilencer: (Boolean) -> Unit,
    onEditProfileClick: () -> Unit
) {
    
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Teacher Profile Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TEACHER PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = onEditProfileClick) {
                            Icon(Icons.Default.Edit, "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Subject Focus: ${profile.subjectSpecialty}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Email: ${profile.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Office: ${profile.officeLocation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val calculatedAge = remember(profile.dob) { calculateAgeInt(profile.dob) }
                    if (profile.dob.isNotBlank()) {
                        Text(
                            "DOB: ${profile.dob}" + (calculatedAge?.let { " (Age: $it Years)" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Silence card settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "AUTOMATED CONTEXT CONTROL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        "Help guarantee focus by silencing the device state ringer system automatically when you are inside active teaching periods.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mute Ringer During Class Hours", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = silencerEnabled,
                            onCheckedChange = onToggleSilencer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoutineSetupTab(
    classes: List<ClassEntity>,
    teachers: List<TeacherProfileEntity>,
    onAddClassClick: (day: Int) -> Unit,
    onEditClassClick: (ClassEntity) -> Unit,
    onDeleteClassClick: (ClassEntity) -> Unit,
    onPrepopulateTemplate: (Int) -> Unit,
    onCopyRoutineClick: (day: Int) -> Unit
) {
    var selectedDayFilter by remember { mutableStateOf(1) } // Default to Monday (1)
    
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    // Filter classes for the selected day in chronological order
    val filteredClasses = remember(classes, selectedDayFilter) {
        classes.filter { it.dayOfWeek == selectedDayFilter }
               .sortedBy { it.startTimeMinutes }
    }

    val fullDayName = when (selectedDayFilter) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Day"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // Day selection chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..7).forEach { d ->
                val dayName = dayNames[d - 1]
                val isSelected = selectedDayFilter == d
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { selectedDayFilter = d }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Header with active day and "Add Period" button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$fullDayName Routine",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            
            Button(
                onClick = { onAddClassClick(selectedDayFilter) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Period", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
            
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onCopyRoutineClick(selectedDayFilter) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy this routine for the whole week", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(10.dp))

        if (filteredClasses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "No periods set for $fullDayName",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Teachers can set routine day-by-day. Use the template flow below to quickly load a routine schedule like the one for Sunday (5 standard periods + administrative ledger duty + school break time).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Button(
                            onClick = { onPrepopulateTemplate(selectedDayFilter) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.School, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Prepopulate 5-Period Template", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(filteredClasses, key = { "routine_${it.id}" }) { item ->
                    val teacher = teachers.find { it.id == item.teacherId }
                    RoutineClassItemCard(
                        classEntity = item,
                        teacherName = teacher?.name ?: "Vance Daily",
                        onEdit = { onEditClassClick(item) },
                        onDelete = { onDeleteClassClick(item) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { onPrepopulateTemplate(selectedDayFilter) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Standard 5 Periods + BREAK Template", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun RoutineClassItemCard(
    classEntity: ClassEntity,
    teacherName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isBreak = classEntity.name.uppercase() == "BREAK" || classEntity.subject.uppercase() == "BREAK"
    
    val cardBg = if (isBreak) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val borderStroke = if (isBreak) {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isBreak) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Break",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (classEntity.name.contains("Ledger", ignoreCase = true)) {
                        Icon(
                            imageVector = Icons.Default.Task,
                            contentDescription = "Duty",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Class",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        classEntity.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (isBreak) {
                    Text(
                        "Interval • ${classEntity.startTime} — ${classEntity.endTime}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Text(
                        "Subject: ${classEntity.subject}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Time: ${classEntity.startTime} — ${classEntity.endTime}  |  Room ${classEntity.roomNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherProfileDialog(
    profile: TeacherProfileEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, office: String, specialty: String, dob: String) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var email by remember { mutableStateOf(profile.email) }
    var office by remember { mutableStateOf(profile.officeLocation) }
    var specialty by remember { mutableStateOf(profile.subjectSpecialty) }
    var dob by remember { mutableStateOf(profile.dob) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        dob = sdf.format(java.util.Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val ageStr = remember(dob) {
        val age = calculateAgeInt(dob)
        if (age == null) "Provide YYYY-MM-DD format" else "$age Years Old"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Teacher Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = office, onValueChange = { office = it }, label = { Text("Office/Room Location") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = specialty, onValueChange = { specialty = it }, label = { Text("Subject Specialty") }, modifier = Modifier.fillMaxWidth())
                
                Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                    OutlinedTextField(
                        value = dob, 
                        onValueChange = { }, 
                        label = { Text("Date of Birth") }, 
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false,
                        trailingIcon = { Icon(Icons.Default.DateRange, null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        placeholder = { Text("e.g. 1985-05-20") },
                        supportingText = {
                            Text(
                                text = "Parsed Age: $ageStr",
                                color = if (ageStr.contains("Provide")) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, email, office, specialty, dob) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = { content() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClassEditSetupDialog(
    classEntity: ClassEntity?,
    defaultDayOfWeek: Int,
    teachers: List<TeacherProfileEntity>,
    managedClasses: List<com.example.data.entity.ManagedClassEntity>,
    onDismiss: () -> Unit,
    onConfirm: (id: Int, name: String, subject: String, room: String, teacherId: Int?, day: Int, start: String, end: String) -> Unit
) {
    var name by remember { mutableStateOf(classEntity?.name ?: "") }
    var subject by remember { mutableStateOf(classEntity?.subject ?: "") }
    var room by remember { mutableStateOf(classEntity?.roomNumber ?: "Room 101") }
    var dayOfWeek by remember { mutableStateOf(classEntity?.dayOfWeek ?: defaultDayOfWeek) }
    var start by remember { mutableStateOf(classEntity?.startTime ?: "09:00") }
    var end by remember { mutableStateOf(classEntity?.endTime ?: "10:30") }
    var selectedTeacherId by remember { mutableStateOf<Int?>(classEntity?.teacherId ?: teachers.firstOrNull()?.id) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    val startTimeState = rememberTimePickerState(
        initialHour = start.split(":").getOrNull(0)?.toIntOrNull() ?: 9,
        initialMinute = start.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = false
    )
    val endTimeState = rememberTimePickerState(
        initialHour = end.split(":").getOrNull(0)?.toIntOrNull() ?: 10,
        initialMinute = end.split(":").getOrNull(1)?.toIntOrNull() ?: 30,
        is24Hour = false
    )

    // Toggle between regular class vs special duty/break
    var periodTypeIsClass by remember {
        mutableStateOf(
            classEntity == null ||
            (classEntity.name.uppercase() != "BREAK" &&
             !classEntity.name.contains("Ledger", ignoreCase = true) &&
             classEntity.subject.uppercase() != "BREAK")
        )
    }

    fun isValidTime(time: String): Boolean {
        if (!time.matches(Regex("\\d{2}:\\d{2}"))) return false
        val parts = time.split(":")
        val mins = parts[1].toIntOrNull() ?: 99
        val hrs = parts[0].toIntOrNull() ?: 99
        return hrs in 0..23 && mins in 0..59
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (classEntity == null) "Add Period" else "Edit Period",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Period category selector
                item {
                    Text("Period Category:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = periodTypeIsClass,
                            onClick = { periodTypeIsClass = true },
                            label = { Text("Regular Class", modifier = Modifier.testTag("dialog_chip_class")) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !periodTypeIsClass,
                            onClick = { periodTypeIsClass = false },
                            label = { Text("Break or Duty", modifier = Modifier.testTag("dialog_chip_break")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (!periodTypeIsClass) {
                    item {
                        Text("Quick Presets:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("BREAK", "Send Period Ledger", "Assembly", "Lunch").forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                        .clickable {
                                            name = preset
                                            subject = if (preset == "BREAK" || preset == "Lunch") "Break" else "Admin"
                                            room = if (preset == "BREAK" || preset == "Lunch") "Cafeteria" else "Office"
                                            selectedTeacherId = null
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    if (periodTypeIsClass) {
                        Text("Select Class (Managed List):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        if (managedClasses.isEmpty()) {
                            Text(
                                "No classes defined in 'Classes' tab. Go to Settings > Classes to add (e.g. 7A, 1B).",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            managedClasses.forEach { mc ->
                                FilterChip(
                                    selected = name == mc.name,
                                    onClick = { name = mc.name },
                                    label = { Text(mc.name) }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Period Title / Duty (e.g. BREAK)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject / Activity Group") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room / Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Session Weekly Day:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..7).forEach { d ->
                            val s = when (d) {
                                1 -> "Mon"
                                2 -> "Tue"
                                3 -> "Wed"
                                4 -> "Thu"
                                5 -> "Fri"
                                6 -> "Sat"
                                7 -> "Sun"
                                else -> ""
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (dayOfWeek == d) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f))
                                    .clickable { dayOfWeek = d }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(s, color = if (dayOfWeek == d) Color.White else Color.Black, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).clickable { showStartTimePicker = true }) {
                            OutlinedTextField(
                                value = start,
                                onValueChange = { },
                                label = { Text("Start Time") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        Box(modifier = Modifier.weight(1f).clickable { showEndTimePicker = true }) {
                            OutlinedTextField(
                                value = end,
                                onValueChange = { },
                                label = { Text("End Time") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && isValidTime(start)) {
                        onConfirm(classEntity?.id ?: 0, name, subject, room, selectedTeacherId, dayOfWeek, start, end)
                    }
                },
                enabled = name.isNotBlank() && isValidTime(start) && isValidTime(end)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                val h = startTimeState.hour.toString().padStart(2, '0')
                val m = startTimeState.minute.toString().padStart(2, '0')
                start = "$h:$m"
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = startTimeState)
        }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                val h = endTimeState.hour.toString().padStart(2, '0')
                val m = endTimeState.minute.toString().padStart(2, '0')
                end = "$h:$m"
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = endTimeState)
        }
    }
}

fun calculateAgeInt(dobString: String?): Int? {
    if (dobString.isNullOrBlank()) return null
    return try {
        val sdf = if (dobString.contains("/")) java.text.SimpleDateFormat("yyyy/MM/dd", Locale.US)
                  else java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val birthDate = sdf.parse(dobString) ?: return null
        val birthCal = Calendar.getInstance().apply { time = birthDate }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        if (age < 0) null else age
    } catch (e: Exception) {
        null
    }
}

@Composable
fun DeveloperInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "MADE BY A HUMAN (PROBABLY)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "I, GaNesh Khatiwada, straight up vibe coded this app, so no promises it even works or is safe ngl. It only needs notification permission. If it asks for anything else, it's acting sus—delete this shit immediately. Have a good one!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManagedClassesTab(
    viewModel: ClassFlowViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    val managedClasses by viewModel.allManagedClasses.collectAsStateWithLifecycle(emptyList())
    var newClassName by remember { mutableStateOf("") }
    val context = LocalContext.current
    var subjectToDelete by remember { mutableStateOf<ManagedClassEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Manage Classes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newClassName,
                onValueChange = { newClassName = it },
                label = { Text("New Class (e.g. 7A)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (newClassName.isNotBlank()) {
                        viewModel.addManagedClass(newClassName)
                        newClassName = ""
                        Toast.makeText(context, "Class Added!", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            managedClasses.forEach { mc ->
                InputChip(
                    selected = true,
                    onClick = { /* No-op */ },
                    label = { Text(mc.name) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Delete",
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { subjectToDelete = mc }
                        )
                    }
                )
            }
        }
    }

    subjectToDelete?.let { subject ->
        MathCaptchaDialog(
            title = "Delete Subject/Class",
            text = "Are you sure you want to delete ${subject.name}? This might break existing students and routines attached to it.",
            onConfirm = {
                subjectToDelete = null
                viewModel.deleteManagedClass(subject)
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Class deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.addManagedClass(subject.name)
                    }
                }
            },
            onDismiss = { subjectToDelete = null }
        )
    }
}

@Composable
fun DataBackupTab(viewModel: ClassFlowViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonStr = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (!jsonStr.isNullOrBlank()) {
                    pendingRestoreJson = jsonStr
                } else {
                    Toast.makeText(context, "Selected backup file is empty.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading selected file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "CloudSync",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Data Export & Import",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Export/backup or restore your scheduled routines, holiday definitions, student registers, and teaching diary notes directly from local storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "BACKUP CONTROL PANEL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.exportBackup(
                                    onSuccess = { filepath ->
                                        Toast.makeText(context, "Backup exported safely to: $filepath", Toast.LENGTH_LONG).show()
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, "Export failed: $error", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Export JSON Backup",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Create App Backup (Export)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Export all local SQLite states to a structured JSON file in local internal directory.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                importLauncher.launch("application/json")
                            }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Import JSON Backup",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Restore App Backup (Import)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Restore and synchronize all classes, holidays, student rosters, and notes from JSON storage.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                }
            }
        }

        item {
            var showBombDialog by remember { mutableStateOf(false) }
            if (showBombDialog) {
                MathCaptchaDialog(
                    title = "☢️ BOMB EVERYTHING",
                    text = "This will PERMANENTLY ERASE all your classes, students, notes, homeworks, and settings. This action cannot be undone.",
                    onConfirm = {
                        viewModel.bombEverything()
                        showBombDialog = false
                        Toast.makeText(context, "All data wiped.", Toast.LENGTH_LONG).show()
                    },
                    onDismiss = { showBombDialog = false }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBombDialog = true }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Bomb Everything",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Factory Reset (Sensitive)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Wipe all local database tables and clear app preferences immediately.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    pendingRestoreJson?.let { jsonStr ->
        MathCaptchaDialog(
            title = "Replace Existing Data",
            text = "Restoring this backup will permanently replace all current classes, homework, teachers, students, notes, and holidays with the contents of the backup file. This action cannot be undone.",
            onConfirm = {
                pendingRestoreJson = null
                viewModel.importBackup(
                    jsonStr = jsonStr,
                    onSuccess = {
                        Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_LONG).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, "Failed to restore backup: $error", Toast.LENGTH_LONG).show()
                    }
                )
            },
            onDismiss = { pendingRestoreJson = null }
        )
    }
}
