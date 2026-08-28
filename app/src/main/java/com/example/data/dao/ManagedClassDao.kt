package com.example.data.dao

import androidx.room.*
import com.example.data.entity.ManagedClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedClassDao {
    @Query("SELECT * FROM managed_classes ORDER BY name ASC")
    fun getAllManagedClasses(): Flow<List<ManagedClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManagedClass(managedClass: ManagedClassEntity): Long

    @Delete
    suspend fun deleteManagedClass(managedClass: ManagedClassEntity)
}
