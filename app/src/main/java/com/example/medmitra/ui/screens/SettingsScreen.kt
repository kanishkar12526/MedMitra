package com.example.medmitra.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.medmitra.data.local.FamilyContact
import com.example.medmitra.data.local.UserPreferencesManager
import com.example.medmitra.service.MedMitraForegroundService
import com.example.medmitra.sos.SOSViewModel
import com.example.medmitra.tts.TTSManager
import com.example.medmitra.ui.components.GlassBackground
import com.example.medmitra.ui.components.GlassButton
import com.example.medmitra.ui.components.GlassCard
import com.example.medmitra.ui.components.GlassDialogSurface
import com.example.medmitra.ui.components.GlassIconButton
import com.example.medmitra.ui.components.GlassTextField
import com.example.medmitra.ui.components.GlassTopAppBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Locale

data class LanguageOption(
    val code: String,
    val nameEnglish: String,
    val nameNative: String
)

val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("en", "English", "English"),
    LanguageOption("hi", "Hindi", "हिंदी"),
    LanguageOption("ta", "Tamil", "தமிழ்"),
    LanguageOption("te", "Telugu", "తెలుగు"),
    LanguageOption("kn", "Kannada", "ಕನ್ನಡ"),
    LanguageOption("ml", "Malayalam", "മലയാളം"),
    LanguageOption("mr", "Marathi", "मराठी"),
    LanguageOption("gu", "Gujarati", "ગુજરાતી"),
    LanguageOption("bn", "Bengali", "বাংলা")
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    sosViewModel: SOSViewModel,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferencesManager.getInstance(context) }

    val isFallDetectionEnabled by sosViewModel.isFallDetectionEnabled.collectAsState()
    val fallSensitivity by sosViewModel.fallSensitivity.collectAsState()
    val isSosSirenEnabled by sosViewModel.isSosSirenEnabled.collectAsState()
    val isDemoModeEnabled by sosViewModel.isDemoModeEnabled.collectAsState()
    val emergencyContact by sosViewModel.emergencyContact.collectAsState()
    val familyContacts by userPrefs.familyContactsFlow.collectAsState()
    val currentAppLanguage by userPrefs.appLanguageFlow.collectAsState()

    var showAddContactDialog by remember { mutableStateOf(false) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val smsPermissionState = rememberPermissionState(Manifest.permission.SEND_SMS)
    val callPermissionState = rememberPermissionState(Manifest.permission.CALL_PHONE)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            snackbarMessage = null
        }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Settings & Preferences",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.9f),
                        contentColor = Color.White
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Senior Mode Toggle Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isSeniorModeEnabled by userPrefs.seniorModeFlow.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Senior Accessibility Mode 👁️",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = if (isSeniorModeEnabled) "Large Buttons & High-Contrast Text Active" else "Standard Compact UI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSeniorModeEnabled) Color(0xFF34D399) else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Switch(
                            checked = isSeniorModeEnabled,
                            onCheckedChange = {
                                userPrefs.isSeniorModeEnabled = it
                                snackbarMessage = if (it) "Senior Mode Enabled (Enlarged UI)" else "Senior Mode Disabled"
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF06B6D4),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Enlarges buttons, menu choices, text sizes, and touch targets across the entire app for elderly ease-of-use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // User Profile & Account Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = userPrefs.userName.ifBlank { "MedMitra User" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = userPrefs.userContact.ifBlank { "No contact details" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        GlassButton(
                            onClick = onLogout,
                            backgroundColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                            borderColor = Color(0xFFFCA5A5).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Logout", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }

                // Multilingual Voice & App Language Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentLangOption = SUPPORTED_LANGUAGES.find { it.code == currentAppLanguage }
                        ?: SUPPORTED_LANGUAGES.first()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Multilingual Language",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Voice Announcements & Locale",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Box {
                            GlassButton(
                                onClick = { languageMenuExpanded = true },
                                backgroundColor = Color(0xFF06B6D4).copy(alpha = 0.25f),
                                borderColor = Color(0xFF22D3EE),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${currentLangOption.nameEnglish} (${currentLangOption.nameNative})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = languageMenuExpanded,
                                onDismissRequest = { languageMenuExpanded = false }
                            ) {
                                SUPPORTED_LANGUAGES.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${lang.nameEnglish} - ${lang.nameNative}",
                                                fontWeight = if (lang.code == currentAppLanguage) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            languageMenuExpanded = false
                                            userPrefs.appLanguage = lang.code
                                            updateAppLocale(context, lang.code)
                                            snackbarMessage = "Language updated to ${lang.nameEnglish} (${lang.nameNative})"
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Medication voice reminders will announce: \"${getMultilingualAnnouncement(currentLangOption.code, "[Medicine]")}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GlassButton(
                        onClick = {
                            val text = getMultilingualAnnouncement(currentLangOption.code, "Paracetamol")
                            val tts = TTSManager(context, currentLangOption.code)
                            tts.speak(text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF06B6D4).copy(alpha = 0.2f),
                        borderColor = Color(0xFF22D3EE)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test Voice Announcement (${currentLangOption.nameEnglish})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Fall Detection & Sensitivity Slider Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Fall Detection",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = if (isFallDetectionEnabled) "Active • Accelerometer active" else "Disabled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isFallDetectionEnabled) Color(0xFF34D399) else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Switch(
                            checked = isFallDetectionEnabled,
                            onCheckedChange = { sosViewModel.setFallDetectionEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF06B6D4),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Monitors device accelerometer sensors for sudden impact spikes indicative of a fall. If detected, triggers a 20-second emergency countdown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    if (isFallDetectionEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))

                        val sliderValue = when (fallSensitivity.lowercase()) {
                            "low" -> 0f
                            "high" -> 2f
                            else -> 1f
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color(0xFF22D3EE),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Sensitivity Threshold:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            val impactText = when (fallSensitivity.lowercase()) {
                                "low" -> "Low (34 m/s²)"
                                "high" -> "High (19 m/s²)"
                                else -> "Medium (26 m/s²)"
                            }

                            Text(
                                text = impactText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF22D3EE)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = sliderValue,
                            onValueChange = { newVal ->
                                val newSens = when (newVal.toInt()) {
                                    0 -> "Low"
                                    2 -> "High"
                                    else -> "Medium"
                                }
                                sosViewModel.setFallSensitivity(newSens)
                            },
                            valueRange = 0f..2f,
                            steps = 1,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF22D3EE),
                                activeTrackColor = Color(0xFF06B6D4),
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                                activeTickColor = Color.White,
                                inactiveTickColor = Color.White.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Low (34 m/s²)\nHard Impact",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (fallSensitivity.equals("Low", ignoreCase = true)) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Medium (26 m/s²)\nStandard",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (fallSensitivity.equals("Medium", ignoreCase = true)) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "High (19 m/s²)\nSensitive",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (fallSensitivity.equals("High", ignoreCase = true)) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Emergency SOS Siren Sound Option Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isSosSirenEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = null,
                                tint = if (isSosSirenEnabled) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Enable Emergency Siren Sound",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = if (isSosSirenEnabled) "High-frequency continuous alarm active" else "Siren Muted during SOS alert",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSosSirenEnabled) Color(0xFF34D399) else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Switch(
                            checked = isSosSirenEnabled,
                            onCheckedChange = { sosViewModel.setSosSirenEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF06B6D4),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "When enabled, plays a loud 1500Hz-3000Hz wailing emergency siren on maximum volume whenever SOS or fall countdown is triggered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Family Emergency Contacts Section
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Family Emergency Contacts",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        GlassButton(
                            onClick = { showAddContactDialog = true },
                            backgroundColor = Color(0xFF06B6D4).copy(alpha = 0.25f),
                            borderColor = Color(0xFF22D3EE),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Contact",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (familyContacts.isEmpty()) {
                        Text(
                            text = "No family contacts added yet. Click 'Add' above to register emergency contacts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            familyContacts.forEach { contact ->
                                FamilyContactItemCard(
                                    contact = contact,
                                    isCurrentPrimary = (contact.phone == emergencyContact || contact.isPrimary),
                                    onMakePrimary = {
                                        userPrefs.setPrimaryFamilyContact(contact.id)
                                        sosViewModel.setEmergencyContact(contact.phone)
                                        snackbarMessage = "${contact.name} set as Primary Emergency Contact"
                                    },
                                    onDirectCall = {
                                        initiateDirectCall(context, contact.phone)
                                    },
                                    onDelete = {
                                        userPrefs.removeFamilyContact(contact.id)
                                        snackbarMessage = "Contact removed"
                                    }
                                )
                            }
                        }
                    }
                }

                // 24/7 Background Service & Battery Optimization Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
                    var isIgnoringBatteryOptimizations by remember {
                        mutableStateOf(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                powerManager.isIgnoringBatteryOptimizations(context.packageName)
                            } else true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatterySaver,
                                contentDescription = null,
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "24/7 Background Service & Battery",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = if (isIgnoringBatteryOptimizations) "Unrestricted • Battery Saver Exempted" else "Optimized • Reminders may be delayed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isIgnoringBatteryOptimizations) Color(0xFF34D399) else Color(0xFFFBBF24)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "MedMitra persistent foreground service ensures continuous fall detection and on-time medication voice reminders even when the device is locked or minimized.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassButton(
                            onClick = {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    }
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            backgroundColor = if (isIgnoringBatteryOptimizations) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFFF59E0B).copy(alpha = 0.35f),
                            borderColor = if (isIgnoringBatteryOptimizations) Color(0xFF34D399) else Color(0xFFFBBF24)
                        ) {
                            Text(
                                text = if (isIgnoringBatteryOptimizations) "Battery Saver Granted" else "Request Battery Exemption",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        GlassButton(
                            onClick = {
                                MedMitraForegroundService.startService(context)
                                snackbarMessage = "MedMitra Foreground Service Restarted"
                            },
                            backgroundColor = Color(0xFF06B6D4).copy(alpha = 0.25f),
                            borderColor = Color(0xFF22D3EE)
                        ) {
                            Text(
                                text = "Restart Service",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Demo Mode Glass Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Demo Mode",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = if (isDemoModeEnabled) "Active • Showing Demo Data" else "Inactive • Real DB & Sensors",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDemoModeEnabled) Color(0xFF34D399) else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Switch(
                            checked = isDemoModeEnabled,
                            onCheckedChange = { sosViewModel.setDemoModeEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF06B6D4),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "When enabled, displays realistic demo medication schedules, completed/missed history, and live simulated smartwatch vitals on the Dashboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Smartwatch Health Connect & Vitals Settings Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Smartwatch Vitals & Health Connect",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Real-time sync with Wear OS / Smartwatch health sensors. Automatically triggers Emergency SOS on critical thresholds:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Heart Rate: Normal 50 – 120 BPM (Alert if <50 or >120 BPM)\n" +
                                "• SpO2 Oxygen: Normal ≥ 90% (Alert if <90%)\n" +
                                "• Body Temperature & Step Tracking\n" +
                                "• Health Connect & Sensor fallback enabled",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF22D3EE)
                    )
                }

                // Permissions & SOS Testing Glass Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFEF4444).copy(alpha = 0.18f),
                    borderColor = Color(0xFFFCA5A5).copy(alpha = 0.45f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "SOS Testing & Permissions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFCA5A5)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!smsPermissionState.status.isGranted || !callPermissionState.status.isGranted) {
                        Text(
                            text = "Grant permissions to automatically send emergency SMS and initiate direct phone calls during an SOS trigger.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!smsPermissionState.status.isGranted) {
                                GlassButton(
                                    onClick = { smsPermissionState.launchPermissionRequest() },
                                    backgroundColor = Color.White.copy(alpha = 0.15f),
                                    borderColor = Color.White.copy(alpha = 0.4f)
                                ) {
                                    Text("Grant SMS Permission", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (!callPermissionState.status.isGranted) {
                                GlassButton(
                                    onClick = { callPermissionState.launchPermissionRequest() },
                                    backgroundColor = Color.White.copy(alpha = 0.15f),
                                    borderColor = Color.White.copy(alpha = 0.4f)
                                ) {
                                    Text("Grant Phone Permission", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    GlassButton(
                        onClick = { sosViewModel.triggerSOS() },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFEF4444).copy(alpha = 0.85f),
                        borderColor = Color(0xFFFCA5A5)
                    ) {
                        Text(
                            text = "TEST SOS ALERT (20s Countdown)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (showAddContactDialog) {
            AddFamilyContactDialog(
                onDismiss = { showAddContactDialog = false },
                onAdd = { name, phone, relationship, isPrimary ->
                    userPrefs.addFamilyContact(name, phone, relationship, isPrimary)
                    if (isPrimary) {
                        sosViewModel.setEmergencyContact(phone)
                    }
                    snackbarMessage = "$name added to Family Contacts"
                    showAddContactDialog = false
                }
            )
        }
    }
}

@Composable
private fun FamilyContactItemCard(
    contact: FamilyContact,
    isCurrentPrimary: Boolean,
    onMakePrimary: () -> Unit,
    onDirectCall: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isCurrentPrimary) Color(0xFF06B6D4).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
        borderColor = if (isCurrentPrimary) Color(0xFF22D3EE).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    if (isCurrentPrimary) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Primary SOS Contact",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "PRIMARY SOS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFBBF24)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${contact.phone} • ${contact.relationship}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Direct Call Button
                GlassIconButton(
                    onClick = onDirectCall,
                    backgroundColor = Color(0xFF10B981).copy(alpha = 0.3f),
                    borderColor = Color(0xFF34D399)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Direct Call",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!isCurrentPrimary) {
                    // Set as Primary Button
                    GlassIconButton(
                        onClick = onMakePrimary,
                        backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.25f),
                        borderColor = Color(0xFFFBBF24)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Set Primary",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete Button
                    GlassIconButton(
                        onClick = onDelete,
                        backgroundColor = Color(0xFFEF4444).copy(alpha = 0.25f),
                        borderColor = Color(0xFFFCA5A5)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Contact",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFamilyContactDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Family") }
    var isPrimary by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassDialogSurface {
            Text(
                text = "Add Family Contact",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            GlassTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Contact Name") },
                placeholder = { Text("e.g. Jane Doe") },
                modifier = Modifier.fillMaxWidth()
            )

            GlassTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                placeholder = { Text("e.g. +1234567890") },
                modifier = Modifier.fillMaxWidth()
            )

            GlassTextField(
                value = relationship,
                onValueChange = { relationship = it },
                label = { Text("Relationship") },
                placeholder = { Text("e.g. Spouse, Child, Parent, Doctor") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isPrimary,
                    onCheckedChange = { isPrimary = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF06B6D4),
                        uncheckedColor = Color.White.copy(alpha = 0.6f)
                    )
                )
                Text(
                    text = "Set as Primary Emergency Contact",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlassButton(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onAdd(name.trim(), phone.trim(), relationship.trim(), isPrimary)
                        }
                    }
                ) {
                    Text("Add Contact")
                }
            }
        }
    }
}

