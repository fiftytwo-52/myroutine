package com.example.data.dao

import androidx.room.*
import com.example.data.entity.SyllabusEntity
import com.example.data.entity.UnitEntity
import com.example.data.entity.TopicEntity
import com.example.data.entity.TopicProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyllabusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyllabus(syllabus: SyllabusEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopicProgress(progress: TopicProgressEntity)

    @Query("SELECT * FROM syllabus ORDER BY createdAt DESC")
    fun getAllSyllabuses(): Flow<List<SyllabusEntity>>

    @Query("SELECT * FROM units WHERE syllabusId = :syllabusId ORDER BY unitNumber ASC")
    fun getUnitsForSyllabus(syllabusId: Int): Flow<List<UnitEntity>>

    @Query("SELECT * FROM topics WHERE unitId = :unitId ORDER BY topicNumber ASC")
    fun getTopicsForUnit(unitId: Int): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE unitId IN (SELECT id FROM units WHERE syllabusId = :syllabusId) ORDER BY id ASC")
    suspend fun getAllTopicsForSyllabus(syllabusId: Int): List<TopicEntity>

    @Query("SELECT * FROM topic_progress WHERE classId = :classId")
    fun getTopicProgressForClass(classId: Int): Flow<List<TopicProgressEntity>>

    @Query("SELECT * FROM topic_progress WHERE topicId = :topicId AND classId = :classId LIMIT 1")
    suspend fun getProgress(topicId: Int, classId: Int): TopicProgressEntity?
    
    @Query("DELETE FROM syllabus WHERE id = :syllabusId")
    suspend fun deleteSyllabus(syllabusId: Int)
}
