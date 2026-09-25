package com.example.medmitra.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medmitra.R
import com.example.medmitra.data.local.UserPreferencesManager
import com.example.medmitra.ui.components.GlassBackground
import com.example.medmitra.ui.components.GlassButton
import com.example.medmitra.ui.components.GlassCard
import com.example.medmitra.ui.components.GlassTextField

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferencesManager.getInstance(context) }

    var userName by remember { mutableStateOf(userPrefs.userName) }
    var userContact by remember { mutableStateOf(userPrefs.userContact) }
    var emergencyName by remember { mutableStateOf(userPrefs.emergencyContactName) }
    var emergencyPhone by remember {
        mutableStateOf(if (userPrefs.emergencyContact == "911") "" else userPrefs.emergencyContact)
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding & Header
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "MedMitra Logo",
                modifier = Modifier.size(76.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "MedMitra",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Text(
                text = "AI Health Companion & Background SOS Shield",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF22D3EE)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Glassmorphic Setup Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.White.copy(alpha = 0.12f),
                borderColor = Color.White.copy(alpha = 0.35f),
                contentPadding = PaddingValues(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "User Profile & SOS Setup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. USER PROFILE SECTION
                Text(
                    text = "1. User Profile",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF22D3EE)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Full Name
                GlassTextField(
                    value = userName,
                    onValueChange = {
                        userName = it
                        errorMessage = null
                    },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. John Doe") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Personal Phone Number
                GlassTextField(
                    value = userContact,
                    onValueChange = {
                        userContact = it
                        errorMessage = null
                    },
                    label = { Text("Personal Phone Number") },
                    placeholder = { Text("e.g. +1 234 567 8900") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.2f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. EMERGENCY CONTACT SECTION
                Text(
                    text = "2. Emergency Contact (for SOS)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFFCA5A5)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Family / Emergency Contact Name
                GlassTextField(
                    value = emergencyName,
                    onValueChange = {
                        emergencyName = it
                        errorMessage = null
                    },
                    label = { Text("Family / Emergency Contact Name") },
                    placeholder = { Text("e.g. Sarah Doe (Spouse / Doctor)") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Emergency Phone Number
                GlassTextField(
                    value = emergencyPhone,
                    onValueChange = {
                        emergencyPhone = it
                        errorMessage = null
                    },
                    label = { Text("Emergency Phone Number") },
                    placeholder = { Text("e.g. +1 987 654 3210 or 911") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF87171)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                GlassButton(
                    onClick = {
                        val trimmedName = userName.trim()
                        val trimmedContact = userContact.trim()
                        val trimmedEmergencyName = emergencyName.trim()
                        val trimmedEmergencyPhone = emergencyPhone.trim()

                        if (trimmedName.isBlank()) {
                            errorMessage = "Please enter your full name."
                            return@GlassButton
                        }
                        if (trimmedContact.isBlank()) {
                            errorMessage = "Please enter your personal phone number."
                            return@GlassButton
                        }
                        if (trimmedEmergencyName.isBlank()) {
                            errorMessage = "Please enter family / emergency contact name."
                            return@GlassButton
                        }
                        if (trimmedEmergencyPhone.isBlank()) {
                            errorMessage = "Please enter emergency phone number."
                            return@GlassButton
                        }

                        userPrefs.saveUserDetails(
                            name = trimmedName,
                            contact = trimmedContact,
                            emergencyName = trimmedEmergencyName,
                            emergencyPhone = trimmedEmergencyPhone
                        )
                        onLoginSuccess()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color(0xFF06B6D4).copy(alpha = 0.85f),
                    borderColor = Color(0xFF22D3EE)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Save & Continue",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Save & Continue",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
