package com.example.medmitra.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.medmitra.MedMitraApplication
import com.example.medmitra.data.local.MedMitraDatabase
import com.example.medmitra.data.local.Medication
import com.example.medmitra.data.local.MedicationSchedule
import com.example.medmitra.data.local.ScheduleStatus
import com.example.medmitra.data.local.ScheduleWithMedication
import com.example.medmitra.data.local.UserPreferencesManager
import com.example.medmitra.data.local.VitalReading
import com.example.medmitra.data.repository.MedicationRepository
import com.example.medmitra.vitals.VitalsManager
import com.example.medmitra.worker.MedicationReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicationRepository
    private val workManager: WorkManager
    private val vitalsManager = VitalsManager.getInstance(application)
    private val userPrefs = UserPreferencesManager.getInstance(application)

    private val _isDemoModeEnabled = MutableStateFlow(userPrefs.isDemoModeEnabled)
    val isDemoModeEnabled: StateFlow<Boolean> = _isDemoModeEnabled.asStateFlow()

    private val _activeMedications = MutableStateFlow<List<Medication>>(emptyList())
    val activeMedications: StateFlow<List<Medication>> = _activeMedications.asStateFlow()

    private val _todaySchedules = MutableStateFlow<List<ScheduleWithMedication>>(emptyList())
    val todaySchedules: StateFlow<List<ScheduleWithMedication>> = _todaySchedules.asStateFlow()

    private val _missedSchedules = MutableStateFlow<List<ScheduleWithMedication>>(emptyList())
    val missedSchedules: StateFlow<List<ScheduleWithMedication>> = _missedSchedules.asStateFlow()

    // Real DB data holders
    private var realTodaySchedules: List<ScheduleWithMedication> = emptyList()
    private var realMissedSchedules: List<ScheduleWithMedication> = emptyList()

    // In-memory demo schedules state for dynamic demo interaction
    private val demoTodaySchedulesState = MutableStateFlow(getDemoTodaySchedules())
    private val demoMissedSchedulesState = MutableStateFlow(getDemoMissedSchedules())

    // Vitals flows
    private val _currentVitalReading = MutableStateFlow<VitalReading?>(null)
    val currentVitalReading: StateFlow<VitalReading?> = _currentVitalReading.asStateFlow()

    private val _isWatchConnected = MutableStateFlow(true)
    val isWatchConnected: StateFlow<Boolean> = _isWatchConnected.asStateFlow()

    private val _recentVitalReadings = MutableStateFlow<List<VitalReading>>(emptyList())
    val recentVitalReadings: StateFlow<List<VitalReading>> = _recentVitalReadings.asStateFlow()

    init {
        val app = application as? MedMitraApplication
        repository = app?.repository ?: MedicationRepository(MedMitraDatabase.getDatabase(application).medicationDao())
        workManager = WorkManager.getInstance(application)

        initializeAndSyncDatabase()
        loadActiveMedications()
        loadTodaySchedules()
        loadMissedSchedules()
        observeVitals()
        observeDemoMode()
    }

    private fun observeDemoMode() {
        viewModelScope.launch {
            userPrefs.demoModeFlow.collect { isDemo ->
                _isDemoModeEnabled.value = isDemo
                refreshDisplayedData()
            }
        }
    }

    private fun observeVitals() {
        viewModelScope.launch {
            vitalsManager.currentReading.collect { reading ->
                if (_isDemoModeEnabled.value) {
                    _currentVitalReading.value = reading ?: getDemoCurrentVital()
                } else {
                    _currentVitalReading.value = reading
                }
            }
        }

        viewModelScope.launch {
            vitalsManager.isConnected.collect { connected ->
                if (_isDemoModeEnabled.value) {
                    _isWatchConnected.value = true
                } else {
                    _isWatchConnected.value = connected
                }
            }
        }

        viewModelScope.launch {
            vitalsManager.recentReadings.collect { readings ->
                if (_isDemoModeEnabled.value) {
                    _recentVitalReadings.value = if (readings.isNotEmpty()) readings else getDemoRecentVitals()
                } else {
                    _recentVitalReadings.value = readings
                }
            }
        }
    }

    private fun refreshDisplayedData() {
        val isDemo = _isDemoModeEnabled.value
        if (isDemo) {
            _todaySchedules.value = demoTodaySchedulesState.value
            _missedSchedules.value = demoMissedSchedulesState.value
            _isWatchConnected.value = true
            _currentVitalReading.value = vitalsManager.currentReading.value ?: getDemoCurrentVital()
            _recentVitalReadings.value = if (vitalsManager.recentReadings.value.isNotEmpty()) vitalsManager.recentReadings.value else getDemoRecentVitals()
        } else {
            _todaySchedules.value = realTodaySchedules
            _missedSchedules.value = realMissedSchedules
            _isWatchConnected.value = vitalsManager.isConnected.value
            _currentVitalReading.value = vitalsManager.currentReading.value
            _recentVitalReadings.value = vitalsManager.recentReadings.value
        }
    }

    private fun initializeAndSyncDatabase() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                // Mark past pending schedules as MISSED
                repository.updatePastPendingSchedulesToMissed(now)

                val count = repository.getMedicationsCount()
                if (count == 0) {
                    // Initial launch on completely empty DB: seed default sample medications
                    seedInitialSampleData()
                } else {
                    // Data exists in Room DB -> Preserve user saved data and ensure today's schedule instances exist
                    ensureTodaySchedulesForActiveMedications()
                }
            } catch (e: Throwable) {
                Log.e("DashboardViewModel", "Error in initializeAndSyncDatabase", e)
            }
        }
    }

    private suspend fun seedInitialSampleData() {
        val aspirin = Medication(
            name = "Aspirin",
            dosage = "100mg",
            type = "Pill",
            instructions = "Take 1 pill in the morning with water"
        )
        val aspirinId = repository.addMedication(aspirin)

        val vitD = Medication(
            name = "Vitamin D3",
            dosage = "1000 IU",
            type = "Capsule",
            instructions = "Take 1 capsule after breakfast"
        )
        val vitDId = repository.addMedication(vitD)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val sched1 = repository.addSchedule(MedicationSchedule(medicationId = aspirinId, scheduledTimeMillis = cal.timeInMillis))
        scheduleNotification(sched1, cal.timeInMillis)

        cal.set(Calendar.HOUR_OF_DAY, 20)
        val sched2 = repository.addSchedule(MedicationSchedule(medicationId = vitDId, scheduledTimeMillis = cal.timeInMillis))
        scheduleNotification(sched2, cal.timeInMillis)
    }

    private suspend fun ensureTodaySchedulesForActiveMedications() {
        val startOfDay = getStartOfDayMillis()
        val endOfDay = getEndOfDayMillis()
        val activeMeds = repository.getActiveMedicationsList()

        for (med in activeMeds) {
            val existingTodaySchedules = repository.getSchedulesForMedicationInTimeRange(med.id, startOfDay, endOfDay)
            if (existingTodaySchedules.isEmpty()) {
                // Check historical schedules to get standard times
                val allSchedules = repository.getSchedulesForMedication(med.id)
                val distinctTimes = allSchedules.map { schedule ->
                    val cal = Calendar.getInstance().apply { timeInMillis = schedule.scheduledTimeMillis }
                    Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                }.distinct()

                val targetTimes = if (distinctTimes.isNotEmpty()) distinctTimes else listOf(Pair(8, 0))

                for ((hour, minute) in targetTimes) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val schedId = repository.addSchedule(
                        MedicationSchedule(
                            medicationId = med.id,
                            scheduledTimeMillis = cal.timeInMillis
                        )
                    )
                    scheduleNotification(schedId, cal.timeInMillis)
                }
            }
        }
    }

    fun simulateHighHR() = vitalsManager.simulateAbnormalHighHR()
    fun simulateLowHR() = vitalsManager.simulateAbnormalLowHR()
    fun simulateLowSpO2() = vitalsManager.simulateAbnormalLowSpO2()
    fun simulateNormalVitals() = vitalsManager.simulateNormalReading()
    fun toggleWatchConnection(connected: Boolean) = vitalsManager.toggleWatchConnection(connected)

    private fun loadActiveMedications() {
        viewModelScope.launch {
            try {
                repository.getAllActiveMedications().collect { meds ->
                    _activeMedications.value = meds
                }
            } catch (e: Throwable) {
                Log.e("DashboardViewModel", "Error loading active medications", e)
            }
        }
    }

    private fun loadTodaySchedules() {
        val startOfDay = getStartOfDayMillis()
        val endOfDay = getEndOfDayMillis()

        viewModelScope.launch {
            try {
                repository.getSchedulesForTimeRange(startOfDay, endOfDay).collect { schedules ->
                    realTodaySchedules = schedules
                    if (!_isDemoModeEnabled.value) {
                        _todaySchedules.value = schedules
                    }
                }
            } catch (e: Throwable) {
                Log.e("DashboardViewModel", "Error loading today schedules", e)
            }
        }

        viewModelScope.launch {
            try {
                demoTodaySchedulesState.collect { demoSchedules ->
                    if (_isDemoModeEnabled.value) {
                        _todaySchedules.value = demoSchedules
                    }
                }
            } catch (e: Throwable) {
                Log.e("DashboardViewModel", "Error in demo schedules flow", e)
            }
        }
    }

    private fun loadMissedSchedules() {
        viewModelScope.launch {
            try {
                repository.getMissedSchedules().collect { schedules ->
                    realMissedSchedules = schedules
                    if (!_isDemoModeEnabled.value) {
                        _missedSchedules.value = schedules
                    }
                }
            } catch (e: Throwable) {
                Log.e("DashboardViewModel", "Error loading missed schedules", e)
            }
        }

        viewModelScope.launch {
            try {
                demoMissedSchedulesState.collect { demoSchedules ->
                    if (_isDemoModeEnabled.value) {
                        _missedSchedules.value = demoSchedules
                    }
                }
            } catch (e: Throwable) {
                Log.e("DashboardViewModel", "Error in demo missed flow", e)
            }
        }
    }

    fun addMedicationAndSchedule(name: String, dosage: String, type: String, instructions: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                val medication = Medication(
                    name = name,
                    dosage = dosage,
                    type = type,
                    instructions = instructions
                )
                val medId = repository.addMedication(medication)

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                // If the time is in the past, schedule for next day
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                val schedule = MedicationSchedule(
                    medicationId = medId,
                    scheduledTimeMillis = calendar.timeInMillis
                )
                
                val scheduleId = repository.addSchedule(schedule)
                scheduleNotification(scheduleId, calendar.timeInMillis)
            } catch (e: Throwable) {
                Log.e("DashboardViewModel", "Error adding medication", e)
            }
        }
    }

    fun markScheduleAsTaken(scheduleId: Long) {
        viewModelScope.launch {
            try {
                if (_isDemoModeEnabled.value && scheduleId < 0) {
                    // Update in demo today schedules
                    val updatedToday = demoTodaySchedulesState.value.map { item ->
                        if (item.schedule.id == scheduleId) {
                            item.copy(schedule = item.schedule.copy(status = ScheduleStatus.TAKEN))
                        } else item
                    }
                    demoTodaySchedulesState.value = updatedToday

                    // Update in demo missed schedules
                    val updatedMissed = demoMissedSchedulesState.value.map { item ->
                        if (item.schedule.id == scheduleId) {
                            item.copy(schedule = item.schedule.copy(status = ScheduleStatus.TAKEN))
                        } else item
                    }
                    demoMissedSchedulesState.value = updatedMissed
                } else {
                    repository.updateScheduleStatus(scheduleId, ScheduleStatus.TAKEN)
                }
            } catch (e: Throwable) {
                Log.e("DashboardViewModel", "Error marking schedule as taken", e)
            }
        }
    }

    private fun scheduleNotification(scheduleId: Long, timeInMillis: Long) {
        val delay = timeInMillis - System.currentTimeMillis()
        if (delay <= 0) return

        val data = Data.Builder()
            .putLong("scheduleId", scheduleId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        workManager.enqueue(workRequest)
    }

    private fun getStartOfDayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private fun getDemoTodaySchedules(): List<ScheduleWithMedication> {
        val time8am = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val time1pm = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 13); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val time8pm = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val time9pm = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 21); set(Calendar.MINUTE, 30); set(Calendar.SECOND, 0) }.timeInMillis

        return listOf(
            ScheduleWithMedication(
                schedule = MedicationSchedule(id = -101L, medicationId = 101L, scheduledTimeMillis = time8am, status = ScheduleStatus.TAKEN),
                medication = Medication(id = 101L, name = "Amoxicillin", dosage = "500mg", type = "Capsule", instructions = "Take 1 capsule with full glass of water")
            ),
            ScheduleWithMedication(
                schedule = MedicationSchedule(id = -102L, medicationId = 102L, scheduledTimeMillis = time1pm, status = ScheduleStatus.PENDING),
                medication = Medication(id = 102L, name = "Atorvastatin", dosage = "20mg", type = "Pill", instructions = "Take 1 tablet after lunch")
            ),
            ScheduleWithMedication(
                schedule = MedicationSchedule(id = -103L, medicationId = 103L, scheduledTimeMillis = time8pm, status = ScheduleStatus.PENDING),
                medication = Medication(id = 103L, name = "Metformin", dosage = "850mg", type = "Pill", instructions = "Take 1 tablet with evening meal")
            ),
            ScheduleWithMedication(
                schedule = MedicationSchedule(id = -104L, medicationId = 104L, scheduledTimeMillis = time9pm, status = ScheduleStatus.PENDING),
                medication = Medication(id = 104L, name = "Lispro Insulin", dosage = "10 Units", type = "Injection", instructions = "Administer subcutaneously before dinner")
            )
        )
    }

    private fun getDemoMissedSchedules(): List<ScheduleWithMedication> {
        val time8am = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val yesterday9pm = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 21); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val yesterday7am = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 7); set(Calendar.MINUTE, 30); set(Calendar.SECOND, 0) }.timeInMillis

        return listOf(
            ScheduleWithMedication(
                schedule = MedicationSchedule(id = -201L, medicationId = 201L, scheduledTimeMillis = time8am, status = ScheduleStatus.MISSED),
                medication = Medication(id = 201L, name = "Lisinopril", dosage = "10mg", type = "Pill", instructions = "Take 1 pill in morning for blood pressure")
            ),
            ScheduleWithMedication(
                schedule = MedicationSchedule(id = -202L, medicationId = 202L, scheduledTimeMillis = yesterday9pm, status = ScheduleStatus.MISSED),
                medication = Medication(id = 202L, name = "Vitamin B12", dosage = "1000mcg", type = "Tablet", instructions = "Take 1 tablet with meal")
            ),
            ScheduleWithMedication(
                schedule = MedicationSchedule(id = -203L, medicationId = 203L, scheduledTimeMillis = yesterday7am, status = ScheduleStatus.MISSED),
                medication = Medication(id = 203L, name = "Omeprazole", dosage = "20mg", type = "Capsule", instructions = "Take before breakfast")
            )
        )
    }

    private fun getDemoCurrentVital(): VitalReading {
        return VitalReading(
            timestamp = System.currentTimeMillis(),
            heartRate = 74,
            spo2 = 98,
            bodyTemp = 36.6f,
            steps = 5420,
            isAbnormal = false
        )
    }

    private fun getDemoRecentVitals(): List<VitalReading> {
        val now = System.currentTimeMillis()
        val list = mutableListOf<VitalReading>()
        for (i in 0 until 10) {
            list.add(
                VitalReading(
                    id = -100L - i,
                    timestamp = now - (i * 300000L),
                    heartRate = 70 + (i % 5),
                    spo2 = 97 + (i % 3),
                    bodyTemp = 36.5f + (i % 2) * 0.1f,
                    steps = 5420 - (i * 120),
                    isAbnormal = false
                )
            )
        }
        return list
    }
}
