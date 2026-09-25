package com.example.medmitra.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ScheduleWithMedication(
    @Embedded val schedule: MedicationSchedule,
    @Relation(
        parentColumn = "medicationId",
        entityColumn = "id"
    )
    val medication: Medication
)
