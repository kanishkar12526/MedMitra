package com.example.medmitra.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Medication::class, MedicationSchedule::class, VitalReading::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MedMitraDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun vitalReadingDao(): VitalReadingDao

    companion object {
        @Volatile
        private var INSTANCE: MedMitraDatabase? = null

        fun getDatabase(context: Context): MedMitraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedMitraDatabase::class.java,
                    "medmitra_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
