package com.example.medmitra

import android.app.Application
import android.util.Log
import com.example.medmitra.data.local.MedMitraDatabase
import com.example.medmitra.data.repository.MedicationRepository

class MedMitraApplication : Application() {

    val database: MedMitraDatabase by lazy {
        MedMitraDatabase.getDatabase(this)
    }

    val repository: MedicationRepository by lazy {
        MedicationRepository(database.medicationDao())
    }

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MedMitraApplication", "Uncaught exception in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            // Initialize database singleton safely
            database
        } catch (e: Exception) {
            Log.e("MedMitraApplication", "Error initializing database safely", e)
        }
    }
}
