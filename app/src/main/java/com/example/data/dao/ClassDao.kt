package com.example.data.dao

import androidx.room.*
import com.example.data.entity.ClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes")
    fun getAllClasses(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getClassById(id: Int): ClassEntity?

    @Query("SELECT * FROM classes WHERE dayOfWeek = :dayOfWeek")
    fun getClassesForDay(dayOfWeek: Int): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE teacherId = :teacherId")
    fun getClassesByTeacher(teacherId: Int): Flow<List<ClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity): Long

    @Update
    suspend fun updateClass(classEntity: ClassEntity)

    @Delete
    suspend fun deleteClass(classEntity: ClassEntity)

    @Query("DELETE FROM classes")
    suspend fun deleteAllClasses()
}
