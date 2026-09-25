package com.example.medmitra.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medmitra.data.local.MedMitraDatabase
import com.example.medmitra.data.local.ScheduleStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_TAKEN = "com.example.medmitra.ACTION_MARK_TAKEN"
        const val ACTION_DISMISS = "com.example.medmitra.ACTION_DISMISS"
        const val EXTRA_SCHEDULE_ID = "scheduleId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            ACTION_MARK_TAKEN -> {
                val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                if (scheduleId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = MedMitraDatabase.getDatabase(context)
                            db.medicationDao().updateScheduleStatus(scheduleId, ScheduleStatus.TAKEN)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
            }
            ACTION_DISMISS -> {
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
            }
        }
    }
}
