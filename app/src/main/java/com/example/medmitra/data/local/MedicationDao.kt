package com.example.medmitra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MedicationSchedule): Long

    @Query("SELECT * FROM medications WHERE isActive = 1")
    fun getAllActiveMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE isActive = 1")
    suspend fun getActiveMedicationsList(): List<Medication>

    @Query("SELECT COUNT(*) FROM medications")
    suspend fun getMedicationsCount(): Int

    @Transaction
    @Query("SELECT * FROM schedules WHERE scheduledTimeMillis >= :startTime AND scheduledTimeMillis <= :endTime ORDER BY scheduledTimeMillis ASC")
    fun getSchedulesForTimeRange(startTime: Long, endTime: Long): Flow<List<ScheduleWithMedication>>

    @Transaction
    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId AND scheduledTimeMillis >= :startTime AND scheduledTimeMillis <= :endTime")
    suspend fun getSchedulesForMedicationInTimeRange(medicationId: Long, startTime: Long, endTime: Long): List<MedicationSchedule>

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId")
    suspend fun getSchedulesForMedication(medicationId: Long): List<MedicationSchedule>

    @Transaction
    @Query("SELECT * FROM schedules WHERE status = 'MISSED' ORDER BY scheduledTimeMillis DESC")
    fun getMissedSchedules(): Flow<List<ScheduleWithMedication>>

    @Query("SELECT * FROM schedules WHERE status = 'PENDING' AND scheduledTimeMillis < :cutoffTimeMillis")
    suspend fun getPastPendingSchedules(cutoffTimeMillis: Long): List<MedicationSchedule>

    @Query("UPDATE schedules SET status = :status WHERE id = :scheduleId")
    suspend fun updateScheduleStatus(scheduleId: Long, status: ScheduleStatus)

    @Transaction
    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    suspend fun getScheduleWithMedicationById(scheduleId: Long): ScheduleWithMedication?
}
