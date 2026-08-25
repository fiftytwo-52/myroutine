package com.myclass.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClassSchedule::class,
        ClassLog::class,
        Homework::class,
        HomeworkSubmission::class,
        Student::class,
        StudentActivity::class,
        Syllabus::class,
        Exam::class,
        ExamMark::class,
        Note::class,
        Holiday::class,
        TeacherProfile::class,
        ManagedClass::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun classScheduleDao(): ClassScheduleDao
    abstract fun classLogDao(): ClassLogDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun studentDao(): StudentDao
    abstract fun syllabusDao(): SyllabusDao
    abstract fun examDao(): ExamDao
    abstract fun noteDao(): NoteDao
    abstract fun holidayDao(): HolidayDao
    abstract fun teacherProfileDao(): TeacherProfileDao
    abstract fun managedClassDao(): ManagedClassDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myclass.db"
                ).build().also { INSTANCE = it }
            }
    }
}
