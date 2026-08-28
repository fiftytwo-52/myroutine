package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_log")
data class ClassLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: Int,
    val dateString: String, // format "yyyy-MM-dd"
    val status: String // "Completed", "Half completed", "Cancelled", "Disturbed"
)
