package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "managed_classes")
data class ManagedClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)
