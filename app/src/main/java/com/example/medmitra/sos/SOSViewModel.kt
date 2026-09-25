package com.example.medmitra.sos

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medmitra.data.local.UserPreferencesManager
import com.example.medmitra.vitals.VitalsManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SOSSource {
    FALL_DETECTION,
    MANUAL_TRIGGER,
    ABNORMAL_VITALS
}

sealed class SOSState {
    object Idle : SOSState()
    data class Countdown(val secondsRemaining: Int, val source: SOSSource) : SOSState()
    data class Dispatched(val contact: String, val smsSent: Boolean, val callInitiated: Boolean) : SOSState()
}

class SOSViewModel(application: Application) : AndroidViewModel(application) {

    private val userPrefs = UserPreferencesManager.getInstance(application)
    private val fallDetector = FallDetector(application)
    private val vitalsManager = VitalsManager.getInstance(application)
    private val sirenManager = EmergencySirenManager(application)

    private val _sosState = MutableStateFlow<SOSState>(SOSState.Idle)
    val sosState: StateFlow<SOSState> = _sosState.asStateFlow()

    private val _isFallDetectionEnabled = MutableStateFlow(userPrefs.isFallDetectionEnabled)
    val isFallDetectionEnabled: StateFlow<Boolean> = _isFallDetectionEnabled.asStateFlow()

    private val _fallSensitivity = MutableStateFlow(userPrefs.fallSensitivity)
    val fallSensitivity: StateFlow<String> = _fallSensitivity.asStateFlow()

    private val _isSosSirenEnabled = MutableStateFlow(userPrefs.isSosSirenEnabled)
    val isSosSirenEnabled: StateFlow<Boolean> = _isSosSirenEnabled.asStateFlow()

    private val _isDemoModeEnabled = MutableStateFlow(userPrefs.isDemoModeEnabled)
    val isDemoModeEnabled: StateFlow<Boolean> = _isDemoModeEnabled.asStateFlow()

    private val _emergencyContact = MutableStateFlow(userPrefs.emergencyContact)
    val emergencyContact: StateFlow<String> = _emergencyContact.asStateFlow()

    private var countdownJob: Job? = null

    companion object {
        private const val TAG = "SOSViewModel"
        private const val COUNTDOWN_SECONDS = 20
    }

    init {
        fallDetector.sensitivity = userPrefs.fallSensitivity
        fallDetector.onFallDetected = {
            if (_isFallDetectionEnabled.value) {
                triggerSOS(SOSSource.FALL_DETECTION)
            }
        }
        if (_isFallDetectionEnabled.value) {
            fallDetector.startListening()
        }

        viewModelScope.launch {
            vitalsManager.abnormalVitalEvent.collect { reading ->
                Log.w(TAG, "Abnormal vital reading detected from smartwatch: HR ${reading.heartRate}, SpO2 ${reading.spo2}")
                triggerSOS(SOSSource.ABNORMAL_VITALS)
            }
        }

        viewModelScope.launch {
            userPrefs.demoModeFlow.collect { demo ->
                _isDemoModeEnabled.value = demo
            }
        }

        viewModelScope.launch {
            userPrefs.fallSensitivityFlow.collect { sensitivity ->
                _fallSensitivity.value = sensitivity
                fallDetector.sensitivity = sensitivity
            }
        }

        viewModelScope.launch {
            userPrefs.isSosSirenEnabledFlow.collect { enabled ->
                _isSosSirenEnabled.value = enabled
            }
        }
    }

    fun setFallDetectionEnabled(enabled: Boolean) {
        userPrefs.isFallDetectionEnabled = enabled
        _isFallDetectionEnabled.value = enabled
        if (enabled) {
            fallDetector.startListening()
        } else {
            fallDetector.stopListening()
        }
    }

    fun setFallSensitivity(sensitivity: String) {
        userPrefs.fallSensitivity = sensitivity
        _fallSensitivity.value = sensitivity
        fallDetector.sensitivity = sensitivity
    }

    fun setSosSirenEnabled(enabled: Boolean) {
        userPrefs.isSosSirenEnabled = enabled
        _isSosSirenEnabled.value = enabled
    }

    fun setDemoModeEnabled(enabled: Boolean) {
        userPrefs.isDemoModeEnabled = enabled
        _isDemoModeEnabled.value = enabled
    }

    fun setEmergencyContact(contact: String) {
        userPrefs.emergencyContact = contact
        _emergencyContact.value = contact
    }

