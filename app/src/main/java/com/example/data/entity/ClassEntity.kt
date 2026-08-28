package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "classes",
    foreignKeys = [
        ForeignKey(
            entity = TeacherProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["teacherId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["teacherId"])]
)
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val subject: String,
    val roomNumber: String,
    val teacherId: Int?, // Optional linked teacher ID
    val dayOfWeek: Int,  // 1 = Monday, 7 = Sunday (matches standard java.time.DayOfWeek)
    val startTime: String, // "HH:mm" (24-hour style format)
    val endTime: String    // "HH:mm" (24-hour style format)
) {
    // Convert startTime to minutes elapsed in the day (useful for status clock comparison)
    val startTimeMinutes: Int
        get() = try {
            val parts = startTime.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }

    // Convert endTime to minutes elapsed in the day
    val endTimeMinutes: Int
        get() = try {
            val parts = endTime.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
}
