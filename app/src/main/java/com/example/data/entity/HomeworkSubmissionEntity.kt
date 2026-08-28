package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "homework_submission")
data class HomeworkSubmissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val homeworkId: Int,
    val studentId: Int,
    val status: String // "Done", "Half Done", "Not Done"
)
