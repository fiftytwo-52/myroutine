package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.entity.HolidayEntity
import com.example.data.entity.TeacherNoteEntity
import com.example.ui.ClassFlowViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: ClassFlowViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.allNotes.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf("All") }

    // Browsed as separate sections so journal entries don't mix with the events/holidays calendar
    var journalSection by remember { mutableStateOf("Journal") }
    
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<TeacherNoteEntity?>(null) }
    
    val weeklyHolidays by viewModel.weeklyHolidays.collectAsStateWithLifecycle(emptySet())
    val allHolidays by viewModel.allHolidays.collectAsStateWithLifecycle(emptyList())
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var noteToDelete by remember { mutableStateOf<TeacherNoteEntity?>(null) }
    
    val tags = listOf("All", "Lesson Plan", "Homework Idea", "General", "Observation")
    
    // Plain diary notes only — dated events (eventEpochDay != null) are managed by the
    // separate Events & Holidays section tied to the event and holiday buttons.
    val journalNotes = remember(notes) { notes.filter { it.eventEpochDay == null } }
    val eventNotes = remember(notes) { notes.filter { it.eventEpochDay != null } }

    val filteredNotes = remember(journalNotes, searchQuery, selectedTagFilter) {
        journalNotes.filter { note ->
            val matchesQuery = note.title.contains(searchQuery, ignoreCase = true) || 
                               note.content.contains(searchQuery, ignoreCase = true)
            val matchesTag = selectedTagFilter == "All" || note.tag == selectedTagFilter
            matchesQuery && matchesTag
        }
    }

    // The journal search bar still searches everything: journal entries, notes, lesson
    // plans, homework ideas, dated events, and holidays are all matched by the query.
    val matchingEvents = remember(eventNotes, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else eventNotes.filter { event ->
            event.title.contains(searchQuery, ignoreCase = true) ||
            event.content.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.eventEpochDay ?: 0L }
    }
    val matchingHolidays = remember(allHolidays, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allHolidays.filter { holiday ->
            holiday.title.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.startDate }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Teaching Journal",
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
                onClick = {
                    editingNote = null
                    showAddNoteDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .testTag("add_journal_note_fab")
            ) {
                Icon(Icons.Default.Add, "Add Note")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = innerPadding.calculateTopPadding() + 6.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section toggle — journal entries and events/holidays are kept as separate views
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = journalSection == "Journal",
                        onClick = { journalSection = "Journal" },
                        label = { Text("Journal (${filteredNotes.size})") }
                    )
                    FilterChip(
                        selected = journalSection == "Events & Holidays",
                        onClick = { journalSection = "Events & Holidays" },
                        label = { Text("Events & Holidays") }
                    )
                }
            }

            // Item 1: Holiday Calendar — Events & Holidays section only
            if (journalSection == "Events & Holidays") item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    ViewOnlyHolidayCalendar(viewModel, weeklyHolidays)
                }
            }

            // Item 2: Search field & Tag filter Category Card — Journal section only
            if (journalSection == "Journal") item {
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
                            placeholder = { Text("Search notes, lesson plans, events, holidays...") },
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
                            "Categorize Journal Entries:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Scrollable LazyRow chip arrangement for Tags (All, Lesson Plan, Homework Idea, General, Observation)
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(tags) { tag ->
                                FilterChip(
                                    selected = selectedTagFilter == tag,
                                    onClick = { selectedTagFilter = tag },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }
            }

            // Empty state — only when no diary notes, events, or holidays matched
            if (journalSection == "Journal" && filteredNotes.isEmpty() &&
                (searchQuery.isBlank() || (matchingEvents.isEmpty() && matchingHolidays.isEmpty()))
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                if (searchQuery.isBlank()) "No teaching notes created." else "No matches found.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (searchQuery.isBlank()) "Compile homework questions, lesson outlines, exam preps, and observations directly in your teaching diary."
                                else "Nothing matched \"${searchQuery}\" across your journal notes, lesson plans, homework ideas, events, and holidays.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (journalSection == "Journal") {
                items(filteredNotes, key = { "note_${it.id}_${it.createdDateMillis}" }) { note ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        JournalNoteCard(
                            note = note,
                            onEdit = {
                                editingNote = note
                                showAddNoteDialog = true
                            },
                            onDelete = { noteToDelete = note }
                        )
                    }
                }
            }

            // The journal search bar reaches beyond diary notes — matching events and
            // holidays surface as their own result groups below the notes list.
            if (journalSection == "Journal" && searchQuery.isNotBlank() && matchingEvents.isNotEmpty()) {
                item {
                    SearchResultHeader(title = "Matching Events", count = matchingEvents.size)
                }
                items(matchingEvents, key = { "event_${it.id}" }) { event ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        CalendarSearchResultCard(
                            icon = Icons.Default.Event,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = event.title,
                            dateText = formatEventEpochDay(event.eventEpochDay),
                            subtitle = event.eventStatus.ifBlank { "Pending" }
                        )
                    }
                }
            }

            if (journalSection == "Journal" && searchQuery.isNotBlank() && matchingHolidays.isNotEmpty()) {
                item {
                    SearchResultHeader(title = "Matching Holidays", count = matchingHolidays.size)
                }
                items(matchingHolidays, key = { "holiday_${it.id}" }) { holiday ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        CalendarSearchResultCard(
                            icon = Icons.Default.Celebration,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = holiday.title,
                            dateText = formatHolidayRange(holiday),
                            subtitle = "Holiday"
                        )
                    }
                }
            }
        }
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete '${note.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        noteToDelete = null
                        viewModel.deleteNote(note)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Note deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.saveNote(
                                    id = 0, // Insert as new Note to avoid conflict or just re-insert
                                    title = note.title,
                                    content = note.content,
                                    tag = note.tag
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
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddNoteDialog) {
        NoteAddEditDialog(
            note = editingNote,
            tags = tags.filter { it != "All" },
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { title, content, tag ->
                viewModel.saveNote(
                    id = editingNote?.id ?: 0,
                    title = title,
                    content = content,
                    tag = tag
                )
                showAddNoteDialog = false
            }
        )
    }
}


