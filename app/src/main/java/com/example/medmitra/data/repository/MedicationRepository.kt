package com.example.medmitra.data.repository

import com.example.medmitra.data.local.Medication
import com.example.medmitra.data.local.MedicationDao
import com.example.medmitra.data.local.MedicationSchedule
import com.example.medmitra.data.local.ScheduleStatus
import com.example.medmitra.data.local.ScheduleWithMedication
import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val medicationDao: MedicationDao) {

    suspend fun addMedication(medication: Medication): Long {
        return medicationDao.insertMedication(medication)
    }

    suspend fun addSchedule(schedule: MedicationSchedule): Long {
        return medicationDao.insertSchedule(schedule)
    }

    fun getAllActiveMedications(): Flow<List<Medication>> {
        return medicationDao.getAllActiveMedications()
    }

    suspend fun getActiveMedicationsList(): List<Medication> {
        return medicationDao.getActiveMedicationsList()
    }

    suspend fun getMedicationsCount(): Int {
        return medicationDao.getMedicationsCount()
    }

    fun getSchedulesForTimeRange(startTime: Long, endTime: Long): Flow<List<ScheduleWithMedication>> {
        return medicationDao.getSchedulesForTimeRange(startTime, endTime)
    }

    suspend fun getSchedulesForMedicationInTimeRange(medicationId: Long, startTime: Long, endTime: Long): List<MedicationSchedule> {
        return medicationDao.getSchedulesForMedicationInTimeRange(medicationId, startTime, endTime)
    }

    suspend fun getSchedulesForMedication(medicationId: Long): List<MedicationSchedule> {
        return medicationDao.getSchedulesForMedication(medicationId)
    }

    fun getMissedSchedules(): Flow<List<ScheduleWithMedication>> {
        return medicationDao.getMissedSchedules()
    }

    suspend fun updateScheduleStatus(scheduleId: Long, status: ScheduleStatus) {
        medicationDao.updateScheduleStatus(scheduleId, status)
    }

    suspend fun updatePastPendingSchedulesToMissed(currentTimeMillis: Long) {
        val pending = medicationDao.getPastPendingSchedules(currentTimeMillis)
        for (schedule in pending) {
            medicationDao.updateScheduleStatus(schedule.id, ScheduleStatus.MISSED)
        }
    }
    
    suspend fun getScheduleWithMedicationById(scheduleId: Long): ScheduleWithMedication? {
        return medicationDao.getScheduleWithMedicationById(scheduleId)
    }
}
