package com.example.medmitra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.medmitra.sos.SOSSource
import com.example.medmitra.sos.SOSState

@Composable
fun SOSDialog(
    sosState: SOSState,
    onCancel: () -> Unit,
    onCallNow: () -> Unit,
    onDismissDispatched: () -> Unit
) {
    if (sosState is SOSState.Idle) return

    Dialog(
        onDismissRequest = {
            if (sosState is SOSState.Countdown) {
                onCancel()
            } else {
                onDismissDispatched()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = sosState !is SOSState.Countdown,
            dismissOnClickOutside = false
        )
    ) {
        GlassDialogSurface(
            backgroundColor = Color(0xFF0F172A).copy(alpha = 0.92f),
            borderColor = Color(0xFFEF4444).copy(alpha = 0.6f)
        ) {
            when (sosState) {
                is SOSState.Countdown -> {
                    val titleText = when (sosState.source) {
                        SOSSource.FALL_DETECTION -> "FALL DETECTED!"
                        SOSSource.ABNORMAL_VITALS -> "CRITICAL VITALS ALERT!"
                        SOSSource.MANUAL_TRIGGER -> "EMERGENCY SOS ALERT"
                    }

                    val subtitleText = when (sosState.source) {
                        SOSSource.FALL_DETECTION -> "A sudden fall impact was detected. Calling emergency contact in:"
                        SOSSource.ABNORMAL_VITALS -> "Abnormal smartwatch vitals detected (Heart Rate or SpO2 out of range). Calling emergency contact in:"
                        SOSSource.MANUAL_TRIGGER -> "Emergency SOS triggered manually. Calling emergency contact in:"
                    }

                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(56.dp)
                    )

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF87171),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    // Countdown circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                    ) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = sosState.secondsRemaining / 20f,
                            label = "countdown"
                        )

                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFFEF4444),
                            strokeWidth = 8.dp,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )

                        Text(
                            text = sosState.secondsRemaining.toString(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            backgroundColor = Color.White.copy(alpha = 0.12f),
                            borderColor = Color.White.copy(alpha = 0.35f),
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("I'M SAFE", fontWeight = FontWeight.Bold)
                        }

                        GlassButton(
                            onClick = onCallNow,
                            modifier = Modifier.weight(1f),
                            backgroundColor = Color(0xFFEF4444).copy(alpha = 0.85f),
                            borderColor = Color(0xFFFCA5A5),
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CALL NOW", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is SOSState.Dispatched -> {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Dispatched",
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(56.dp)
                    )

                    Text(
                        text = "EMERGENCY DISPATCHED",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF22D3EE),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Emergency contact: ${sosState.contact}\n" +
                                (if (sosState.smsSent) "✓ Emergency SMS dispatched.\n" else "") +
                                (if (sosState.callInitiated) "✓ Phone dialer launched." else ""),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GlassButton(
                        onClick = onDismissDispatched,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("DISMISS", fontWeight = FontWeight.Bold)
                    }
                }

                else -> {}
            }
        }
    }
}
