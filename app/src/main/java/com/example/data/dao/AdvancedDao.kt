package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassLogDao {
    @Query("SELECT * FROM class_log WHERE dateString = :dateString")
    fun getLogsForDate(dateString: String): Flow<List<ClassLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ClassLogEntity)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Query("SELECT * FROM exam_marks WHERE examId = :examId AND classId = :classId")
    fun getMarksForExamAndClass(examId: Int, classId: Int): Flow<List<ExamMarkEntity>>

    @Query("SELECT * FROM exam_marks")
    fun getAllMarks(): Flow<List<ExamMarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMark(mark: ExamMarkEntity)

    @Query("DELETE FROM exams WHERE id = :examId")
    suspend fun deleteExam(examId: Int)

    @Query("DELETE FROM exam_marks WHERE examId = :examId")
    suspend fun deleteMarksForExam(examId: Int)

    @Query("SELECT * FROM exam_marks WHERE examId = :examId")
    fun getAllMarksForExam(examId: Int): Flow<List<ExamMarkEntity>>

    @Query("SELECT * FROM exam_marks WHERE studentId = :studentId")
    fun getMarksForStudent(studentId: Int): Flow<List<ExamMarkEntity>>
}

@Dao
interface StudentActivityDao {
    @Query("SELECT * FROM student_activities WHERE studentId = :studentId")
    fun getActivitiesForStudent(studentId: Int): Flow<List<StudentActivityEntity>>

    @Query("SELECT * FROM student_activities")
    fun getAllActivities(): Flow<List<StudentActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: StudentActivityEntity)

    @Query("DELETE FROM student_activities WHERE id = :id")
    suspend fun deleteActivity(id: Int)

    @Query("DELETE FROM student_activities WHERE studentId = :studentId")
    suspend fun deleteAllActivitiesForStudent(studentId: Int)
}

@Dao
interface HomeworkSubmissionDao {
    @Query("SELECT * FROM homework_submission WHERE homeworkId = :homeworkId")
    fun getSubmissionsForHomework(homeworkId: Int): Flow<List<HomeworkSubmissionEntity>>

    @Query("SELECT * FROM homework_submission WHERE studentId = :studentId AND (status = 'Not Done' OR status = 'not done')")
    suspend fun getNotDoneSubmissionsForStudent(studentId: Int): List<HomeworkSubmissionEntity>

    @Query("SELECT * FROM homework_submission")
    fun getAllSubmissions(): Flow<List<HomeworkSubmissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: HomeworkSubmissionEntity)

    @Query("DELETE FROM homework_submission WHERE studentId = :studentId")
    suspend fun deleteAllSubmissionsForStudent(studentId: Int)
}
