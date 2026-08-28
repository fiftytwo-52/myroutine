package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dateString: String,
    val targetClassNames: String? = null, // Comma separated class names
    val fullMarks: String = "100",
    val passMarks: String = "40",
    val subject: String = ""
)

@Entity(
    tableName = "exam_marks",
    indices = [Index(value = ["examId", "classId", "studentId"], unique = true)]
)
data class ExamMarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val examId: Int,
    val classId: Int,
    val studentId: Int,
    val marksObtained: String,
    val remarks: String = ""
)