@Composable
fun JournalNoteCard(
    note: TeacherNoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val formattedDate = remember(note.createdDateMillis) {
        val sdf = SimpleDateFormat("EEEE, MMM dd yyyy, h:mm a", Locale.getDefault())
        sdf.format(Date(note.createdDateMillis))
    }

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
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (note.tag) {
                                        "Lesson Plan" -> Color(0xFFD0BCFF)
                                        "Homework Idea" -> Color(0xFFFFD8E4)
                                        "Observation" -> Color(0xFFC5E1A5)
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = note.tag.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to read more...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteAddEditDialog(
    note: TeacherNoteEntity?,
    tags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, tag: String) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var selectedTag by remember { mutableStateOf(note?.tag ?: "General") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (note == null) "New Journal Entry" else "Edit Journal Entry",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title *") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    "Select Category Tag:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            label = { Text(tag) }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Write your notes details here... *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onConfirm(title, content, selectedTag)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Save Entry")
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
private fun SearchResultHeader(title: String, count: Int) {
    Text(
        "$title ($count)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
private fun CalendarSearchResultCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    dateText: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = iconTint
            )
        }
    }
}

/** Formats a note's eventEpochDay (days since epoch) as a readable date. */
private fun formatEventEpochDay(epochDay: Long?): String = epochDay?.let {
    try {
        java.time.LocalDate.ofEpochDay(it)
            .format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM dd yyyy"))
    } catch (e: Exception) {
        ""
    }
} ?: ""

/** Formats a holiday's "YYYY-MM-DD" start/end range for the search results. */
private fun formatHolidayRange(holiday: HolidayEntity): String = try {
    val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val start = java.time.LocalDate.parse(holiday.startDate)
    if (holiday.startDate == holiday.endDate) {
        start.format(fmt)
    } else {
        "${start.format(fmt)} – ${java.time.LocalDate.parse(holiday.endDate).format(fmt)}"
    }
} catch (e: Exception) {
    if (holiday.startDate == holiday.endDate) holiday.startDate
    else "${holiday.startDate} – ${holiday.endDate}"
}
