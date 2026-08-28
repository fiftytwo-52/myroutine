package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "student_activities")
data class StudentActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val studentName: String,
    val className: String,
    val activityType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
