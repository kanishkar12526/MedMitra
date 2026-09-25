package com.example.medmitra.worker

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medmitra.MainActivity
import com.example.medmitra.data.local.MedMitraDatabase
import com.example.medmitra.data.local.UserPreferencesManager
import com.example.medmitra.receiver.MedicationActionReceiver
import com.example.medmitra.tts.TTSManager
import com.example.medmitra.tts.getMultilingualAnnouncement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MedicationReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val CHANNEL_NAME = "Medication Reminders"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val scheduleId = inputData.getLong("scheduleId", -1L)
        if (scheduleId == -1L) return@withContext Result.failure()

        val db = MedMitraDatabase.getDatabase(context)
        val scheduleWithMed = db.medicationDao().getScheduleWithMedicationById(scheduleId)
            ?: return@withContext Result.failure()

        val medicationName = scheduleWithMed.medication.name
        val dosage = scheduleWithMed.medication.dosage

        showNotification(medicationName, dosage, scheduleId)
        speakReminder(medicationName, dosage)

        Result.success()
    }

    private fun showNotification(medicationName: String, dosage: String, scheduleId: Long) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for medication reminder notifications"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = (scheduleId % Int.MAX_VALUE).toInt()

        val markTakenIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = MedicationActionReceiver.ACTION_MARK_TAKEN
            putExtra(MedicationActionReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(MedicationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            ((scheduleId * 10) + 1).toInt(),
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = MedicationActionReceiver.ACTION_DISMISS
            putExtra(MedicationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            ((scheduleId * 10) + 2).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle("Medication Reminder: $medicationName")
            .setContentText("Time to take $dosage of $medicationName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Mark as Taken", markTakenPendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasPermission) {
            notificationManager.notify(notificationId, notification)
        }
    }

    private suspend fun speakReminder(medicationName: String, dosage: String) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MedMitra:TTSWakeLock"
        )

        try {
            wakeLock.acquire(15000L)
            val userPrefs = UserPreferencesManager.getInstance(context)
            val lang = userPrefs.appLanguage
            val announcementText = getMultilingualAnnouncement(lang, medicationName)
            val ttsManager = TTSManager(context.applicationContext, lang)
            ttsManager.speakAndWait(announcementText)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
