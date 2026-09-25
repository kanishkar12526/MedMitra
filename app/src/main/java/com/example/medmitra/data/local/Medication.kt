package com.example.medmitra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val type: String, // e.g. Pill, Syrup
    val instructions: String,
    val isActive: Boolean = true
)
