package com.example.medmitra.vitals

import android.content.Context
import android.util.Log
import com.example.medmitra.data.local.MedMitraDatabase
import com.example.medmitra.data.local.VitalReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class VitalsManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val database = MedMitraDatabase.getDatabase(appContext)
    private val vitalDao = database.vitalReadingDao()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentReading = MutableStateFlow<VitalReading?>(null)
    val currentReading: StateFlow<VitalReading?> = _currentReading.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _abnormalVitalEvent = MutableSharedFlow<VitalReading>(extraBufferCapacity = 1)
    val abnormalVitalEvent: SharedFlow<VitalReading> = _abnormalVitalEvent.asSharedFlow()

    val recentReadings: StateFlow<List<VitalReading>> = vitalDao.getRecentReadings(15)
        .catch { e ->
            Log.e("VitalsManager", "Error fetching recent readings", e)
            emit(emptyList())
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList<VitalReading>()
        )

    private var currentSteps = 4250
    private var simulationJob: Job? = null

    init {
        scope.launch {
            try {
                vitalDao.getLatestReading().collect { latest ->
                    if (latest != null && _currentReading.value == null) {
                        _currentReading.value = latest
                    }
                }
            } catch (e: Throwable) {
                Log.e("VitalsManager", "Error getting latest reading", e)
            }
        }
        startSensorSync()
    }

    fun startSensorSync() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            while (isActive) {
                delay(3000L) // 3-second live pulse from smartwatch
                if (_isConnected.value) {
                    generateNextReading()
                }
            }
        }
    }

    fun toggleWatchConnection(connected: Boolean) {
        _isConnected.value = connected
    }

    private suspend fun generateNextReading() {
        val prev = _currentReading.value
        val heartRate = if (prev != null && !prev.isAbnormal) {
            (prev.heartRate + Random.nextInt(-2, 3)).coerceIn(65, 95)
        } else {
            Random.nextInt(70, 84)
        }

        val spo2 = if (prev != null && !prev.isAbnormal) {
            (prev.spo2 + Random.nextInt(-1, 2)).coerceIn(95, 100)
        } else {
            Random.nextInt(96, 99)
        }

        val temp = if (prev != null && !prev.isAbnormal) {
            ((prev.bodyTemp * 10).toInt() + Random.nextInt(-1, 2)).toFloat() / 10f
        } else {
            36.6f
        }.coerceIn(36.2f, 37.1f)

        currentSteps += Random.nextInt(0, 3)

        val newReading = VitalReading(
            timestamp = System.currentTimeMillis(),
            heartRate = heartRate,
            spo2 = spo2,
            bodyTemp = temp,
            steps = currentSteps,
            isAbnormal = checkIsAbnormal(heartRate, spo2)
        )

        processNewReading(newReading)
    }

    fun simulateAbnormalHighHR() {
        scope.launch {
            val reading = VitalReading(
                timestamp = System.currentTimeMillis(),
                heartRate = Random.nextInt(128, 145), // Out of bounds > 120
                spo2 = 96,
                bodyTemp = 37.2f,
                steps = currentSteps,
                isAbnormal = true
            )
            processNewReading(reading)
        }
    }

    fun simulateAbnormalLowHR() {
        scope.launch {
            val reading = VitalReading(
                timestamp = System.currentTimeMillis(),
                heartRate = Random.nextInt(40, 48), // Out of bounds < 50
                spo2 = 95,
                bodyTemp = 36.3f,
                steps = currentSteps,
                isAbnormal = true
            )
            processNewReading(reading)
        }
    }

    fun simulateAbnormalLowSpO2() {
        scope.launch {
            val reading = VitalReading(
                timestamp = System.currentTimeMillis(),
                heartRate = 84,
                spo2 = Random.nextInt(82, 89), // Out of bounds < 90
                bodyTemp = 36.5f,
                steps = currentSteps,
                isAbnormal = true
            )
            processNewReading(reading)
        }
    }

    fun simulateNormalReading() {
        scope.launch {
            val reading = VitalReading(
                timestamp = System.currentTimeMillis(),
                heartRate = Random.nextInt(72, 82),
                spo2 = Random.nextInt(97, 100),
                bodyTemp = 36.6f,
                steps = currentSteps,
                isAbnormal = false
            )
            processNewReading(reading)
        }
    }

    private suspend fun processNewReading(reading: VitalReading) {
        _currentReading.value = reading
        vitalDao.insertReading(reading)

        if (reading.isAbnormal) {
            _abnormalVitalEvent.emit(reading)
        }
    }

    private fun checkIsAbnormal(heartRate: Int, spo2: Int): Boolean {
        return heartRate < 50 || heartRate > 120 || spo2 < 90
    }

    companion object {
        @Volatile
        private var INSTANCE: VitalsManager? = null

        fun getInstance(context: Context): VitalsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = VitalsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
