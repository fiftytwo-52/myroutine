package com.example.data.dao

import androidx.room.*
import com.example.data.entity.TeacherProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherProfileDao {
    @Query("SELECT * FROM teacher_profiles")
    fun getAllTeachers(): Flow<List<TeacherProfileEntity>>

    @Query("SELECT * FROM teacher_profiles WHERE id = :id")
    suspend fun getTeacherById(id: Int): TeacherProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherProfileEntity): Long

    @Update
    suspend fun updateTeacher(teacher: TeacherProfileEntity)

    @Delete
    suspend fun deleteTeacher(teacher: TeacherProfileEntity)

    @Query("DELETE FROM teacher_profiles")
    suspend fun deleteAllTeachers()
}
