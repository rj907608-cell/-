package com.pharmacy.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Size
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * تشغيل اهتزاز ونغمة تنبيه لطيفة عند التقاط الباركود بنجاح
 */
private fun playScanFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70)
            }
        }
    } catch (_: Exception) {}

    try {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    } catch (_: Exception) {}
}

/**
 * محلل الباركود المزدوج: يعتمد على محرك Google ML Kit الذكي لدعم جميع باركودات الأدوية (EAN-13, EAN-8, UPC, Code 128)
 * مع محرك ZXing الداعم للدوران وتصحيح الإضاءة كخيار احتياطي أوفلاين 100%.
 */
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val mlKitOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_DATA_MATRIX
        )
        .build()

    private val mlKitScanner = BarcodeScanning.getClient(mlKitOptions)

    private val zxingReader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.QR_CODE
                )
            )
        )
    }

    private var isDetected = false
    private var lastScannedTime = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (isDetected || currentTime - lastScannedTime < 1000) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            mlKitScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val detectedCode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                    if (!detectedCode.isNullOrBlank() && !isDetected) {
                        isDetected = true
                        lastScannedTime = currentTime
                        onBarcodeDetected(detectedCode.trim())
                    } else {
                        fallbackZxing(imageProxy, currentTime)
                    }
                }
                .addOnFailureListener {
                    fallbackZxing(imageProxy, currentTime)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            fallbackZxing(imageProxy, currentTime)
            imageProxy.close()
        }
    }

    private fun fallbackZxing(imageProxy: ImageProxy, currentTime: Long) {
        if (isDetected) return
        try {
            val plane = imageProxy.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val width = imageProxy.width
            val height = imageProxy.height

            val yData = ByteArray(width * height)
            for (y in 0 until height) {
                if (pixelStride == 1) {
                    buffer.position(y * rowStride)
                    buffer.get(yData, y * width, width)
                } else {
                    for (x in 0 until width) {
                        yData[y * width + x] = buffer.get(y * rowStride + x * pixelStride)
                    }
                }
            }

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val (rotatedData, finalWidth, finalHeight) = rotateYData(yData, width, height, rotationDegrees)

            val source = PlanarYUVLuminanceSource(
                rotatedData,
                finalWidth,
                finalHeight,
                0,
                0,
                finalWidth,
                finalHeight,
                false
            )

            var resultText: String? = null
            try {
                val bitmap = BinaryBitmap(HybridBinarizer(source))
                val result = zxingReader.decodeWithState(bitmap)
                resultText = result?.text
            } catch (_: Exception) {
                try {
                    val bitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
                    val result = zxingReader.decodeWithState(bitmap)
                    resultText = result?.text
                } catch (_: Exception) {}
            } finally {
                zxingReader.reset()
            }

            if (!resultText.isNullOrBlank() && !isDetected) {
                isDetected = true
                lastScannedTime = currentTime
                onBarcodeDetected(resultText.trim())
            }
        } catch (_: Exception) {}
    }

    private fun rotateYData(
        data: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int
    ): Triple<ByteArray, Int, Int> {
        return when (rotationDegrees) {
            90 -> {
                val rotated = ByteArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[x * height + (height - y - 1)] = data[y * width + x]
                    }
                }
                Triple(rotated, height, width)
            }
            180 -> {
                val rotated = ByteArray(width * height)
                for (i in 0 until width * height) {
                    rotated[width * height - 1 - i] = data[i]
                }
                Triple(rotated, width, height)
            }
            270 -> {
                val rotated = ByteArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[(width - x - 1) * height + y] = data[y * width + x]
                    }
                }
                Triple(rotated, height, width)
            }
            else -> Triple(data, width, height)
        }
    }
}

/**
 * واجهة مسح الباركود بالكاميرا مع أزرار التحكم (الفلاش والتقريب والتركيز التلقائي)
 */
