package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teacher_profiles")
data class TeacherProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val officeLocation: String,
    val subjectSpecialty: String,
    val dob: String = "",
    val schoolName: String = ""
)
