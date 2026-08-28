package com.example.data.dao

import androidx.room.*
import com.example.data.entity.ClassEntity
import com.example.data.entity.HomeworkEntity
import kotlinx.coroutines.flow.Flow

data class HomeworkWithClass(
    @Embedded val homework: HomeworkEntity,
    @Relation(
        parentColumn = "classId",
        entityColumn = "id"
    )
    val classEntity: ClassEntity?
)

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homework")
    fun getAllHomework(): Flow<List<HomeworkEntity>>

    @Transaction
    @Query("SELECT * FROM homework")
    fun getAllHomeworkWithClass(): Flow<List<HomeworkWithClass>>

    @Query("SELECT * FROM homework WHERE classId = :classId")
    fun getHomeworkForClass(classId: Int): Flow<List<HomeworkEntity>>

    @Transaction
    @Query("SELECT * FROM homework WHERE isCompleted = :isCompleted")
    fun getHomeworkByCompletionWithClass(isCompleted: Boolean): Flow<List<HomeworkWithClass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomework(homework: HomeworkEntity): Long

    @Update
    suspend fun updateHomework(homework: HomeworkEntity)

    @Delete
    suspend fun deleteHomework(homework: HomeworkEntity)

    @Query("DELETE FROM homework")
    suspend fun deleteAllHomework()
}
