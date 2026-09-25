package com.example.medmitra.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.medmitra.MainActivity
import com.example.medmitra.R
import com.example.medmitra.data.local.UserPreferencesManager
import com.example.medmitra.sos.FallDetector

class MedMitraForegroundService : Service() {

    private var fallDetector: FallDetector? = null

    companion object {
        private const val TAG = "MedMitraFGService"
        const val CHANNEL_ID = "medmitra_foreground_service_channel"
        const val CHANNEL_NAME = "MedMitra Background Monitor"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, MedMitraForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start foreground service safely", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MedMitraForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MedMitraForegroundService onCreate")
        try {
            createNotificationChannel()
            startForegroundWithNotification()
            initBackgroundMonitoring()
        } catch (e: Exception) {
            Log.e(TAG, "Error during service onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MedMitraForegroundService onStartCommand")
        try {
            startForegroundWithNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error during onStartCommand", e)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent low-priority channel for MedMitra background health & fall monitoring."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        try {
            val contentIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MedMitra Active")
                .setContentText("Continuous Vitals, Fall Detection & Reminder Service Running")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build()

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    try {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                        )
                    } catch (e: Throwable) {
                        Log.w(TAG, "Failed to start health FGS, trying default foreground type", e)
                        try {
                            startForeground(
                                NOTIFICATION_ID,
                                notification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                            )
                        } catch (e2: Throwable) {
                            Log.w(TAG, "Special use FGS failed, falling back to standard startForeground", e2)
                            startForeground(NOTIFICATION_ID, notification)
                        }
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                        )
                    } catch (e: Throwable) {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "startForeground execution encountered error", e)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "startForeground call failed completely", e)
        }
    }

    private fun initBackgroundMonitoring() {
        val userPrefs = UserPreferencesManager.getInstance(applicationContext)
        if (userPrefs.isFallDetectionEnabled) {
            if (fallDetector == null) {
                fallDetector = FallDetector(applicationContext).apply {
                    sensitivity = userPrefs.fallSensitivity
                    onFallDetected = {
                        Log.w(TAG, "Background Fall Detected via MedMitraForegroundService!")
                        val emergencyPhone = userPrefs.emergencyContact
                        Log.i(TAG, "Background fall alert registered for emergency contact: $emergencyPhone")
                    }
                    startListening()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fallDetector?.stopListening()
        fallDetector = null
        Log.d(TAG, "MedMitraForegroundService onDestroy")
    }
}
