package com.example.medmitra.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medmitra.R
import com.example.medmitra.data.local.ScheduleStatus
import com.example.medmitra.data.local.ScheduleWithMedication
import com.example.medmitra.data.local.UserPreferencesManager
import com.example.medmitra.ui.components.GlassBackground
import com.example.medmitra.ui.components.GlassButton
import com.example.medmitra.ui.components.GlassCard
import com.example.medmitra.ui.components.GlassDialogSurface
import com.example.medmitra.ui.components.GlassIconButton
import com.example.medmitra.ui.components.GlassTextField
import com.example.medmitra.ui.components.GlassTopAppBar
import com.example.medmitra.ui.components.SmartwatchVitalsCard
import com.example.medmitra.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onTriggerSOS: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferencesManager.getInstance(context) }
    
    val isSeniorModeEnabled by userPrefs.seniorModeFlow.collectAsState()
    val appLanguage by userPrefs.appLanguageFlow.collectAsState()
    val isTamil = appLanguage == "ta"

    var showAddDialog by remember { mutableStateOf(false) }

    val todaySchedules by viewModel.todaySchedules.collectAsState()
    val missedSchedules by viewModel.missedSchedules.collectAsState()

    val currentVitalReading by viewModel.currentVitalReading.collectAsState()
    val isWatchConnected by viewModel.isWatchConnected.collectAsState()

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "MedMitra Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(if (isSeniorModeEnabled) 40.dp else 32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isTamil) "மேட்மித்ரா" else "MedMitra",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = if (isSeniorModeEnabled) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        GlassButton(
                            onClick = onTriggerSOS,
                            backgroundColor = Color(0xFFEF4444).copy(alpha = 0.35f),
                            borderColor = Color(0xFFFCA5A5),
                            contentColor = Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "SOS",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(if (isSeniorModeEnabled) 24.dp else 18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isTamil) "அவசரம் (SOS)" else "SOS",
                                fontWeight = FontWeight.Bold,
                                style = if (isSeniorModeEnabled) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                )
            },
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF06B6D4),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(if (isSeniorModeEnabled) 24.dp else 16.dp),
                    modifier = Modifier.padding(bottom = 16.dp, end = 8.dp).size(if (isSeniorModeEnabled) 72.dp else 56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Medication",
                        modifier = Modifier.size(if (isSeniorModeEnabled) 36.dp else 24.dp)
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(if (isSeniorModeEnabled) 20.dp else 16.dp)
            ) {
                // 🚨 PROMINENT ONE-TAP SOS BUTTON (HOME SCREEN)
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFEF4444).copy(alpha = 0.32f),
                        borderColor = Color(0xFFFCA5A5).copy(alpha = 0.8f),
                        contentPadding = PaddingValues(if (isSeniorModeEnabled) 18.dp else 14.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(if (isSeniorModeEnabled) 32.dp else 24.dp)
                                )
                                Text(
                                    text = if (isTamil) "ஒரு தட்டு அவசர உதவி (One-Tap SOS)" else "ONE-TAP EMERGENCY SOS",
                                    style = (if (isSeniorModeEnabled) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium).copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }

                            Text(
                                text = if (isTamil) "உடனடி அவசர அழைப்பு மற்றும் குடும்பத்தினருக்கு குறுஞ்செய்தி அனுப்பவும்" else "Instantly places direct phone call & dispatches emergency SMS to family contacts.",
                                style = if (isSeniorModeEnabled) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )

                            GlassButton(
                                onClick = onTriggerSOS,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isSeniorModeEnabled) 68.dp else 52.dp),
                                backgroundColor = Color(0xFFDC2626),
                                borderColor = Color(0xFFFECACA),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isSeniorModeEnabled) 30.dp else 22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isTamil) "🚨 அவசர உதவி உடனடியாக அழைக்கவும்" else "🚨 PRESS FOR IMMEDIATE SOS CALL & SMS",
                                        style = (if (isSeniorModeEnabled) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge).copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Senior Mode Active Status Banner if enabled
                if (isSeniorModeEnabled) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isTamil) "👁️ பெரிய எழுத்துக்கள் முறை (Senior Mode Active)" else "👁️ Senior Mode Active • Large Buttons & Text",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFDDD6FE)
                                )
                            }
                        }
                    }
                }

                // Smartwatch Vitals Live Dashboard Card
                item {
                    SmartwatchVitalsCard(
                        currentReading = currentVitalReading,
                        isConnected = isWatchConnected,
                        onToggleConnection = { viewModel.toggleWatchConnection(it) },
                        onSimulateHighHR = { viewModel.simulateHighHR() },
                        onSimulateLowSpO2 = { viewModel.simulateLowSpO2() },
                        onSimulateNormal = { viewModel.simulateNormalVitals() }
                    )
                }

                // Missed Medications Header
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(if (isSeniorModeEnabled) 28.dp else 22.dp)
                        )
                        Text(
                            text = if (isTamil) "தவறிய மருந்துகள்" else "Missed Medications",
                            style = (if (isSeniorModeEnabled) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge).copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFF87171)
                        )
                    }
                }

                if (missedSchedules.isEmpty()) {
                    item {
                        GlassCard(
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            borderColor = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isTamil) "தவறிய மருந்துகள் ஏதுமில்லை! நன்று." else "No missed medications. Great job!",
                                style = if (isSeniorModeEnabled) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                } else {
                    items(
                        items = missedSchedules,
                        key = { "missed_${it.schedule.id}_${it.medication.id}" }
                    ) { scheduleWithMed ->
                        ScheduleItemCard(
                            scheduleWithMed = scheduleWithMed,
                            onTaken = { viewModel.markScheduleAsTaken(scheduleWithMed.schedule.id) },
                            isMissed = true,
                            isSeniorMode = isSeniorModeEnabled,
                            isTamil = isTamil
                        )
                    }
                }

                // Today's Schedule Header
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(if (isSeniorModeEnabled) 28.dp else 22.dp)
                        )
                        Text(
                            text = if (isTamil) "இன்றைய மருந்துகள் அட்டவணை" else "Today's Schedule",
                            style = (if (isSeniorModeEnabled) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge).copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF22D3EE)
                        )
                    }
                }

                if (todaySchedules.isEmpty()) {
                    item {
                        GlassCard(
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            borderColor = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isTamil) "இன்று சாப்பிட வேண்டிய மருந்துகள் ஏதுமில்லை." else "No medications scheduled for today.",
                                style = if (isSeniorModeEnabled) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                } else {
                    items(
                        items = todaySchedules,
                        key = { "today_${it.schedule.id}_${it.medication.id}" }
                    ) { scheduleWithMed ->
                        ScheduleItemCard(
                            scheduleWithMed = scheduleWithMed,
                            onTaken = { viewModel.markScheduleAsTaken(scheduleWithMed.schedule.id) },
                            isMissed = false,
                            isSeniorMode = isSeniorModeEnabled,
                            isTamil = isTamil
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddMedicationDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, dosage, type, instructions, hour, minute ->
                    viewModel.addMedicationAndSchedule(name, dosage, type, instructions, hour, minute)
                    showAddDialog = false
                },
                isTamil = isTamil,
                isSeniorMode = isSeniorModeEnabled
            )
        }
    }
}

