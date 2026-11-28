package com.cartvr.app.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cartvr.app.ui.loyalty.BarcodeUtils
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Full-screen reusable barcode & QR scanner powered by CameraX + bundled offline ML Kit.
 *
 * Uses a single-scan [AtomicBoolean] lock while [isPaused] is false so rapid camera frames never
 * trigger duplicate result sheets. Resumes automatically when [isPaused] becomes false again.
 */
@Composable
fun BarcodeScannerDialog(
    title: String = "Scan Barcode",
    subtitle: String = "Align the barcode inside the frame",
    isPaused: Boolean = false,
    isLookingUp: Boolean = false,
    onDismiss: () -> Unit,
    onBarcodeDetected: (rawValue: String, formatName: String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }

    // Single-scan lock: prevents multiple camera frames from firing overlapping sheets.
    val scanLocked = remember { AtomicBoolean(false) }
    val pausedFlag = remember { AtomicBoolean(isPaused) }

    LaunchedEffect(isPaused) {
        pausedFlag.set(isPaused)
        if (!isPaused) {
            scanLocked.set(false)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(torchEnabled, cameraRef) {
        cameraRef?.cameraControl?.enableTorch(torchEnabled)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                CameraBarcodePreview(
                    scanLocked = scanLocked,
                    pausedFlag = pausedFlag,
                    onCameraBound = { cam ->
                        cameraRef = cam
                        hasFlashUnit = cam.cameraInfo.hasFlashUnit()
                        cam.cameraControl.enableTorch(torchEnabled)
                    },
                    onBarcodeDetected = { rawValue, formatName ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBarcodeDetected(rawValue, formatName)
                    }
                )

                // Darkened overlay with centered transparent scan window
                ScannerOverlayBox()
            } else {
                // Friendly camera permission explanation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Camera Access Needed",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Allow camera access so CartVR can scan product barcodes and store loyalty cards—even offline in store aisles.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Allow Camera")
                            }
                            if (permissionRequested) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open App Settings")
                                }
                            }
                            TextButton(onClick = { showManualDialog = true }) {
                                Text("Type Barcode Number Instead")
                            }
                        }
                    }
                }
            }

            // Top bar overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close scanner",
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }

                if (hasCameraPermission && hasFlashUnit) {
                    Surface(
                        shape = CircleShape,
                        color = if (torchEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Black.copy(alpha = 0.55f)
                        }
                    ) {
                        IconButton(onClick = { torchEnabled = !torchEnabled }) {
                            Icon(
                                imageVector = if (torchEnabled) {
                                    Icons.Rounded.FlashlightOn
                                } else {
                                    Icons.Rounded.FlashlightOff
                                },
                                contentDescription = if (torchEnabled) "Turn flashlight off" else "Turn flashlight on",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // Bottom helper pill
            if (hasCameraPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isLookingUp) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Looking up barcode...",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            scanLocked.set(true)
                            showManualDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Type Barcode Manually")
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = {
                showManualDialog = false
                if (!isPaused) scanLocked.set(false)
            },
            title = { Text("Enter Barcode Number") },
            text = {
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("Barcode digits") },
                    placeholder = { Text("e.g. 6001087354921") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = manualCode.trim()
                        if (cleaned.isNotEmpty()) {
                            showManualDialog = false
                            manualCode = ""
                            onBarcodeDetected(cleaned, "CODE_128")
                        }
                    },
                    enabled = manualCode.isNotBlank()
                ) {
                    Text("Look Up")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showManualDialog = false
                        if (!isPaused) scanLocked.set(false)
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScannerOverlayBox() {
    val frameColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        drawRect(Color.Black.copy(alpha = 0.52f))
        val boxWidth = size.width * 0.78f
        val boxHeight = minOf(size.height * 0.28f, boxWidth * 0.62f)
        val left = (size.width - boxWidth) / 2f
        val top = (size.height - boxHeight) / 2f
        val corner = CornerRadius(24.dp.toPx(), 24.dp.toPx())

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = corner,
            blendMode = BlendMode.Clear
        )

        drawRoundRect(
            color = frameColor,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = corner,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
private fun CameraBarcodePreview(
    scanLocked: AtomicBoolean,
    pausedFlag: AtomicBoolean,
    onCameraBound: (Camera) -> Unit,
    onBarcodeDetected: (rawValue: String, formatName: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val frameBusy = remember { AtomicBoolean(false) }
    val barcodeScanner: BarcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var providerRef: ProcessCameraProvider? = null

        val listener = Runnable {
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                providerRef = cameraProvider
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            analyzeFrame(
                                imageProxy = imageProxy,
                                scanner = barcodeScanner,
                                frameBusy = frameBusy,
                                scanLocked = scanLocked,
                                pausedFlag = pausedFlag
                            ) { rawValue, formatName ->
                                mainExecutor.execute {
                                    onBarcodeDetected(rawValue, formatName)
                                }
                            }
                        }
                    }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                onCameraBound(camera)
            }
        }

        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            runCatching { providerRef?.unbindAll() }
            runCatching { barcodeScanner.close() }
            runCatching { cameraExecutor.shutdown() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeFrame(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    frameBusy: AtomicBoolean,
    scanLocked: AtomicBoolean,
    pausedFlag: AtomicBoolean,
    onMatch: (rawValue: String, formatName: String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null || pausedFlag.get() || scanLocked.get() || !frameBusy.compareAndSet(false, true)) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            if (!pausedFlag.get() && !scanLocked.get()) {
                val match = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                val rawValue = match?.rawValue?.trim()
                if (match != null && !rawValue.isNullOrEmpty() && scanLocked.compareAndSet(false, true)) {
                    val formatName = BarcodeUtils.mlkitFormatToName(match.format)
                    onMatch(rawValue, formatName)
                }
            }
        }
        .addOnCompleteListener {
            frameBusy.set(false)
            imageProxy.close()
        }
}
