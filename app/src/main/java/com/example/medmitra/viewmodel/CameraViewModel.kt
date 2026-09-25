package com.example.medmitra.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.medmitra.BuildConfig
import com.example.medmitra.MedMitraApplication
import com.example.medmitra.data.local.MedMitraDatabase
import com.example.medmitra.data.local.Medication
import com.example.medmitra.data.local.MedicationSchedule
import com.example.medmitra.data.local.ScheduleStatus
import com.example.medmitra.data.repository.MedicationRepository
import com.example.medmitra.util.OfflinePrescriptionParser
import com.example.medmitra.util.ParsedPrescriptionItem
import com.example.medmitra.worker.MedicationReminderWorker
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class ScanMode {
    MEDICINE_INFO,
    SCAN_PRESCRIPTION
}

sealed class CameraUiState {
    object Idle : CameraUiState()
    object Loading : CameraUiState()
    data class Success(val result: String) : CameraUiState()
    data class PrescriptionSuccess(
        val items: List<ParsedPrescriptionItem>
    ) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicationRepository
    private val workManager: WorkManager

    private val _scanMode = MutableStateFlow(ScanMode.MEDICINE_INFO)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Idle)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
    )

    init {
        val app = application as? MedMitraApplication
        repository = app?.repository ?: MedicationRepository(MedMitraDatabase.getDatabase(application).medicationDao())
        workManager = WorkManager.getInstance(application)
    }

    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
        resetState()
    }

    fun analyzeImage(bitmap: Bitmap) {
        _uiState.value = CameraUiState.Loading
        if (_scanMode.value == ScanMode.MEDICINE_INFO) {
            viewModelScope.launch {
                try {
                    analyzeMedicineInfo(bitmap)
                } catch (e: Exception) {
                    Log.e("CameraViewModel", "Analysis error", e)
                    _uiState.value = CameraUiState.Error(e.localizedMessage ?: "Unknown error occurred during analysis")
                }
            }
        } else {
            processPrescriptionOffline(bitmap)
        }
    }

    fun processPrescriptionOffline(bitmap: Bitmap) {
        _uiState.value = CameraUiState.Loading
        viewModelScope.launch {
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val image = InputImage.fromBitmap(bitmap, 0)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        viewModelScope.launch {
                            val rawText = visionText.text
                            Log.d("CameraViewModel", "Offline OCR extracted raw text:\n$rawText")

                            if (rawText.isBlank()) {
                                _uiState.value = CameraUiState.Error("No text detected in image. Please ensure the prescription is clearly lit and centered.")
                                return@launch
                            }

                            val items = OfflinePrescriptionParser.parse(rawText)
                            if (items.isEmpty()) {
                                _uiState.value = CameraUiState.Error("No prescription medications recognized. Please ensure the doctor's handwriting or printed prescription text is legible.")
                                return@launch
                            }

                            saveExtractedPrescriptions(items)
                            _uiState.value = CameraUiState.PrescriptionSuccess(items)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CameraViewModel", "ML Kit OCR failed", e)
                        _uiState.value = CameraUiState.Error("Offline text recognition failed: ${e.localizedMessage}")
                    }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error initiating offline OCR", e)
                _uiState.value = CameraUiState.Error(e.localizedMessage ?: "Unknown error during offline scanning")
            }
        }
    }

    private suspend fun analyzeMedicineInfo(bitmap: Bitmap) {
        val prompt = "Identify this medicine. Provide a brief about its use, side effects, and impact on the body."
        val response = generativeModel.generateContent(
            content {
                image(bitmap)
                text(prompt)
            }
        )

        response.text?.let {
            _uiState.value = CameraUiState.Success(it)
        } ?: run {
            _uiState.value = CameraUiState.Error("Failed to generate details. Result was empty.")
        }
    }

    private suspend fun saveExtractedPrescriptions(items: List<ParsedPrescriptionItem>) {
        for (item in items) {
            val medication = Medication(
                name = item.name,
                dosage = item.dosage,
                type = "Prescription",
                instructions = item.instructions
            )
            val medId = repository.addMedication(medication)

            for (timeStr in item.times) {
                val (hour, minute) = parseTimeStringToHourMinute(timeStr)
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (cal.timeInMillis <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                val schedule = MedicationSchedule(
                    medicationId = medId,
                    scheduledTimeMillis = cal.timeInMillis,
                    status = ScheduleStatus.PENDING
                )
                val scheduleId = repository.addSchedule(schedule)
                scheduleNotification(scheduleId, cal.timeInMillis)
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

    private fun parseTimeStringToHourMinute(timeStr: String): Pair<Int, Int> {
        val clean = timeStr.trim().uppercase()
        try {
            if (clean.contains("AM") || clean.contains("PM")) {
                val isPm = clean.contains("PM")
                val digitsOnly = clean.replace("AM", "").replace("PM", "").trim()
                val parts = digitsOnly.split(":")
                var hour = parts[0].trim().toInt()
                val minute = if (parts.size > 1) parts[1].trim().toInt() else 0
                if (isPm && hour < 12) hour += 12
                if (!isPm && hour == 12) hour = 0
                return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            } else if (clean.contains(":")) {
                val parts = clean.split(":")
                val hour = parts[0].trim().toInt()
                val minute = if (parts.size > 1) parts[1].trim().toInt() else 0
                return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            }
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Failed to parse time string '$timeStr'", e)
        }
        return Pair(8, 0)
    }

    fun resetState() {
        _uiState.value = CameraUiState.Idle
    }
}
