package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teacher_notes")
data class TeacherNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdDateMillis: Long = System.currentTimeMillis(),
    val tag: String = "General", // e.g. "Lesson Plan", "Homework Notes", "General"
    // Non-null marks this row as a dated calendar event instead of a plain note.
    val eventEpochDay: Long? = null
)
