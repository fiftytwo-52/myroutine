package com.example.data.dao

import androidx.room.*
import com.example.data.entity.HolidayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays ORDER BY startDate ASC")
    fun getAllHolidays(): Flow<List<HolidayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: HolidayEntity): Long

    @Update
    suspend fun updateHoliday(holiday: HolidayEntity)

    @Delete
    suspend fun deleteHoliday(holiday: HolidayEntity)

    @Query("DELETE FROM holidays")
    suspend fun deleteAllHolidays()
}