    fun triggerSOS(source: SOSSource = SOSSource.MANUAL_TRIGGER) {
        if (_sosState.value !is SOSState.Idle) return

        Log.w(TAG, "Triggering SOS via $source")
        
        // Start playing continuous loud high-frequency siren sound if enabled
        if (_isSosSirenEnabled.value) {
            sirenManager.startSiren()
        }

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in COUNTDOWN_SECONDS downTo 0) {
                _sosState.value = SOSState.Countdown(i, source)
                vibratePulse()
                if (i == 0) {
                    dispatchEmergency(getApplication())
                    break
                }
                delay(1000L)
            }
        }
    }

    fun cancelSOS() {
        Log.i(TAG, "SOS cancelled by user (I'M SAFE)")
        sirenManager.stopSiren()
        countdownJob?.cancel()
        countdownJob = null
        _sosState.value = SOSState.Idle
    }

    fun triggerEmergencyImmediately(context: Context) {
        sirenManager.stopSiren()
        countdownJob?.cancel()
        countdownJob = null
        dispatchEmergency(context)
    }

    fun dismissDispatched() {
        Log.i(TAG, "SOS dispatched dialog dismissed")
        sirenManager.stopSiren()
        _sosState.value = SOSState.Idle
    }

    private fun dispatchEmergency(context: Context) {
        val hasLocationPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPerm) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                // First try fast lastLocation for instant GPS link
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        val locLink = "https://maps.google.com/?q=${lastLoc.latitude},${lastLoc.longitude}"
                        sendSmsAndCall(context, locLink)
                    } else {
                        // Request fresh high-accuracy location
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { loc ->
                                val locLink = if (loc != null) "https://maps.google.com/?q=${loc.latitude},${loc.longitude}" else null
                                sendSmsAndCall(context, locLink)
                            }
                            .addOnFailureListener {
                                sendSmsAndCall(context, null)
                            }
                    }
                }.addOnFailureListener {
                    sendSmsAndCall(context, null)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error retrieving location for emergency dispatch", e)
                sendSmsAndCall(context, null)
            }
        } else {
            sendSmsAndCall(context, null)
        }
    }

    private fun sendSmsAndCall(context: Context, locationUrl: String?) {
        val familyContacts = userPrefs.getFamilyContactsList()
            .map { it.phone.trim() }
            .filter { it.isNotBlank() && it != "911" }

        val configuredPrimary = userPrefs.emergencyContact.trim()
        val primaryContact = if (configuredPrimary.isNotBlank() && configuredPrimary != "911") {
            configuredPrimary
        } else {
            familyContacts.firstOrNull() ?: ""
        }

        val allContacts = (listOf(primaryContact) + familyContacts)
            .filter { it.isNotBlank() && it != "911" }
            .distinct()

        Log.w(TAG, "Dispatching emergency to primary: '$primaryContact', all contacts: $allContacts, locationUrl: $locationUrl")

        var smsSentCount = 0
        var callInitiated = false

        val userName = userPrefs.userName.ifBlank { "MedMitra user" }
        val message = if (!locationUrl.isNullOrBlank()) {
            "EMERGENCY ALERT: $userName needs immediate help! Live GPS Location: $locationUrl"
        } else {
            "EMERGENCY ALERT: $userName needs immediate help! (SOS triggered via MedMitra App)"
        }

        // 1. Send Emergency SMS with Live GPS Location Link to ALL emergency & family contacts
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            val smsManager: SmsManager? = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val subId = SmsManager.getDefaultSmsSubscriptionId()
                    if (subId != SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
                        context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
                    } else {
                        context.getSystemService(SmsManager::class.java)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            } catch (e: Throwable) {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            for (targetNumber in allContacts) {
                if (targetNumber.isBlank()) continue
                try {
                    val parts = smsManager?.divideMessage(message)
                    if (parts != null && parts.size > 1) {
                        smsManager.sendMultipartTextMessage(targetNumber, null, parts, null, null)
                    } else {
                        smsManager?.sendTextMessage(targetNumber, null, message, null, null)
                    }
                    smsSentCount++
                    Log.i(TAG, "Emergency SMS with location sent to $targetNumber: $message")
                } catch (e: Throwable) {
                    Log.e(TAG, "Direct SMS failed for $targetNumber, using intent fallback", e)
                    try {
                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$targetNumber")).apply {
                            putExtra("sms_body", message)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(smsIntent)
                        smsSentCount++
                    } catch (ex: Throwable) {
                        Log.e(TAG, "SMS intent fallback failed for $targetNumber", ex)
                    }
                }
            }
        } else {
            Log.w(TAG, "SEND_SMS permission not granted. Launching SMS intent fallback with prefilled location.")
            for (targetNumber in allContacts) {
                if (targetNumber.isBlank()) continue
                try {
                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$targetNumber")).apply {
                        putExtra("sms_body", message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(smsIntent)
                    smsSentCount++
                } catch (ex: Throwable) {
                    Log.e(TAG, "Fallback SMS intent failed for $targetNumber", ex)
                }
            }
        }

        // 2. Initiate DIRECT Emergency Phone Call to configured primary emergency contact
        if (primaryContact.isNotBlank()) {
            try {
                val hasCallPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED

                val action = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
                val callIntent = Intent(action, Uri.parse("tel:$primaryContact")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                callInitiated = true
                Log.i(TAG, "Launched direct emergency call ($action) for $primaryContact")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException on ACTION_CALL, falling back to ACTION_DIAL", e)
                try {
                    val fallbackIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$primaryContact")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                    callInitiated = true
                } catch (ex: Throwable) {
                    Log.e(TAG, "Fallback dial intent failed", ex)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initiate call intent", e)
            }
        } else {
            Log.w(TAG, "No emergency contact number configured. Skipping call dispatch.")
        }

        _sosState.value = SOSState.Dispatched(
            contact = primaryContact.ifBlank { "Emergency Contacts" },
            smsSent = smsSentCount > 0,
            callInitiated = callInitiated
        )
    }

    private fun vibratePulse() {
        try {
            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.vibrate(200L)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sirenManager.stopSiren()
        fallDetector.stopListening()
    }
}