private fun updateAppLocale(context: Context, languageCode: String) {
    try {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun getMultilingualAnnouncement(lang: String, medicationName: String): String {
    val template = when (lang) {
        "hi" -> "स्वस्थ रहने के लिए अपनी दवा %s लें"
        "ta" -> "ஆரோக்கியமாக இருக்க உங்கள் மருந்து %s உட்கொள்ளுங்கள்"
        "te" -> "ఆరోగ్యంగా ఉండటానికి మీ మందు %s తీసుకోండి"
        "kn" -> "ಆರೋಗ್ಯವಾಗಿರಲು ನಿಮ್ಮ ಔಷಧ %s ತೆಗೆದುಕೊಳ್ಳಿ"
        "ml" -> "ആരോഗ്യത്തോടെയിരിക്കാൻ നിങ്ങളുടെ മരുന്ന് %s കഴിക്കുക"
        "mr" -> "निरोगी राहण्यासाठी तुमचे औषध %s घ्या"
        "gu" -> "તંદુરસ્ત રહેવા માટે તમારી દવા %s લો"
        "bn" -> "সুস্থ থাকতে আপনার ওষুধ %s নিন"
        else -> "Take your medicine %s to stay healthy"
    }
    return String.format(template, medicationName)
}

private fun initiateDirectCall(context: Context, phoneNumber: String) {
    if (phoneNumber.isBlank()) return
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED

    val action = if (hasPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
    try {
        val intent = Intent(action, Uri.parse("tel:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: SecurityException) {
        val fallbackIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallbackIntent)
    } catch (e: Exception) {
        // Ignored
    }
}
