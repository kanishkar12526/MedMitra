package com.example.medmitra.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medmitra.ui.components.GlassBackground
import com.example.medmitra.ui.components.GlassButton
import com.example.medmitra.ui.components.GlassCard
import com.example.medmitra.ui.components.GlassTopAppBar
import com.example.medmitra.util.ParsedPrescriptionItem
import com.example.medmitra.viewmodel.CameraUiState
import com.example.medmitra.viewmodel.CameraViewModel
import com.example.medmitra.viewmodel.ScanMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel(),
    onNavigateToDashboard: () -> Unit = {}
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()
    val scanMode by viewModel.scanMode.collectAsState()

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Medication Scanner",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode Selector Segmented Tabs
                ScanModeSelector(
                    selectedMode = scanMode,
                    onModeSelected = { viewModel.setScanMode(it) }
                )

                // 100% Offline Processing Badge Banner
                if (scanMode == ScanMode.SCAN_PRESCRIPTION) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% Offline Processing ⚡",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF34D399)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ML Kit On-Device OCR",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraPermissionState.status.isGranted) {
                        CameraContent(
                            scanMode = scanMode,
                            uiState = uiState,
                            onAnalyze = { bitmap -> viewModel.analyzeImage(bitmap) },
                            onReset = { viewModel.resetState() },
                            onNavigateToDashboard = onNavigateToDashboard
                        )
                    } else {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF22D3EE),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Camera Permission Required",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Please grant camera permission to scan medicine labels or doctor prescriptions.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                GlassButton(
                                    onClick = { cameraPermissionState.launchPermissionRequest() }
                                ) {
                                    Text("Grant Camera Permission")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanModeSelector(
    selectedMode: ScanMode,
    onModeSelected: (ScanMode) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(6.dp),
        backgroundColor = Color.White.copy(alpha = 0.08f),
        borderColor = Color.White.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isInfoSelected = selectedMode == ScanMode.MEDICINE_INFO
            GlassButton(
                onClick = { onModeSelected(ScanMode.MEDICINE_INFO) },
                modifier = Modifier.weight(1f),
                backgroundColor = if (isInfoSelected) Color(0xFF06B6D4).copy(alpha = 0.85f) else Color.Transparent,
                borderColor = if (isInfoSelected) Color(0xFF67E8F9) else Color.White.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Medicine Info",
                    fontWeight = if (isInfoSelected) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val isRxSelected = selectedMode == ScanMode.SCAN_PRESCRIPTION
            GlassButton(
                onClick = { onModeSelected(ScanMode.SCAN_PRESCRIPTION) },
                modifier = Modifier.weight(1f),
                backgroundColor = if (isRxSelected) Color(0xFF06B6D4).copy(alpha = 0.85f) else Color.Transparent,
                borderColor = if (isRxSelected) Color(0xFF67E8F9) else Color.White.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Scan Prescription",
                    fontWeight = if (isRxSelected) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CameraContent(
    scanMode: ScanMode,
    uiState: CameraUiState,
    onAnalyze: (Bitmap) -> Unit,
    onReset: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState is CameraUiState.Idle) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.2f),
                border = BorderStroke(
                    2.dp,
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF22D3EE).copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.2f)
                        )
                    )
                )
            ) {
                CameraPreview(
                    imageCapture = imageCapture,
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Instructional Overlay Badge
            GlassCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                backgroundColor = Color(0xFF0F172A).copy(alpha = 0.75f),
                borderColor = Color(0xFF22D3EE).copy(alpha = 0.5f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (scanMode == ScanMode.MEDICINE_INFO)
                        "Point camera at pill or medicine label"
                    else
                        "Point camera at printed doctor's prescription",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            // Glass Overlay Control Box
            GlassButton(
                onClick = {
                    takePhoto(
                        imageCapture = imageCapture,
                        executor = ContextCompat.getMainExecutor(context),
                        onImageCaptured = onAnalyze,
                        onError = { Log.e("CameraScreen", "Capture error", it) }
                    )
                },
                backgroundColor = Color(0xFF06B6D4).copy(alpha = 0.9f),
                borderColor = Color(0xFF67E8F9),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (scanMode == ScanMode.MEDICINE_INFO)
                        "CAPTURE & ANALYZE INFO"
                    else
                        "OFFLINE SCAN & AUTO-SCHEDULE ⚡",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else {
            ResultView(
                scanMode = scanMode,
                uiState = uiState,
                onReset = onReset,
                onNavigateToDashboard = onNavigateToDashboard
            )
        }
    }
}

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier
) {
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }

    LaunchedEffect(Unit) {
        val cameraProvider = context.getCameraProvider()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        previewUseCase = Preview.Builder().build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                imageCapture
            )
        } catch (exc: Exception) {
            Log.e("CameraScreen", "Use case binding failed", exc)
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                previewUseCase?.surfaceProvider = this.surfaceProvider
            }
        },
        update = { view ->
            previewUseCase?.surfaceProvider = view.surfaceProvider
        },
        modifier = modifier
    )
}

@Composable
fun ResultView(
    scanMode: ScanMode,
    uiState: CameraUiState,
    onReset: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (uiState) {
            is CameraUiState.Loading -> {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFF22D3EE))
                        Text(
                            text = if (scanMode == ScanMode.MEDICINE_INFO)
                                "Analyzing medicine with Gemini AI..."
                            else
                                "Processing prescription 100% offline on-device...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            is CameraUiState.Success -> {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Medicine Analysis Result",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF22D3EE)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.result,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    GlassButton(
                        onClick = onReset,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Another")
                    }
                }
            }
            is CameraUiState.PrescriptionSuccess -> {
                PrescriptionConfirmationCard(
                    items = uiState.items,
                    onReset = onReset,
                    onNavigateToDashboard = onNavigateToDashboard
                )
            }
            is CameraUiState.Error -> {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                    borderColor = Color(0xFFFCA5A5).copy(alpha = 0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analysis Failed",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFCA5A5)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.message,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    GlassButton(
                        onClick = onReset,
                        backgroundColor = Color(0xFFEF4444).copy(alpha = 0.4f),
                        borderColor = Color(0xFFFCA5A5),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Try Again")
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun PrescriptionConfirmationCard(
    items: List<ParsedPrescriptionItem>,
    onReset: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF06B6D4).copy(alpha = 0.15f),
        borderColor = Color(0xFF22D3EE).copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Prescription Processed!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "${items.size} medication(s) auto-scheduled offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF67E8F9)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF10B981).copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "100% Offline ⚡",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF34D399)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.White.copy(alpha = 0.2f),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Dosage: ${item.dosage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF22D3EE),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.times.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF22D3EE)
                                )
                            }
                        }
                        if (item.instructions.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Instructions: ${item.instructions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassButton(
                    onClick = onReset,
                    backgroundColor = Color.White.copy(alpha = 0.12f),
                    borderColor = Color.White.copy(alpha = 0.3f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Another")
                }

                GlassButton(
                    onClick = onNavigateToDashboard,
                    backgroundColor = Color(0xFF06B6D4).copy(alpha = 0.85f),
                    borderColor = Color(0xFF67E8F9)
                ) {
                    Text("Go to Dashboard", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun takePhoto(
    imageCapture: ImageCapture,
    executor: Executor,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                onImageCaptured(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
    val listenableFuture = ProcessCameraProvider.getInstance(this)
    listenableFuture.addListener({
        try {
            continuation.resume(listenableFuture.get())
        } catch (e: Exception) {
            continuation.cancel(e)
        }
    }, ContextCompat.getMainExecutor(this))
}
