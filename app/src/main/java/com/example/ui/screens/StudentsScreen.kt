package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.FileUpload
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
import com.example.data.entity.StudentEntity
import com.example.ui.ClassFlowViewModel
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentsScreen(
    viewModel: ClassFlowViewModel,
    modifier: Modifier = Modifier
) {
    val students by viewModel.allStudents.collectAsStateWithLifecycle(initialValue = emptyList())
    val managedClasses by viewModel.allManagedClasses.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilters by remember { mutableStateOf(setOf<String>()) }
    
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var viewingStudentReport by remember { mutableStateOf<StudentEntity?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }
    
    // Derived class list for filtering options
    val availableClasses = remember(students, managedClasses) {
        (managedClasses.map { it.name } + students.map { it.className }).distinct().sorted()
    }
    
    // Filtered list
    val filteredStudents = remember(students, searchQuery, selectedClassFilters) {
        students.filter { student ->
            val matchesQuery = student.name.contains(searchQuery, ignoreCase = true) || 
                               student.rollNumber.contains(searchQuery, ignoreCase = true)
            val matchesClass = selectedClassFilters.isEmpty() || selectedClassFilters.contains(student.className)
            matchesQuery && matchesClass
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Student Records",
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        var showReportMenu by remember { mutableStateOf(false) }
                        Box {
                            TextButton(
                                onClick = { showReportMenu = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.Assessment, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Report", fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(
                                expanded = showReportMenu,
                                onDismissRequest = { showReportMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Print Class PDF") },
                                    onClick = {
                                        showReportMenu = false
                                        if (selectedClassFilters.size != 1) {
                                            Toast.makeText(context, "Select exactly ONE class to print.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.generateClassPdfReport(context, selectedClassFilters.first())
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Print, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export CSV (All/Filtered)") },
                                    onClick = {
                                        showReportMenu = false
                                        viewModel.exportStudentReport(
                                            context = context,
                                            classFilter = selectedClassFilters.ifEmpty { null },
                                            onSuccess = { Toast.makeText(context, "Report: $it", Toast.LENGTH_LONG).show() },
                                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.TableChart, null) }
                                )
                            }
                        }
                        TextButton(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Outlined.FileUpload, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingStudent = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .testTag("add_student_fab")
            ) {
                Icon(Icons.Default.Add, "Add Student")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar & Filters Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search student name or roll...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    
                    Text(
                        "Filter by Class Room (${filteredStudents.size} of ${students.size} students):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedClassFilters.isEmpty(),
                        onClick = { selectedClassFilters = emptySet() },
                        label = { Text("All") }
                    )
                    availableClasses.forEach { className ->
                        FilterChip(
                            selected = selectedClassFilters.contains(className),
                            onClick = {
                                selectedClassFilters = if (selectedClassFilters.contains(className)) {
                                    selectedClassFilters - className
                                } else {
                                    selectedClassFilters + className
                                }
                            },
                            label = { Text(className) }
                        )
                    }
                }
                }
            }

            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "No student records found.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tap the '+' button to log student details, parent contacts, and roll numbers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredStudents, key = { _, s -> "student_${s.id}" }) { index, student ->
                        StudentItemCard(
                            student = student,
                            serialNumber = index + 1,
                            viewModel = viewModel,
                            onEdit = {
                                editingStudent = student
                                showAddEditDialog = true
                            },
                            onDelete = { studentToDelete = student },
                            onPrint = { viewingStudentReport = student }
                        )
                    }
                }
            }
        }
    }
    
    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to delete ${student.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        studentToDelete = null
                        viewModel.deleteStudent(student)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Student record deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.saveStudent(
                                    id = 0,
                                    name = student.name,
                                    rollNumber = student.rollNumber,
                                    className = student.className,
                                    contactNumber = student.contactNumber,
                                    guardianName = student.guardianName,
                                    performanceNotes = student.performanceNotes
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
                TextButton(onClick = { studentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddEditDialog) {
        StudentAddEditDialog(
            student = editingStudent,
            managedClasses = managedClasses,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { name, roll, cls, contact, guardian, notes ->
                viewModel.saveStudent(
                    id = editingStudent?.id ?: 0,
                    name = name,
                    rollNumber = roll,
                    className = cls,
                    contactNumber = contact,
                    guardianName = guardian,
                    performanceNotes = notes
                )
                showAddEditDialog = false
            },
            onResetBehavior = if (editingStudent != null) {
                {
                    viewModel.resetBehaviorForStudent(editingStudent!!.id)
                    Toast.makeText(context, "Behavior counts reset to 0 for ${editingStudent!!.name}", Toast.LENGTH_SHORT).show()
                }
            } else null
        )
    }

    if (showImportDialog) {
        val importLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }
                    if (!text.isNullOrBlank()) {
                        importText = text
                    } else {
                        Toast.makeText(context, "Selected file is empty.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        StudentImportDialog(
            managedClasses = managedClasses,
            importText = importText,
            onImportTextChange = { importText = it },
            onDismiss = { 
                showImportDialog = false
                importText = ""
            },
            onConfirm = { className, text ->
                viewModel.importStudentsFromText(className, text)
                showImportDialog = false
                importText = ""
                Toast.makeText(context, "Imported students for class $className!", Toast.LENGTH_SHORT).show()
            },
            onPickFileTrigger = { importLauncher.launch("text/plain") }
        )
    }
    
    if (viewingStudentReport != null) {
        StudentDetailedReportDialog(
            student = viewingStudentReport!!,
            viewModel = viewModel,
            onDismiss = { viewingStudentReport = null },
            onPrint = { viewModel.generateIndividualPdfReport(context, viewingStudentReport!!) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailedReportDialog(
    student: StudentEntity,
    viewModel: ClassFlowViewModel,
    onDismiss: () -> Unit,
    onPrint: () -> Unit
) {
    val submissions by viewModel.getSubmissionsForStudent(student.id).collectAsStateWithLifecycle(emptyList())
    val activities by viewModel.getActivitiesForStudent(student.id).collectAsStateWithLifecycle(emptyList())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report: ${student.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Card(
                     modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                     shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Homework Record:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                             Text("Done: ${submissions.count { it.status.lowercase() == "done" }}", style = MaterialTheme.typography.bodySmall)
                             Text("Half: ${submissions.count { it.status.lowercase() == "half done" }}", style = MaterialTheme.typography.bodySmall)
                             Text("Not: ${submissions.count { it.status.lowercase() == "not done" }}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val normalizedActivities = remember(activities) {
                    activities.filter { it.activityType != "Disciplined" }.map {
                        when (it.activityType) {
                            "Making Noise", "Name By Monitor" -> it.copy(activityType = "Named by Monitor")
                            "Impressed by Engaging", "Positive Curiosity" -> it.copy(activityType = it.activityType)
                            else -> it
                        }
                    }
                }
                
                Card(
                     modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                     shape = RoundedCornerShape(12.dp),
                     border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Behavior Summary & Log:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (activities.isEmpty()) {
                            Text("No activities recorded.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            val groupedActivities = normalizedActivities.groupBy { it.activityType }
                            groupedActivities.forEach { (type, typeActivities) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(type, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = typeActivities.size.toString(),
                                                fontWeight = FontWeight.Bold, 
                                                style = MaterialTheme.typography.labelMedium, 
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        val activitiesWithNotes = typeActivities.filter { it.note.isNotBlank() && it.activityType != "Named by Monitor" }
                                        if (activitiesWithNotes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            activitiesWithNotes.forEach { activity ->
                                                Text("• ${activity.note}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 2.dp))
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
            Button(onClick = onPrint) { Text("Print/Save PDF") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudentImportDialog(
    managedClasses: List<com.example.data.entity.ManagedClassEntity>,
    importText: String,
    onImportTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (className: String, text: String) -> Unit,
    onPickFileTrigger: () -> Unit
) {
    var selectedClass by remember { mutableStateOf("") }
    var showWarning by remember { mutableStateOf(false) }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            title = { Text("⚠️ Overwrite Warning") },
            text = { Text("Importing students for class $selectedClass will PERMANENTLY DELETE all current student records for this class. Are you sure you want to proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm(selectedClass, importText)
                        showWarning = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Overwrite & Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarning = false }) { Text("Cancel") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Students (.txt)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Target Class:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                if (managedClasses.isEmpty()) {
                    Text(
                        "No classes defined. Go to Settings > Classes to add them first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    managedClasses.forEach { mc ->
                        FilterChip(
                            selected = selectedClass == mc.name,
                            onClick = { selectedClass = mc.name },
                            label = { Text(mc.name) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                OutlinedButton(
                    onClick = onPickFileTrigger,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select .txt file")
                }

                Text("...OR paste names manually:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = importText,
                    onValueChange = onImportTextChange,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("John Doe\nJane Smith\n...") },
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { showWarning = true },
                enabled = selectedClass.isNotBlank() && importText.isNotBlank()
            ) {
                Text("Proceed")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentItemCard(
    student: StudentEntity,
    serialNumber: Int,
    viewModel: ClassFlowViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val activities by viewModel.getActivitiesForStudent(student.id).collectAsStateWithLifecycle(emptyList())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = serialNumber.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Column {
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Roll: ${student.rollNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Class Room: ${student.className}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Row {
                    IconButton(onClick = onPrint) {
                        Icon(Icons.Default.Print, "Print Report", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "BEHAVIOUR & ACTIVITY LOGS:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    if (activities.isEmpty()) {
                        Text(
                            "No abnormal activities recorded. (Normal/Active)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val activityCounts = remember(activities) {
                            activities.filter { it.activityType != "Disciplined" }.map {
                                when (it.activityType) {
                                    "Making Noise", "Name By Monitor" -> it.copy(activityType = "Named by Monitor")
                                    "Impressed by Engaging", "Positive Curiosity" -> it.copy(activityType = it.activityType)
                                    else -> it
                                }
                            }.groupBy { it.activityType }
                                .mapValues { it.value.size }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activityCounts.forEach { (type, count) ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text("$type: $count", fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    if (student.guardianName.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Guardian: ${student.guardianName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    if (student.contactNumber.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Contact: ${student.contactNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    if (student.performanceNotes.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(12.dp)
                        ) {
                            Text(
                                "PERFORMANCE NOTES:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = student.performanceNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentAddEditDialog(
    student: StudentEntity?,
    managedClasses: List<com.example.data.entity.ManagedClassEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, roll: String, className: String, contact: String, guardian: String, notes: String) -> Unit,
    onResetBehavior: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(student?.name ?: "") }
    var roll by remember { mutableStateOf(student?.rollNumber ?: "") }
    var className by remember { mutableStateOf(student?.className ?: "") }
    var guardian by remember { mutableStateOf(student?.guardianName ?: "") }
    var contact by remember { mutableStateOf(student?.contactNumber ?: "") }
    var notes by remember { mutableStateOf(student?.performanceNotes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (student == null) "Log Student Record" else "Edit Student Record",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roll,
                    onValueChange = { roll = it },
                    label = { Text("Roll Number (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Select Class Room *", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                if (managedClasses.isEmpty()) {
                    Text(
                        "No classes defined. Add them in Settings > Classes tab first.",
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
                            selected = className == mc.name,
                            onClick = { className = mc.name },
                            label = { Text(mc.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = guardian,
                    onValueChange = { guardian = it },
                    label = { Text("Guardian Name (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Guardian Contact Phone (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Performance & Behaviour Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (student != null && onResetBehavior != null) {
                    TextButton(onClick = onResetBehavior) {
                        Text("Reset Behavior Record", color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    onClick = {
                        if (name.isNotBlank() && className.isNotBlank()) {
                            onConfirm(name, roll, className, contact, guardian, notes)
                        }
                    },
                    enabled = name.isNotBlank() && className.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
