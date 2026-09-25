package com.example.medmitra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vital_readings")
data class VitalReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val heartRate: Int,
    val spo2: Int,
    val bodyTemp: Float,
    val steps: Int,
    val isAbnormal: Boolean
)
