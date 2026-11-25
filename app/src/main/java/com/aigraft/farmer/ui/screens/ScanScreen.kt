package com.aigraft.farmer.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aigraft.farmer.data.ScanMode
import com.aigraft.farmer.data.modeLabel
import com.aigraft.farmer.i18n.AppStrings
import com.aigraft.farmer.ui.theme.AiGraftTokens
import com.aigraft.farmer.ui.theme.Baloo2FontFamily
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "ScanScreen"

@Composable
fun ScanScreen(
    mode: ScanMode,
    batchId: String?,
    strings: AppStrings,
    isPreparingPhoto: Boolean,
    onPrepareFile: suspend (File, ScanMode, String?) -> Boolean,
    onPrepareUri: suspend (Uri, ScanMode, String?) -> Boolean,
    onPhotoReady: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = AiGraftTokens.colors
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequestedOnce = true
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = onPrepareUri(uri, mode, batchId)
                if (ok) onPhotoReady()
            }
        }
    }

    var cameraReady by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val busy = capturing || isPreparingPhoto

    LaunchedEffect(torchOn, boundCamera) {
        runCatching { boundCamera?.cameraControl?.enableTorch(torchOn) }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surfaceInverse)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("camera_permission_view")
        ) {
            RoundOverlayButton(
                onClick = onClose,
                testTagName = "scan_close_button",
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = strings.done,
                    tint = colors.onSurfaceInverse,
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(colors.brandPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        tint = colors.onBrandPrimary,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.camNeedTitle,
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = colors.onSurfaceInverse,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = strings.camNeedText,
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = colors.onSurfaceInverse.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag(if (permissionRequestedOnce) "open_settings_button" else "allow_camera_button"),
                    shape = RoundedCornerShape(999.dp),
                    color = colors.brandPrimary,
                    onClick = {
                        if (permissionRequestedOnce) {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        Icon(
                            imageVector = if (permissionRequestedOnce) Icons.Rounded.Settings else Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.onBrandPrimary,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            text = if (permissionRequestedOnce) strings.openSettings else strings.allowCamera,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = colors.onBrandPrimary,
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("pick_gallery_button"),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, colors.onSurfaceInverse),
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = colors.onSurfaceInverse,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = strings.useGallery,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = colors.onSurfaceInverse,
                        )
                    }
                }
            }
        }
        return
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var provider: ProcessCameraProvider? = null

        val listener = Runnable {
            try {
                provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = if (provider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) == true) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                provider?.unbindAll()
                boundCamera = provider?.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                cameraReady = true
            } catch (e: Exception) {
                Log.e(TAG, "CameraX bind failed", e)
            }
        }
        providerFuture.addListener(listener, executor)

        onDispose {
            runCatching { provider?.unbindAll() }
            boundCamera = null
        }
    }

    val capturePhoto = {
        if (cameraReady && !busy) {
            capturing = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            val tempFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        scope.launch {
                            val ok = onPrepareFile(tempFile, mode, batchId)
                            runCatching { tempFile.delete() }
                            capturing = false
                            if (ok) onPhotoReady()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "ImageCapture failed", exception)
                        runCatching { tempFile.delete() }
                        capturing = false
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceInverse)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_view"),
        )

        // Top overlay: Close, Mode pill, Torch toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RoundOverlayButton(
                onClick = onClose,
                testTagName = "scan_close_button",
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = strings.done,
                    tint = colors.onSurfaceInverse,
                    modifier = Modifier.size(28.dp),
                )
            }

            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.brandPrimary)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = when (mode) {
                        ScanMode.SEEDLING -> Icons.Rounded.Eco
                        ScanMode.GUIDE -> Icons.Rounded.ContentCut
                        ScanMode.GRAFT -> Icons.Rounded.CallMerge
                    },
                    contentDescription = null,
                    tint = colors.onBrandPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = modeLabel(mode, strings),
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = colors.onBrandPrimary,
                )
            }

            RoundOverlayButton(
                onClick = { torchOn = !torchOn },
                testTagName = "torch_toggle",
                background = if (torchOn) colors.warning else Color.Black.copy(alpha = 0.55f),
            ) {
                Icon(
                    imageVector = if (torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    contentDescription = strings.light,
                    tint = if (torchOn) colors.onWarning else colors.onSurfaceInverse,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // Center dashed framing box + farmer hint pill
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .aspectRatio(0.8f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                        color = colors.brandTertiary,
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                        style = Stroke(
                            width = 4.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(28f, 18f), 0f),
                        ),
                    )
                }
            }
            Text(
                text = if (mode == ScanMode.GUIDE) strings.guideHint else strings.keepInBox,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = colors.onSurfaceInverse,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        // Bottom Bar: Gallery + Huge Shutter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !busy) {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .padding(vertical = 6.dp)
                    .testTag("gallery_button"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = strings.gallery,
                    tint = colors.onSurfaceInverse,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = strings.gallery,
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.onSurfaceInverse,
                )
            }

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .alpha(if (!cameraReady || busy) 0.5f else 1f)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(6.dp, colors.brandTertiary, CircleShape)
                    .clickable(enabled = cameraReady && !busy, onClick = capturePhoto)
                    .testTag("capture_button"),
                contentAlignment = Alignment.Center,
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        color = colors.brandPrimary,
                        modifier = Modifier.size(34.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(colors.brandPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.width(72.dp))
        }

        if (busy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .testTag("preparing_overlay"),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = colors.brandTertiary)
                    Text(
                        text = strings.preparing,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = colors.onSurfaceInverse,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundOverlayButton(
    onClick: () -> Unit,
    testTagName: String,
    modifier: Modifier = Modifier,
    background: Color = Color.Black.copy(alpha = 0.55f),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .testTag(testTagName),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
