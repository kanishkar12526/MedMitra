package com.example.medmitra.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// World-Class Liquid Glass Theme Palette
val GlassObsidianBackground = Color(0xFF0B0F19)
val GlassWhiteBackground = Color.White.copy(alpha = 0.12f)
val GlassWhiteBorder = Color.White.copy(alpha = 0.42f)
val GlassDarkBackground = Color(0xFF0F172A).copy(alpha = 0.78f)
val GlassPrimaryBackground = Color(0xFF06B6D4).copy(alpha = 0.22f)
val GlassPrimaryBorder = Color(0xFF22D3EE).copy(alpha = 0.55f)

/**
 * Premium Liquid Glass Background with living animated ambient light meshes.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_mesh")
    val pulseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meshPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070A14), // Obsidian Navy
                        Color(0xFF0F172A), // Slate Glass
                        Color(0xFF0A192F)  // Deep Cyan Teal
                    )
                )
            )
    ) {
        // Living Liquid Light Orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Cyan Light Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.35f), Color.Transparent)
                ),
                radius = width * 0.75f,
                center = Offset(width * 0.2f + pulseOffset, height * 0.15f - pulseOffset)
            )

            // Violet Light Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.28f), Color.Transparent)
                ),
                radius = width * 0.85f,
                center = Offset(width * 0.85f - pulseOffset, height * 0.60f + pulseOffset)
            )

            // Emerald Light Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF10B981).copy(alpha = 0.22f), Color.Transparent)
                ),
                radius = width * 0.65f,
                center = Offset(width * 0.35f - pulseOffset, height * 0.85f)
            )
        }

        content()
    }
}

/**
 * Ultra-Refined Liquid Glass Card Container with Specular Light Borders & Press Physics.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassWhiteBackground,
    borderColor: Color = GlassWhiteBorder,
    borderWidth: Dp = 1.2.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.985f else 1f

    val liquidBorderBrush = Brush.linearGradient(
        colors = listOf(
            borderColor,
            Color.White.copy(alpha = 0.55f),
            borderColor.copy(alpha = 0.15f)
        )
    )

    val cardModifier = if (onClick != null) {
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    } else {
        modifier
            .clip(shape)
            .background(backgroundColor)
    }

    Surface(
        modifier = cardModifier,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(borderWidth, liquidBorderBrush)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Liquid Glass Button with Micro-Press Physics.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    backgroundColor: Color = GlassPrimaryBackground,
    borderColor: Color = GlassPrimaryBorder,
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.96f else 1f

    val liquidBorderBrush = Brush.linearGradient(
        colors = listOf(
            borderColor,
            Color.White.copy(alpha = 0.6f),
            borderColor.copy(alpha = 0.2f)
        )
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.1f),
            disabledContentColor = contentColor.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.2.dp, liquidBorderBrush),
        content = content
    )
}

/**
 * Liquid Glass Icon Button.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassWhiteBackground,
    borderColor: Color = GlassWhiteBorder,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.92f else 1f

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(
                1.2.dp,
                Brush.linearGradient(
                    listOf(borderColor, Color.White.copy(alpha = 0.5f), borderColor.copy(alpha = 0.2f))
                )
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

/**
 * Liquid Glass Outlined TextField.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.14f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
            disabledContainerColor = Color.White.copy(alpha = 0.04f),
            focusedBorderColor = Color(0xFF22D3EE),
            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
            focusedLabelColor = Color(0xFF22D3EE),
            unfocusedLabelColor = Color.White.copy(alpha = 0.75f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF22D3EE)
        )
    )
}

/**
 * Liquid Glass Top App Bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.1f),
        border = BorderStroke(
            1.2.dp,
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        )
    ) {
        TopAppBar(
            title = title,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }
}

/**
 * Liquid Glass Dialog Surface.
 */
@Composable
fun GlassDialogSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    backgroundColor: Color = GlassDarkBackground,
    borderColor: Color = GlassWhiteBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                colors = listOf(
                    borderColor,
                    Color.White.copy(alpha = 0.6f),
                    borderColor.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content
        )
    }
}
