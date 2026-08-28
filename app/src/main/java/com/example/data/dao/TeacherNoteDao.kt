package com.example.data.dao

import androidx.room.*
import com.example.data.entity.TeacherNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherNoteDao {
    @Query("SELECT * FROM teacher_notes ORDER BY createdDateMillis DESC")
    fun getAllNotes(): Flow<List<TeacherNoteEntity>>

    @Query("SELECT * FROM teacher_notes WHERE eventEpochDay IS NOT NULL ORDER BY eventEpochDay ASC, id ASC")
    fun getDatedEvents(): Flow<List<TeacherNoteEntity>>

    @Query("SELECT * FROM teacher_notes WHERE eventEpochDay = :epochDay ORDER BY id ASC")
    fun getEventsForDay(epochDay: Long): Flow<List<TeacherNoteEntity>>

    @Query("SELECT * FROM teacher_notes WHERE tag = :tag ORDER BY createdDateMillis DESC")
    fun getNotesByTag(tag: String): Flow<List<TeacherNoteEntity>>

    @Query("UPDATE teacher_notes SET eventStatus = :status WHERE id = :noteId")
    suspend fun updateEventStatus(noteId: Int, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: TeacherNoteEntity): Long

    @Update
    suspend fun updateNote(note: TeacherNoteEntity)

    @Delete
    suspend fun deleteNote(note: TeacherNoteEntity)

    @Query("DELETE FROM teacher_notes")
    suspend fun deleteAllNotes()
}
