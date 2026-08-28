package com.example.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ClassEntity
import com.example.data.entity.ExamEntity
import com.example.data.entity.ManagedClassEntity
import com.example.ui.ClassFlowViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExamsScreen(viewModel: ClassFlowViewModel) {
    val context = LocalContext.current
    val exams by viewModel.allExams.collectAsStateWithLifecycle(initialValue = emptyList())
    val managedClasses by viewModel.allManagedClasses.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var showAddExamDialog by remember { mutableStateOf(false) }
    var selectedExam by remember { mutableStateOf<ExamEntity?>(null) }
    var selectedClassForMarks by remember { mutableStateOf<ManagedClassEntity?>(null) }
    var viewingExamStats by remember { mutableStateOf<ExamEntity?>(null) }
    var examToDelete by remember { mutableStateOf<ExamEntity?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Exams & Grading",
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExamDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.Add, "Add Exam")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
        ) {
            if (exams.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No exams created yet. Use '+' to start.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(exams) { exam ->
                val examMarks by viewModel.getAllMarksForExam(exam.id).collectAsStateWithLifecycle(emptyList())
                val numericMarks = remember(examMarks) { examMarks.mapNotNull { it.marksObtained.toDoubleOrNull() } }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewingExamStats = exam },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exam.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Date: ${exam.dateString} | FM: ${exam.fullMarks} PM: ${exam.passMarks}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (numericMarks.isNotEmpty()) {
                                    Text(
                                        text = "High: ${numericMarks.max()} • Low: ${numericMarks.min()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (exam.targetClassNames != null) {
                                    Text(
                                        text = "Classes: ${exam.targetClassNames}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row {
                                IconButton(onClick = { viewingExamStats = exam }) {
                                    Icon(Icons.Default.Analytics, "Stats", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { examToDelete = exam }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedClassForMarks != null && selectedExam != null) {
        MarksEntryDialog(
            exam = selectedExam!!,
            managedClass = selectedClassForMarks!!,
            viewModel = viewModel,
            onDismiss = {
                selectedClassForMarks = null
                selectedExam = null
            }
        )
    }

    if (showAddExamDialog) {
        AddExamDialog(
            managedClasses = managedClasses,
            onDismiss = { showAddExamDialog = false },
            onConfirm = { name, date, targetClasses, full, pass, subject ->
                viewModel.saveExam(name = name, dateString = date, targetClassNames = targetClasses, fullMarks = full, passMarks = pass, subject = subject)
                showAddExamDialog = false
            }
        )
    }

    if (viewingExamStats != null) {
        ExamDetailsDialog(
            exam = viewingExamStats!!,
            viewModel = viewModel,
            managedClasses = managedClasses,
            onDismiss = { viewingExamStats = null },
            onUpdateMarks = { mc ->
                selectedExam = viewingExamStats
                selectedClassForMarks = mc
                viewingExamStats = null
            }
        )
    }

    examToDelete?.let { exam ->
        MathCaptchaDialog(
            title = "Delete Exam",
            text = "Are you sure you want to delete ${exam.name}? This will remove all associated marks.",
            onConfirm = {
                examToDelete = null
                viewModel.deleteExam(exam.id)
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Exam deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.saveExam(
                            name = exam.name,
                            dateString = exam.dateString,
                            targetClassNames = exam.targetClassNames,
                            fullMarks = exam.fullMarks,
                            passMarks = exam.passMarks,
                            subject = exam.subject
                        )
                    }
                }
            },
            onDismiss = { examToDelete = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExamDialog(managedClasses: List<ManagedClassEntity>, onDismiss: () -> Unit, onConfirm: (String, String, String?, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var fullMarks by remember { mutableStateOf("100") }
    var passMarks by remember { mutableStateOf("40") }
    var selectedClasses by remember { mutableStateOf(setOf<String>()) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        date = sdf.format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Exam", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exam Name (e.g. First Terminal)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { },
                    label = { Text("Exam Date") },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    readOnly = true,
                    enabled = false,
                    trailingIcon = { Icon(Icons.Default.DateRange, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fullMarks,
                        onValueChange = { fullMarks = it },
                        label = { Text("Full Marks") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = passMarks,
                        onValueChange = { passMarks = it },
                        label = { Text("Pass Marks") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Text("Target Classes (Multi-select):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedClasses.isEmpty(),
                        onClick = { selectedClasses = emptySet() },
                        label = { Text("All Classes") }
                    )
                    managedClasses.forEach { mc ->
                        FilterChip(
                            selected = selectedClasses.contains(mc.name),
                            onClick = {
                                if (selectedClasses.contains(mc.name)) {
                                    selectedClasses = selectedClasses - mc.name
                                } else {
                                    selectedClasses = selectedClasses + mc.name
                                }
                            },
                            label = { Text(mc.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank()) {
                        val targetClasses = if (selectedClasses.isEmpty()) null else selectedClasses.joinToString(",")
                        onConfirm(name, date, targetClasses, fullMarks, passMarks, subject) 
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamDetailsDialog(
    exam: ExamEntity,
    viewModel: ClassFlowViewModel,
    managedClasses: List<ManagedClassEntity>,
    onDismiss: () -> Unit,
    onUpdateMarks: (ManagedClassEntity) -> Unit
) {
    var stats by remember { mutableStateOf<com.example.ui.ExamStats?>(null) }
    val passMarkInt = exam.passMarks.toIntOrNull() ?: 40
    
    val classesToShow = if (exam.targetClassNames != null) {
        val names = exam.targetClassNames.split(",")
        managedClasses.filter { names.contains(it.name) }
    } else {
        managedClasses
    }
    
    LaunchedEffect(exam.id) {
        stats = viewModel.getExamStats(exam.id, passMarkInt)
    }

    val currentStats = stats
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${exam.name} - Detailed View", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (currentStats != null && currentStats.totalStudents > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("STATISTICS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Passed", style = MaterialTheme.typography.labelSmall)
                                    Text(currentStats.passed.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Failed", style = MaterialTheme.typography.labelSmall)
                                    Text(currentStats.failed.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Avg", style = MaterialTheme.typography.labelSmall)
                                    Text(String.format(java.util.Locale.US, "%.1f", currentStats.averageMark), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Highest: ${currentStats.highestMark} (${currentStats.highestMarkStudentName ?: "N/A"})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Lowest: ${currentStats.lowestMark} (${currentStats.lowestMarkStudentName ?: "N/A"})", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Text("No marks data recorded yet.", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
                
                // Student List
                val students by viewModel.allStudents.collectAsStateWithLifecycle(emptyList())
                val allMarks by viewModel.getAllMarksForExam(exam.id).collectAsStateWithLifecycle(emptyList())

                val filteredStudents = remember(students, classesToShow) {
                    val classNames = classesToShow.map { it.name }
                    students.filter { classNames.contains(it.className) }
                }

                Text("Student Performances:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredStudents) { student ->
                         val mark = allMarks.find { it.studentId == student.id }
                         Card(
                            modifier = Modifier.fillMaxWidth(),
                             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                         ) {
                             Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                 Text(student.name, fontWeight = FontWeight.SemiBold)
                                 Text("Marks: ${mark?.marksObtained ?: "N/A"}")
                             }
                         }
                    }
                }
            }
        },
        confirmButton = {
            val context = LocalContext.current
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.generateExamPdfReport(context, exam) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) { Text("Export") }
                Button(
                    onClick = { 
                        if (classesToShow.isNotEmpty()) onUpdateMarks(classesToShow[0])
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Edit Marks") }
                Button(onClick = onDismiss) { Text("Done") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksEntryDialog(
    exam: ExamEntity,
    managedClass: ManagedClassEntity,
    viewModel: ClassFlowViewModel,
    onDismiss: () -> Unit
) {
    val students by viewModel.getStudentsForClass(managedClass.name).collectAsStateWithLifecycle(emptyList())
    val marks by viewModel.getAllMarksForExam(exam.id).collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current

    // Per-student draft state — nothing is persisted until "Save All" is tapped.
    val drafts = remember { androidx.compose.runtime.mutableStateMapOf<Int, MarkDraft>() }
    val fullMarks = remember(exam.fullMarks) { exam.fullMarks.toDoubleOrNull() ?: 100.0 }

    fun draftFor(studentId: Int): MarkDraft {
        val record = marks.find { it.studentId == studentId }
        return drafts[studentId] ?: MarkDraft(
            mark = record?.marksObtained ?: "",
            remarks = record?.remarks ?: "",
            absent = record?.marksObtained == "Absent"
        )
    }

    fun isInvalid(draft: MarkDraft): Boolean {
        if (draft.absent || draft.mark.isBlank()) return false
        val m = draft.mark.toDoubleOrNull()
        return m == null || m < 0 || m > fullMarks
    }

    val hasErrors = students.any { isInvalid(draftFor(it.id)) }

    fun saveAllDrafts() {
        students.forEach { student ->
            val draft = draftFor(student.id)
            val finalMark = when {
                draft.absent -> "Absent"
                draft.mark.isBlank() -> return@forEach
                else -> draft.mark
            }
            viewModel.saveExamMark(
                examId = exam.id,
                classId = managedClass.id,
                studentId = student.id,
                marks = finalMark,
                remarks = draft.remarks
            )
        }
        Toast.makeText(context, "Marks saved", Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Marks: ${exam.name} - ${managedClass.name}", fontWeight = FontWeight.Bold) },
        text = {
            if (students.isEmpty()) {
                Text("No students found in this class.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(students) { student ->
                        val draft = draftFor(student.id)

                        val isError = isInvalid(draft)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(student.name, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text("Absent", style = MaterialTheme.typography.bodySmall)
                                        androidx.compose.material3.Checkbox(
                                            checked = draft.absent,
                                            onCheckedChange = { absent ->
                                                drafts[student.id] = draft.copy(
                                                    absent = absent,
                                                    mark = if (absent) "Absent" else ""
                                                )
                                            }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = if (draft.absent) "Absent" else draft.mark,
                                        onValueChange = {
                                            if (!draft.absent) {
                                                drafts[student.id] = draft.copy(mark = it)
                                            }
                                        },
                                        label = { Text("Marks") },
                                        isError = isError,
                                        enabled = !draft.absent,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = draft.remarks,
                                        onValueChange = {
                                            drafts[student.id] = draft.copy(remarks = it)
                                        },
                                        label = { Text("Remarks") },
                                        modifier = Modifier.weight(2f),
                                        singleLine = true
                                    )
                                }
                                if (isError) {
                                    Text("Mark must be between 0 and $fullMarks.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        if (!hasErrors) {
                            saveAllDrafts()
                            onDismiss()
                        }
                    },
                    enabled = !hasErrors
                ) { Text("Save All") }
            }
        }
    )
}

// Draft state holder for the bulk marks-entry dialog.
data class MarkDraft(
    val mark: String = "",
    val remarks: String = "",
    val absent: Boolean = false
)
