package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "syllabus")
data class SyllabusEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val className: String, // e.g. "Science 5A", but wait, user says multiple classes could share syllabus. So maybe just title e.g. "Science Grade 5"
    val rawText: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "units",
    foreignKeys = [
        ForeignKey(
            entity = SyllabusEntity::class,
            parentColumns = ["id"],
            childColumns = ["syllabusId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("syllabusId")]
)
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val syllabusId: Int,
    val unitNumber: Int,
    val title: String,
    val description: String = ""
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("unitId")]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val unitId: Int,
    val topicNumber: Int,
    val title: String
)

@Entity(
    tableName = "topic_progress",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("topicId"), Index("classId")]
)
data class TopicProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: Int, // The specific class, e.g. 5A
    val topicId: Int,
    val isCompleted: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
