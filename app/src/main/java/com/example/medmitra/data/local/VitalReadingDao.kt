package com.example.medmitra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalReadingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: VitalReading): Long

    @Query("SELECT * FROM vital_readings ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReading(): Flow<VitalReading?>

    @Query("SELECT * FROM vital_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int = 20): Flow<List<VitalReading>>

    @Query("SELECT * FROM vital_readings WHERE isAbnormal = 1 ORDER BY timestamp DESC")
    fun getAbnormalReadings(): Flow<List<VitalReading>>

    @Query("DELETE FROM vital_readings")
    suspend fun clearAllReadings()
}
