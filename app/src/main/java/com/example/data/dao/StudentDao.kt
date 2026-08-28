package com.example.data.dao

import androidx.room.*
import com.example.data.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE className = :className ORDER BY name ASC")
    fun getStudentsByClass(className: String): Flow<List<StudentEntity>>

    @Query("DELETE FROM students WHERE className = :className")
    suspend fun deleteStudentsByClass(className: String)

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Int): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()
}
