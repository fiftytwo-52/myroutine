package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "homework",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classId"])]
)
data class HomeworkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: Int,
    val title: String,
    val description: String,
    val createdDateMillis: Long = System.currentTimeMillis(), // Creation timestamp
    val isCompleted: Boolean = false,
    val notes: String? = null,
    val imageUri: String? = null, // Path or URI of image taken from Camera or picked from Gallery
    val checkingDateMillis: Long? = null, // Homework checking date
    val classworkNote: String? = null // Associated classwork note details
)

