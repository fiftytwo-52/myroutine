package com.myclass.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassScheduleDao {
    @Query("SELECT * FROM class_schedule ORDER BY dayOfWeek, startTime")
    fun observeAll(): Flow<List<ClassSchedule>>

    @Query("SELECT * FROM class_schedule WHERE dayOfWeek = :day ORDER BY startTime")
    fun observeForDay(day: Int): Flow<List<ClassSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: ClassSchedule): Long

    @Delete
    suspend fun delete(schedule: ClassSchedule)
}

@Dao
interface ClassLogDao {
    @Query("SELECT * FROM class_log WHERE scheduleId = :scheduleId ORDER BY epochDay DESC")
    fun observeForSchedule(scheduleId: Long): Flow<List<ClassLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: ClassLog): Long

    @Delete
    suspend fun delete(log: ClassLog)
}

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homework ORDER BY deadlineEpochDay ASC")
    fun observeAll(): Flow<List<Homework>>

    @Query("SELECT * FROM homework WHERE id = :id")
    fun observeById(id: Long): Flow<Homework?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(homework: Homework): Long

    @Delete
    suspend fun delete(homework: Homework)

    @Query("SELECT * FROM homework_submission WHERE homeworkId = :homeworkId")
    fun observeSubmissions(homeworkId: Long): Flow<List<HomeworkSubmission>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubmission(submission: HomeworkSubmission): Long

    @Query("DELETE FROM homework_submission WHERE homeworkId = :homeworkId")
    suspend fun deleteSubmissions(homeworkId: Long)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM student ORDER BY className, rollNumber")
    fun observeAll(): Flow<List<Student>>

    @Query("SELECT * FROM student WHERE className = :className ORDER BY rollNumber")
    fun observeByClass(className: String): Flow<List<Student>>

    @Query("SELECT * FROM student WHERE id = :id")
    suspend fun getById(id: Long): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(student: Student): Long

    @Delete
    suspend fun delete(student: Student)

    @Query("SELECT * FROM student_activity WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun observeActivities(studentId: Long): Flow<List<StudentActivity>>

    @Insert
    suspend fun insertActivity(activity: StudentActivity): Long

    @Delete
    suspend fun deleteActivity(activity: StudentActivity)
}

@Dao
interface SyllabusDao {
    @Query("SELECT * FROM syllabus ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Syllabus>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(syllabus: Syllabus): Long

    @Delete
    suspend fun delete(syllabus: Syllabus)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exam ORDER BY dateEpochDay ASC")
    fun observeAll(): Flow<List<Exam>>

    @Query("SELECT * FROM exam WHERE id = :id")
    suspend fun getById(id: Long): Exam?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exam: Exam): Long

    @Delete
    suspend fun delete(exam: Exam)

    @Query("SELECT * FROM exam_mark WHERE examId = :examId")
    fun observeMarks(examId: Long): Flow<List<ExamMark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMark(mark: ExamMark): Long
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM note ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note): Long

    @Delete
    suspend fun delete(note: Note)
}

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holiday ORDER BY startEpochDay ASC")
    fun observeAll(): Flow<List<Holiday>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(holiday: Holiday): Long

    @Delete
    suspend fun delete(holiday: Holiday)
}

@Dao
interface TeacherProfileDao {
    @Query("SELECT * FROM teacher_profile WHERE id = 1")
    fun observeProfile(): Flow<TeacherProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: TeacherProfile)
}

@Dao
interface ManagedClassDao {
    @Query("SELECT * FROM managed_class ORDER BY name")
    fun observeAll(): Flow<List<ManagedClass>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(managedClass: ManagedClass): Long

    @Delete
    suspend fun delete(managedClass: ManagedClass)
}
