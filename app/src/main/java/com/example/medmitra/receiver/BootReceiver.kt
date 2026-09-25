package com.example.medmitra.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.medmitra.data.local.MedMitraDatabase
import com.example.medmitra.data.local.ScheduleStatus
import com.example.medmitra.service.MedMitraForegroundService
import com.example.medmitra.worker.MedicationReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive triggered with action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            // 1. Start Persistent Foreground Service
            MedMitraForegroundService.startService(context)

            // 2. Reschedule future pending medication reminders with WorkManager
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = MedMitraDatabase.getDatabase(context)
                    val now = System.currentTimeMillis()
                    val activeMeds = db.medicationDao().getActiveMedicationsList()
                    val activeMedIds = activeMeds.map { it.id }.toSet()

                    val workManager = WorkManager.getInstance(context)

                    for (medId in activeMedIds) {
                        val schedules = db.medicationDao().getSchedulesForMedication(medId)
                        for (schedule in schedules) {
                            if (schedule.status == ScheduleStatus.PENDING && schedule.scheduledTimeMillis > now) {
                                val delay = schedule.scheduledTimeMillis - now
                                val inputData = Data.Builder()
                                    .putLong("scheduleId", schedule.id)
                                    .build()

                                val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                    .setInputData(inputData)
                                    .build()

                                workManager.enqueue(workRequest)
                                Log.i(TAG, "Rescheduled medication schedule ID ${schedule.id} with delay $delay ms")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling reminders on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
