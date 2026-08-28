package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rollNumber: String,
    val className: String, // e.g. "5a", "7b"
    val contactNumber: String = "",
    val guardianName: String = "",
    val performanceNotes: String = ""
)
