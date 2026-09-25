package com.example.medmitra.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class FamilyContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val relationship: String = "Emergency Contact",
    val isPrimary: Boolean = false
)

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("medmitra_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_CONTACT = "user_contact"
        private const val KEY_EMERGENCY_CONTACT_NAME = "emergency_contact_name"
        private const val KEY_EMERGENCY_CONTACT = "emergency_contact"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_FAMILY_CONTACTS = "family_contacts_json"
        private const val KEY_FALL_DETECTION_ENABLED = "fall_detection_enabled"
        private const val KEY_DEMO_MODE_ENABLED = "demo_mode_enabled"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_FALL_SENSITIVITY = "fall_sensitivity"
        private const val KEY_SOS_SIREN_ENABLED = "sos_siren_enabled"
        private const val KEY_SENIOR_MODE_ENABLED = "senior_mode_enabled"
        private const val DEFAULT_EMERGENCY_CONTACT = ""

        @Volatile
        private var INSTANCE: UserPreferencesManager? = null

        fun getInstance(context: Context): UserPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val _isLoggedInFlow = MutableStateFlow(isLoggedIn)
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    private val _demoModeFlow = MutableStateFlow(isDemoModeEnabled)
    val demoModeFlow: StateFlow<Boolean> = _demoModeFlow.asStateFlow()

    private val _familyContactsFlow = MutableStateFlow(getFamilyContactsList())
    val familyContactsFlow: StateFlow<List<FamilyContact>> = _familyContactsFlow.asStateFlow()

    private val _appLanguageFlow = MutableStateFlow(appLanguage)
    val appLanguageFlow: StateFlow<String> = _appLanguageFlow.asStateFlow()

    private val _fallSensitivityFlow = MutableStateFlow(fallSensitivity)
    val fallSensitivityFlow: StateFlow<String> = _fallSensitivityFlow.asStateFlow()

    private val _isSosSirenEnabledFlow = MutableStateFlow(isSosSirenEnabled)
    val isSosSirenEnabledFlow: StateFlow<Boolean> = _isSosSirenEnabledFlow.asStateFlow()

    private val _seniorModeFlow = MutableStateFlow(isSeniorModeEnabled)
    val seniorModeFlow: StateFlow<Boolean> = _seniorModeFlow.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_DEMO_MODE_ENABLED -> _demoModeFlow.value = isDemoModeEnabled
            KEY_IS_LOGGED_IN -> _isLoggedInFlow.value = isLoggedIn
            KEY_FAMILY_CONTACTS -> _familyContactsFlow.value = getFamilyContactsList()
            KEY_APP_LANGUAGE -> _appLanguageFlow.value = appLanguage
            KEY_FALL_SENSITIVITY -> _fallSensitivityFlow.value = fallSensitivity
            KEY_SOS_SIREN_ENABLED -> _isSosSirenEnabledFlow.value = isSosSirenEnabled
            KEY_SENIOR_MODE_ENABLED -> _seniorModeFlow.value = isSeniorModeEnabled
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userContact: String
        get() = prefs.getString(KEY_USER_CONTACT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_CONTACT, value).apply()

    var emergencyContactName: String
        get() = prefs.getString(KEY_EMERGENCY_CONTACT_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMERGENCY_CONTACT_NAME, value).apply()

    var emergencyContact: String
        get() {
            val saved = prefs.getString(KEY_EMERGENCY_CONTACT, "") ?: ""
            if (saved.isNotBlank() && saved != "911") {
                return saved
            }
            val familyList = getFamilyContactsList()
            val primary = familyList.firstOrNull { it.isPrimary && it.phone.isNotBlank() && it.phone != "911" }
                ?: familyList.firstOrNull { it.phone.isNotBlank() && it.phone != "911" }
            return primary?.phone ?: ""
        }
        set(value) = prefs.edit().putString(KEY_EMERGENCY_CONTACT, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()
            _isLoggedInFlow.value = value
        }

    var isFallDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_FALL_DETECTION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FALL_DETECTION_ENABLED, value).apply()

    var isDemoModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEMO_MODE_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DEMO_MODE_ENABLED, value).apply()
            _demoModeFlow.value = value
        }

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en"
        set(value) {
            prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()
            _appLanguageFlow.value = value
        }

    var fallSensitivity: String
        get() = prefs.getString(KEY_FALL_SENSITIVITY, "Medium") ?: "Medium"
        set(value) {
            prefs.edit().putString(KEY_FALL_SENSITIVITY, value).apply()
            _fallSensitivityFlow.value = value
        }

    var isSosSirenEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOS_SIREN_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SOS_SIREN_ENABLED, value).apply()
            _isSosSirenEnabledFlow.value = value
        }

    var isSeniorModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SENIOR_MODE_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SENIOR_MODE_ENABLED, value).apply()
            _seniorModeFlow.value = value
        }

    fun saveUserDetails(
        name: String,
        contact: String,
        emergencyName: String,
        emergencyPhone: String
    ) {
        val primaryPhone = emergencyPhone.ifBlank { DEFAULT_EMERGENCY_CONTACT }
        val primaryName = emergencyName.ifBlank { "Primary Emergency Contact" }

        prefs.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_CONTACT, contact)
            .putString(KEY_EMERGENCY_CONTACT_NAME, primaryName)
            .putString(KEY_EMERGENCY_CONTACT, primaryPhone)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()

        // Ensure primary contact is added to family contacts
        val currentList = getFamilyContactsList().toMutableList()
        val existingPrimaryIndex = currentList.indexOfFirst { it.isPrimary }
        if (existingPrimaryIndex >= 0) {
            currentList[existingPrimaryIndex] = currentList[existingPrimaryIndex].copy(
                name = primaryName,
                phone = primaryPhone
            )
        } else {
            currentList.add(
                0,
                FamilyContact(
                    name = primaryName,
                    phone = primaryPhone,
                    relationship = "Primary Emergency",
                    isPrimary = true
                )
            )
        }
        saveFamilyContactsList(currentList)

        _isLoggedInFlow.value = true
    }

    fun getFamilyContactsList(): List<FamilyContact> {
        val jsonStr = prefs.getString(KEY_FAMILY_CONTACTS, null)
        if (jsonStr.isNullOrBlank()) {
            val defaultPrimaryPhone = emergencyContact
            val defaultPrimaryName = emergencyContactName.ifBlank { "Primary Emergency Contact" }
            return listOf(
                FamilyContact(
                    name = defaultPrimaryName,
                    phone = defaultPrimaryPhone,
                    relationship = "Primary Emergency",
                    isPrimary = true
                )
            )
        }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<FamilyContact>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FamilyContact(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        phone = obj.optString("phone", ""),
                        relationship = obj.optString("relationship", "Emergency Contact"),
                        isPrimary = obj.optBoolean("isPrimary", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addFamilyContact(name: String, phone: String, relationship: String = "Family", isPrimary: Boolean = false) {
        val currentList = getFamilyContactsList().toMutableList()
        val updatedList = if (isPrimary) {
            val listWithUnsetPrimary = currentList.map { it.copy(isPrimary = false) }.toMutableList()
            listWithUnsetPrimary.add(
                FamilyContact(
                    name = name,
                    phone = phone,
                    relationship = relationship,
                    isPrimary = true
                )
            )
            emergencyContact = phone
            emergencyContactName = name
            listWithUnsetPrimary
        } else {
            currentList.add(
                FamilyContact(
                    name = name,
                    phone = phone,
                    relationship = relationship,
                    isPrimary = false
                )
            )
            currentList
        }
        saveFamilyContactsList(updatedList)
    }

    fun removeFamilyContact(id: String) {
        val currentList = getFamilyContactsList().toMutableList()
        currentList.removeAll { it.id == id }
        saveFamilyContactsList(currentList)
    }

    fun setPrimaryFamilyContact(id: String) {
        val currentList = getFamilyContactsList().map { contact ->
            if (contact.id == id) {
                emergencyContact = contact.phone
                emergencyContactName = contact.name
                contact.copy(isPrimary = true)
            } else {
                contact.copy(isPrimary = false)
            }
        }
        saveFamilyContactsList(currentList)
    }

    private fun saveFamilyContactsList(list: List<FamilyContact>) {
        val array = JSONArray()
        for (contact in list) {
            val obj = JSONObject().apply {
                put("id", contact.id)
                put("name", contact.name)
                put("phone", contact.phone)
                put("relationship", contact.relationship)
                put("isPrimary", contact.isPrimary)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_FAMILY_CONTACTS, array.toString()).apply()
        _familyContactsFlow.value = list
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
        _isLoggedInFlow.value = false
    }
}
