package com.techsultan.zenithpro.features.product.presentation

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.ExperimentalGetImage
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.techsultan.zenithpro.core.util.Util.vibrate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Boolean,
    onBack: () -> Unit,
    onTypeBarcode: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()
    
    var hasCameraPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission is required to scan barcodes", color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val scanner = BarcodeScanning.getClient()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isScanning) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (isScanning && barcodes.isNotEmpty()) {
                                        barcodes.firstOrNull()?.rawValue?.let { barcode ->
                                            isScanning = false
                                            vibrate(context)
                                            val handled = onBarcodeScanned(barcode)
                                            if (handled) {
                                                isScanning = false
                                            } else {
                                                errorMessage = "Product not found"
                                                isScanning = false
                                                scope.launch {
                                                    delay(2000)
                                                    errorMessage = null
                                                    isScanning = true
                                                }
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera.cameraControl
                    } catch (exc: Exception) {
                        Log.e("BarcodeScanner", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(context))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with cutout
        ScannerOverlay()

        // Top Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Scan Barcode",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Align barcode within frame",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }

            IconButton(
                onClick = {
                    isFlashOn = !isFlashOn
                    cameraControl?.enableTorch(isFlashOn)
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }
        }

        // Error Message Overlay
        errorMessage?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 260.dp)
            ) {
                Surface(
                    color = Color.Red.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = it,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Scanning is automatic. Keep the\nphone steady for better results.",
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Button(
                onClick = onTypeBarcode,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(56.dp).padding(horizontal = 32.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Type Barcode Instead", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 4.dp.toPx()
        val cornerLength = 40.dp.toPx()
        val boxSize = 260.dp.toPx()
        val left = (size.width - boxSize) / 2
        val top = (size.height - boxSize) / 2.5f
        val rectSize = Size(boxSize, boxSize)

        // Dark background with cutout
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            size = size
        )
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = rectSize,
            cornerRadius = CornerRadius(12.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // Green Corners
        val green = Color(0xFF4CAF50)
        
        // Top Left
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(left, top + cornerLength)
                lineTo(left, top + 12.dp.toPx())
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(left, top, left + 24.dp.toPx(), top + 24.dp.toPx()),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left + cornerLength, top)
            },
            color = green,
            style = Stroke(width = strokeWidth)
        )

        // Top Right
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(left + boxSize - cornerLength, top)
                lineTo(left + boxSize - 12.dp.toPx(), top)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(left + boxSize - 24.dp.toPx(), top, left + boxSize, top + 24.dp.toPx()),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left + boxSize, top + cornerLength)
            },
            color = green,
            style = Stroke(width = strokeWidth)
        )

        // Bottom Left
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(left, top + boxSize - cornerLength)
                lineTo(left, top + boxSize - 12.dp.toPx())
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(left, top + boxSize - 24.dp.toPx(), left + 24.dp.toPx(), top + boxSize),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(left + cornerLength, top + boxSize)
            },
            color = green,
            style = Stroke(width = strokeWidth)
        )

        // Bottom Right
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(left + boxSize - cornerLength, top + boxSize)
                lineTo(left + boxSize - 12.dp.toPx(), top + boxSize)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(left + boxSize - 24.dp.toPx(), top + boxSize - 24.dp.toPx(), left + boxSize, top + boxSize),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(left + boxSize, top + boxSize - cornerLength)
            },
            color = green,
            style = Stroke(width = strokeWidth)
        )
        
        // Horizontal Scanning Line
        val lineY = top + (boxSize / 2)
        drawLine(
            color = green.copy(alpha = 0.5f),
            start = Offset(left + 8.dp.toPx(), lineY),
            end = Offset(left + boxSize - 8.dp.toPx(), lineY),
            strokeWidth = 2.dp.toPx()
        )
    }
}
