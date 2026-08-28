package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SyllabusEntity
import com.example.ui.ClassFlowViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusScreen(viewModel: ClassFlowViewModel) {
    val context = LocalContext.current
    val allSyllabus by viewModel.allSyllabuses.collectAsStateWithLifecycle(initialValue = emptyList<SyllabusEntity>())
    val allClasses by viewModel.allClasses.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var showAddDialog by remember { mutableStateOf(value = false) }
    var selectedSyllabus by remember { mutableStateOf<Int?>(null) }
    var syllabusToDelete by remember { mutableStateOf<SyllabusEntity?>(null) }

    val classNames = remember(allClasses) {
        allClasses.asSequence().map { it.name }.distinct().toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Curriculum & Syllabus", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Syllabus")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allSyllabus) { syllabus ->
                SyllabusCard(
                    syllabus = syllabus,
                    onClick = { selectedSyllabus = syllabus.id },
                    onDelete = { syllabusToDelete = syllabus }
                )
            }
        }
    }

    if (showAddDialog) {
        AddSyllabusDialog(
            classNames = classNames,
            onDismiss = { showAddDialog = false },
            onSave = { subject, clsName, text, _ ->
                viewModel.addSyllabus(subject, clsName, text)
                showAddDialog = false
            }
        )
    }

    syllabusToDelete?.let { syllabus ->
        AlertDialog(
            onDismissRequest = { syllabusToDelete = null },
            title = { Text("Delete Syllabus") },
            text = {
                Text(
                    "Permanently delete \"${syllabus.subject}\" for ${syllabus.className}? " +
                        "All of its units, topics, and tracking progress will be removed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedSyllabus == syllabus.id) selectedSyllabus = null
                        viewModel.deleteSyllabus(syllabus.id)
                        syllabusToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { syllabusToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    selectedSyllabus?.let { id ->
        val currentSyllabus = allSyllabus.find { it.id == id }
        if (currentSyllabus != null) {
            SyllabusDetailDialog(
                syllabus = currentSyllabus,
                onDismiss = { selectedSyllabus = null },
                onOpenPdf = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(currentSyllabus.rawText.toUri(), "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Open Syllabus PDF"))
                    } catch (_: Exception) {
                        // Handle error
                    }
                }
            )
        }
    }
}

@Composable
fun SyllabusCard(
    syllabus: SyllabusEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(syllabus.subject, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Class: ${syllabus.className}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSyllabusDialog(
    classNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, (Double) -> Unit) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(classNames.firstOrNull() ?: "") }
    var syllabusText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Syllabus") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Box {
                    OutlinedTextField(
                        value = selectedClass,
                        onValueChange = { },
                        label = { Text("Class") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        classNames.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedClass = name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = syllabusText,
                    onValueChange = { syllabusText = it },
                    label = { Text("Syllabus Content / PDF Link") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(subject, selectedClass, syllabusText) { } }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SyllabusDetailDialog(
    syllabus: SyllabusEntity,
    onDismiss: () -> Unit,
    onOpenPdf: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(syllabus.subject, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                
                Text("Class: ${syllabus.className}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                
                HorizontalDivider()
                
                Text(syllabus.rawText, style = MaterialTheme.typography.bodyMedium)
                
                Button(
                    onClick = onOpenPdf,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Attachment")
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}
