package com.myclass.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A weekly recurring class period in the teaching schedule. */
@Entity(tableName = "class_schedule")
data class ClassSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val className: String,
    val roomNumber: String,
    val dayOfWeek: Int, // 1 = Monday ... 7 = Sunday (java.time.DayOfWeek.value)
    val startTime: String, // "HH:mm"
    val endTime: String    // "HH:mm"
)

/** Teacher's log for one occurrence of a scheduled class. */
@Entity(tableName = "class_log")
data class ClassLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleId: Long,
    val epochDay: Long, // java.time.LocalDate.toEpochDay()
    val status: String, // Completed / Half completed / Cancelled / Disturbed
    val notes: String
)

/** Homework assigned to a class. */
@Entity(tableName = "homework")
data class Homework(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val classworkNote: String = "",
    val className: String,
    val deadlineEpochDay: Long,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** Per-student submission/grading status for a homework. */
@Entity(tableName = "homework_submission")
data class HomeworkSubmission(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val homeworkId: Long,
    val studentId: Long,
    val status: String // Done / Half Done / Not Done
)

/** Student roster entry. */
@Entity(tableName = "student")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rollNumber: String,
    val className: String,
    val contactNumber: String = "",
    val guardianName: String = ""
)

/** Free-form performance/activity note attached to a student. */
@Entity(tableName = "student_activity")
data class StudentActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Syllabus for a subject/class; raw text and/or an attached PDF. */
@Entity(tableName = "syllabus")
data class Syllabus(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val className: String,
    val notesText: String = "",
    val pdfUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** Exam definition. */
@Entity(tableName = "exam")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dateEpochDay: Long,
    val targetClasses: String, // comma separated e.g. "5A,10B"
    val subject: String,
    val fullMarks: Double,
    val passMarks: Double
)

/** Marks obtained by a student in an exam. */
@Entity(tableName = "exam_mark")
data class ExamMark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val studentId: Long,
    val marks: Double? = null,
    val remarks: String = ""
)

/** Private teacher journal note. */
@Entity(tableName = "note")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val tag: String, // Lesson Plan / Homework Notes / General
    val createdAt: Long = System.currentTimeMillis()
)

/** Holiday / academic calendar event. */
@Entity(tableName = "holiday")
data class Holiday(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startEpochDay: Long,
    val endEpochDay: Long
)

/** Teacher profile (single row, id = 1). */
@Entity(tableName = "teacher_profile")
data class TeacherProfile(
    @PrimaryKey val id: Long = 1,
    val name: String = "",
    val email: String = "",
    val officeLocation: String = "",
    val subjectSpecialty: String = "",
    val dateOfBirth: String = "",
    val schoolName: String = ""
)

/** Global list of managed classes, e.g. "5A", "10B". */
@Entity(tableName = "managed_class")
data class ManagedClass(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
