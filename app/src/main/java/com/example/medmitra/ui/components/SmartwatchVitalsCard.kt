package com.example.medmitra.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medmitra.data.local.VitalReading

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartwatchVitalsCard(
    currentReading: VitalReading?,
    isConnected: Boolean,
    onToggleConnection: (Boolean) -> Unit,
    onSimulateHighHR: () -> Unit,
    onSimulateLowSpO2: () -> Unit,
    onSimulateNormal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHRAbnormal = currentReading != null && (currentReading.heartRate < 50 || currentReading.heartRate > 120)
    val isSpO2Abnormal = currentReading != null && currentReading.spo2 < 90
    val isAbnormal = isHRAbnormal || isSpO2Abnormal || (currentReading?.isAbnormal == true)

    val cardBg = if (isAbnormal) Color(0xFFEF4444).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.1f)
    val cardBorder = if (isAbnormal) Color(0xFFFCA5A5).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.25f)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = cardBg,
        borderColor = cardBorder
    ) {
        // Card Header: Smartwatch Title & Connection Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06B6D4).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = "Smartwatch",
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Smartwatch Vitals",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Real-time health stream",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Connection Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isConnected) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f))
                    .clickable { onToggleConnection(!isConnected) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Text(
                        text = if (isConnected) "Watch Connected 🟢" else "Disconnected 🔴",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isConnected) Color(0xFF34D399) else Color(0xFFFCA5A5)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Vitals Metric Grid (2 x 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Heart Rate Metric Tile
            VitalMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Favorite,
                iconTint = if (isHRAbnormal) Color(0xFFF87171) else Color(0xFFEC4899),
                label = "Heart Rate",
                value = if (currentReading != null) "${currentReading.heartRate} BPM" else "-- BPM",
                isAbnormal = isHRAbnormal,
                isHeartRate = true
            )

            // SpO2 Metric Tile
            VitalMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WaterDrop,
                iconTint = if (isSpO2Abnormal) Color(0xFFF87171) else Color(0xFF38BDF8),
                label = "SpO2 Oxygen",
                value = if (currentReading != null) "${currentReading.spo2}%" else "-- %",
                isAbnormal = isSpO2Abnormal
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Body Temp Metric Tile
            VitalMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Thermostat,
                iconTint = Color(0xFFF59E0B),
                label = "Body Temp",
                value = if (currentReading != null) "${currentReading.bodyTemp} °C" else "-- °C",
                isAbnormal = false
            )

            // Daily Steps Metric Tile
            VitalMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                iconTint = Color(0xFF10B981),
                label = "Daily Steps",
                value = if (currentReading != null) "%,d".format(currentReading.steps) else "--",
                isAbnormal = false
            )
        }

        // Abnormal Reading Alert Banner
        if (isAbnormal && currentReading != null) {
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.25f))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Abnormal Alert",
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(28.dp)
                    )

                    Column {
                        Text(
                            text = "Abnormal Vital Reading Alert!",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFCA5A5)
                        )
                        val details = buildString {
                            if (isHRAbnormal) append("Heart Rate out of bounds (${currentReading.heartRate} BPM). ")
                            if (isSpO2Abnormal) append("Oxygen SpO2 critical (${currentReading.spo2}%). ")
                        }
                        Text(
                            text = details + "20s Emergency SOS countdown triggered.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Simulation Test Toolbar
        Text(
            text = "Smartwatch Test & Simulation Stream:",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.75f)
        )

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassButton(
                onClick = onSimulateHighHR,
                backgroundColor = Color(0xFFEF4444).copy(alpha = 0.25f),
                borderColor = Color(0xFFFCA5A5).copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("High HR (>120) ⚠️", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            GlassButton(
                onClick = onSimulateLowSpO2,
                backgroundColor = Color(0xFFEF4444).copy(alpha = 0.25f),
                borderColor = Color(0xFFFCA5A5).copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Low SpO2 (<90%) ⚠️", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            GlassButton(
                onClick = onSimulateNormal,
                backgroundColor = Color(0xFF10B981).copy(alpha = 0.25f),
                borderColor = Color(0xFF34D399).copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Normal 🟢", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun VitalMetricTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    isAbnormal: Boolean,
    modifier: Modifier = Modifier,
    isHeartRate: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isHeartRate) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val tileBg = if (isAbnormal) Color(0xFFEF4444).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
    val tileBorder = if (isAbnormal) Color(0xFFFCA5A5).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f)

    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        color = tileBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, tileBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier
                        .size(20.dp)
                        .scale(if (isHeartRate) pulseScale else 1f)
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isAbnormal) Color(0xFFFCA5A5) else Color.White
            )

            if (isAbnormal) {
                Text(
                    text = "ABNORMAL",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF87171)
                )
            }
        }
    }
}