@Composable
fun ScheduleItemCard(
    scheduleWithMed: ScheduleWithMedication,
    onTaken: () -> Unit,
    isMissed: Boolean,
    isSeniorMode: Boolean = false,
    isTamil: Boolean = false
) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = remember(scheduleWithMed.schedule.scheduledTimeMillis) {
        formatter.format(Date(scheduleWithMed.schedule.scheduledTimeMillis))
    }

    val bgColor = if (isMissed) Color(0xFFEF4444).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.12f)
    val borderColor = if (isMissed) Color(0xFFFCA5A5).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.35f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = bgColor,
        borderColor = borderColor,
        contentPadding = PaddingValues(if (isSeniorMode) 18.dp else 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${scheduleWithMed.medication.name} • $timeString",
                    style = (if (isSeniorMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium).copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${if (isTamil) "அளவு" else "Dosage"}: ${scheduleWithMed.medication.dosage}  |  ${if (isTamil) "வகை" else "Type"}: ${scheduleWithMed.medication.type}",
                    style = if (isSeniorMode) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                if (scheduleWithMed.medication.instructions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (isTamil) "வழிமுறைகள்" else "Instructions"}: ${scheduleWithMed.medication.instructions}",
                        style = if (isSeniorMode) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (scheduleWithMed.schedule.status == ScheduleStatus.PENDING || scheduleWithMed.schedule.status == ScheduleStatus.MISSED) {
                GlassIconButton(
                    onClick = onTaken,
                    modifier = Modifier.size(if (isSeniorMode) 58.dp else 44.dp),
                    backgroundColor = if (isMissed) Color(0xFFEF4444).copy(alpha = 0.35f) else Color(0xFF06B6D4).copy(alpha = 0.35f),
                    borderColor = if (isMissed) Color(0xFFFCA5A5) else Color(0xFF22D3EE)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark as Taken",
                        tint = Color.White,
                        modifier = Modifier.size(if (isSeniorMode) 32.dp else 22.dp)
                    )
                }
            } else if (scheduleWithMed.schedule.status == ScheduleStatus.TAKEN) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Taken",
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(if (isSeniorMode) 38.dp else 28.dp)
                )
            }
        }
    }
}

@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, Int, Int) -> Unit,
    isTamil: Boolean = false,
    isSeniorMode: Boolean = false
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(0) }

    val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

    val timePickerDialog = remember(context, hour, minute) {
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                hour = selectedHour
                minute = selectedMinute
            },
            hour,
            minute,
            false
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassDialogSurface {
            Text(
                text = if (isTamil) "மருந்து சேர்க்கவும்" else "Add Medication",
                style = (if (isSeniorMode) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge).copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            GlassTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (isTamil) "மருந்தின் பெயர்" else "Medication Name") },
                modifier = Modifier.fillMaxWidth()
            )

            GlassTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text(if (isTamil) "அளவு (e.g. 1 மாத்திரை)" else "Dosage (e.g. 1 pill)") },
                modifier = Modifier.fillMaxWidth()
            )

            GlassTextField(
                value = type,
                onValueChange = { type = it },
                label = { Text(if (isTamil) "வகை (e.g. மாத்திரை, சிரப்)" else "Type (e.g. Pill, Syrup)") },
                modifier = Modifier.fillMaxWidth()
            )

            GlassTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text(if (isTamil) "வழிமுறைகள் (e.g. உணவுக்கு பின்)" else "Instructions (e.g. After meal)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${if (isTamil) "நேரம்" else "Time"}: $timeString",
                    style = if (isSeniorMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                GlassButton(onClick = { timePickerDialog.show() }) {
                    Text(if (isTamil) "நேரம் தேர்வு செய்க" else "Select Time")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(if (isTamil) "ரத்து" else "Cancel", color = Color.White.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlassButton(
                    onClick = {
                        if (name.isNotBlank() && dosage.isNotBlank()) {
                            onAdd(name, dosage, type, instructions, hour, minute)
                        }
                    }
                ) {
                    Text(if (isTamil) "சேர்" else "Add")
                }
            }
        }
    }
}
