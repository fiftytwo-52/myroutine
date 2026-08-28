package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.entity.*

@Database(
    entities = [
        ClassEntity::class,
        HomeworkEntity::class,
        TeacherProfileEntity::class,
        StudentEntity::class,
        TeacherNoteEntity::class,
        HolidayEntity::class,
        ClassLogEntity::class,
        ExamEntity::class,
        ExamMarkEntity::class,
        HomeworkSubmissionEntity::class,
        ManagedClassEntity::class,
        StudentActivityEntity::class,
        SyllabusEntity::class,
        UnitEntity::class,
        TopicEntity::class,
        TopicProgressEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun teacherDao(): TeacherProfileDao
    abstract fun studentDao(): StudentDao
    abstract fun teacherNoteDao(): TeacherNoteDao
    abstract fun holidayDao(): HolidayDao
    abstract fun classLogDao(): ClassLogDao
    abstract fun examDao(): ExamDao
    abstract fun homeworkSubmissionDao(): HomeworkSubmissionDao
    abstract fun managedClassDao(): ManagedClassDao
    abstract fun studentActivityDao(): StudentActivityDao
    abstract fun syllabusDao(): SyllabusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "classflow_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