@Composable
fun BarcodeScannerDialog(
    sampleBarcodes: List<Pair<String, String>> = emptyList(),
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualBarcodeInput by remember { mutableStateOf("") }
    var torchEnabled by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 520.dp, max = 620.dp)
                .testTag("barcode_scanner_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // شريط العنوان وأزرار الغلق
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner Icon",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "قارئ الباركود (الكاميرا)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }

                // منطقة الكاميرا
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        CameraPreviewWithScanner(
                            torchEnabled = torchEnabled,
                            zoomRatio = zoomRatio,
                            onBarcodeDetected = { code ->
                                mainHandler.post {
                                    playScanFeedback(context)
                                    onBarcodeScanned(code)
                                    onDismiss()
                                }
                            }
                        )

                        // إطار المسح والخط الليزري المتحرك
                        ScannerOverlay()

                        // شريط أدوات التحكم السريع (الفلاش والتقريب 1x / 2x)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // زر التقريب Zoom
                            IconButton(
                                onClick = {
                                    zoomRatio = if (zoomRatio <= 1.2f) 2f else 1f
                                },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (zoomRatio > 1.2f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                                    contentDescription = "Zoom",
                                    tint = Color.White
                                )
                            }

                            // زر الفلاش Torch
                            IconButton(
                                onClick = { torchEnabled = !torchEnabled },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Flash",
                                    tint = if (torchEnabled) Color.Yellow else Color.White
                                )
                            }
                        }

                        // نص إرشادي أسفل إطار المسح
                        Text(
                            text = "وجّه الكاميرا نحو باركود الدواء (مثل EAN-13)\nالمس الشاشة في حال رغبت بضبط التركيز",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    } else {
                        // طلب الصلاحية إذا لم تمنح
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = "Camera Required",
                                tint = Color.LightGray,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "مطلوب إذن الكاميرا لمسح باركود الأدوية",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("منح الصلاحية الآن")
                            }
                        }
                    }
                }

                // الجزء السفلي: إدخال يدوي برقم الباركود
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualBarcodeInput,
                            onValueChange = { manualBarcodeInput = it },
                            label = { Text("أو اكتب الباركود يدوياً") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_barcode_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualBarcodeInput.isNotBlank()) {
                                    playScanFeedback(context)
                                    onBarcodeScanned(manualBarcodeInput.trim())
                                    onDismiss()
                                }
                            },
                            enabled = manualBarcodeInput.isNotBlank(),
                            modifier = Modifier.testTag("apply_manual_barcode_btn")
                        ) {
                            Text("تأكيد")
                        }
                    }
                }
            }
        }
    }
}

/**
 * مكون الكاميرا عبر CameraX و PreviewView مع دعم التركيز باللمس والتقريب والدقة العالية
 */
@Composable
private fun CameraPreviewWithScanner(
    torchEnabled: Boolean,
    zoomRatio: Float,
    onBarcodeDetected: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    LaunchedEffect(torchEnabled) {
        cameraControl?.enableTorch(torchEnabled)
    }

    LaunchedEffect(zoomRatio) {
        cameraControl?.setZoomRatio(zoomRatio)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            // تفعيل التركيز التلقائي عند لمس الشاشة (Tap-to-Focus)
            previewView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val factory = previewView.meteringPointFactory
                    val point = factory.createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()
                    cameraControl?.startFocusAndMetering(action)
                    v.performClick()
                }
                true
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val targetResolution = Size(1280, 720)

                val preview = Preview.Builder()
                    .setTargetResolution(targetResolution)
                    .build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(targetResolution)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(onBarcodeDetected))
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
                    cameraControl?.setZoomRatio(zoomRatio)
                } catch (_: Exception) {
                    // فشل تشغيل الكاميرا
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * مستطيل المسح والخط الليزري الأحمر المتحرك وزوايا التركيز
 */
@Composable
private fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_line")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier
            .size(260.dp, 160.dp)
            .border(2.dp, Color(0xFF00E676), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height * laserProgress
            drawLine(
                color = Color(0xFFFF1744),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 3.5.dp.toPx()
            )
        }
    }
}
