package com.example.medmitra.sos

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

class FallDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var onFallDetected: (() -> Unit)? = null
    var sensitivity: String = "Medium"

    private var isListening = false
    private var lastFreeFallTimestamp: Long = 0L
    private var lastFallTriggerTimestamp: Long = 0L

    companion object {
        private const val TAG = "FallDetector"
        private const val FREE_FALL_THRESHOLD = 4.5f // m/s^2 (~0.45g)
        private const val FREE_FALL_WINDOW_MS = 1500L
        private const val TRIGGER_COOLDOWN_MS = 5000L
    }

    fun getImpactThreshold(): Float {
        return when (sensitivity.lowercase()) {
            "low" -> 42.0f
            "high" -> 18.0f
            else -> 28.0f
        }
    }

    fun startListening() {
        if (isListening || accelerometer == null) return
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        isListening = true
        Log.d(TAG, "FallDetector started listening to Accelerometer (sensitivity=$sensitivity, threshold=${getImpactThreshold()} m/s²)")
    }

    fun stopListening() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
        Log.d(TAG, "FallDetector stopped listening")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val accelMagnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val currentTime = System.currentTimeMillis()

        // Detect low gravity (free-fall state)
        if (accelMagnitude < FREE_FALL_THRESHOLD) {
            lastFreeFallTimestamp = currentTime
        }

        val impactThreshold = getImpactThreshold()
        val peakThreshold = impactThreshold + 6.0f

        // Detect impact peak
        val isImpactAfterFreeFall = (accelMagnitude > impactThreshold) &&
                (currentTime - lastFreeFallTimestamp < FREE_FALL_WINDOW_MS)
        val isExtremePeak = accelMagnitude > peakThreshold

        if ((isImpactAfterFreeFall || isExtremePeak) && (currentTime - lastFallTriggerTimestamp > TRIGGER_COOLDOWN_MS)) {
            lastFallTriggerTimestamp = currentTime
            Log.w(TAG, "FALL DETECTED! Acceleration peak: $accelMagnitude m/s^2 (Threshold: $impactThreshold m/s²)")
            onFallDetected?.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
