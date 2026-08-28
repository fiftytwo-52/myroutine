package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val startDate: String, // format "YYYY-MM-DD"
    val endDate: String    // format "YYYY-MM-DD"
)
