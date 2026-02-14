package de.app.instagram.create.presentation.ui

import android.Manifest
import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.core.Camera
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import java.util.Locale
import kotlinx.coroutines.delay

private data class GalleryImage(
    val id: Long,
    val uri: Uri,
)

@Composable
actual fun PlatformCreateScreen(
    modifier: Modifier,
) {
    val context = LocalContext.current
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var hasCameraPermission by remember { mutableStateOf(hasCameraPermission(context)) }
    var hasGalleryPermission by remember { mutableStateOf(hasGalleryPermission(context)) }
    var galleryImages by remember { mutableStateOf(emptyList<GalleryImage>()) }
    var selectedImageId by remember { mutableStateOf<Long?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasCameraPermission = hasCameraPermission(context)
        hasGalleryPermission = hasGalleryPermission(context)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasGalleryPermission) {
            permissionsLauncher.launch(requiredPermissions())
        }
    }

    LaunchedEffect(hasGalleryPermission) {
        galleryImages = if (hasGalleryPermission) {
            loadGalleryImages(context)
        } else {
            emptyList()
        }
        if (selectedImageId == null) {
            selectedImageId = galleryImages.firstOrNull()?.id
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Selected gallery image",
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (hasCameraPermission) {
                CameraPreview(
                    lensFacing = lensFacing,
                    onImageCaptureReady = { imageCapture = it },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PermissionMessage(
                    text = "Camera permission is required to open the camera.",
                    onGrant = { permissionsLauncher.launch(requiredPermissions()) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (selectedImageUri == null) {
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = "Flip camera",
                        tint = Color.White,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .clickable(enabled = !isCapturing && hasCameraPermission) {
                            val capture = imageCapture ?: return@clickable
                            isCapturing = true
                            capturePhoto(
                                context = context,
                                imageCapture = capture,
                                onSaved = { uri ->
                                    selectedImageUri = uri
                                    galleryImages = loadGalleryImages(context)
                                    selectedImageId = galleryImages.firstOrNull { it.uri == uri }?.id
                                    isCapturing = false
                                },
                                onError = {
                                    isCapturing = false
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Capture photo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            } else {
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Back to camera",
                        tint = Color.White,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(vertical = 10.dp),
        ) {
            Text(
                text = "Gallery",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (hasGalleryPermission) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                ) {
                    items(galleryImages, key = { it.id }) { image ->
                        val isSelected = image.id == selectedImageId
                        AsyncImage(
                            model = image.uri,
                            contentDescription = "Gallery image ${image.id}",
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable {
                                    selectedImageId = image.id
                                    selectedImageUri = image.uri
                                },
                        )
                    }
                }
            } else {
                PermissionMessage(
                    text = "Gallery permission is required to show photos.",
                    onGrant = { permissionsLauncher.launch(requiredPermissions()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    lensFacing: Int,
    onImageCaptureReady: (ImageCapture?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val focusIndicatorSize = 56.dp
    val focusIndicatorSizePx = with(density) { focusIndicatorSize.toPx() }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusPulseKey by remember { mutableIntStateOf(0) }
    val focusAlpha by animateFloatAsState(
        targetValue = if (focusPoint != null) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "focusAlpha",
    )
    val focusScale by animateFloatAsState(
        targetValue = if (focusPoint != null) 1f else 1.24f,
        animationSpec = tween(durationMillis = 180),
        label = "focusScale",
    )
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(focusPulseKey) {
        if (focusPoint != null) {
            delay(700)
            focusPoint = null
        }
    }

    DisposableEffect(lensFacing, lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var providerRef: ProcessCameraProvider? = null
        var cameraRef: Camera? = null

        val bindCamera = Runnable {
            val provider = providerFuture.get()
            providerRef = provider

            val desiredSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            val selector = try {
                if (provider.hasCamera(desiredSelector)) {
                    desiredSelector
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            } catch (_: Exception) {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            provider.unbindAll()
            val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            cameraRef = camera
            onImageCaptureReady(imageCapture)

            val scaleDetector = ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val cameraInfo = camera.cameraInfo
                        val cameraControl = camera.cameraControl
                        val currentZoom = cameraInfo.zoomState.value?.zoomRatio ?: 1f
                        val minZoom = cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                        val maxZoom = cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                        val nextZoom = (currentZoom * detector.scaleFactor).coerceIn(minZoom, maxZoom)
                        cameraControl.setZoomRatio(nextZoom)
                        return true
                    }
                },
            )
            val tapDetector = GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(event: MotionEvent): Boolean {
                        focusPoint = Offset(event.x, event.y)
                        focusPulseKey += 1
                        val meteringPoint = previewView.meteringPointFactory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(
                            meteringPoint,
                            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
                        ).build()
                        camera.cameraControl.startFocusAndMetering(action)
                        return true
                    }
                },
            )

            previewView.setOnTouchListener { _, event ->
                var handled = scaleDetector.onTouchEvent(event)
                handled = tapDetector.onTouchEvent(event) || handled
                handled
            }
        }

        providerFuture.addListener(bindCamera, mainExecutor)
        onDispose {
            previewView.setOnTouchListener(null)
            cameraRef = null
            onImageCaptureReady(null)
            providerRef?.unbindAll()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        val point = focusPoint
        if (point != null) {
            Box(
                modifier = Modifier
                    .size(focusIndicatorSize)
                    .graphicsLayer {
                        translationX = point.x - (focusIndicatorSizePx / 2f)
                        translationY = point.y - (focusIndicatorSizePx / 2f)
                        scaleX = focusScale
                        scaleY = focusScale
                        alpha = focusAlpha
                    }
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                    ),
            )
        }
    }
}

@Composable
private fun PermissionMessage(
    text: String,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 18.dp),
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable(onClick = onGrant)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Grant permission",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSaved: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit,
) {
    val timestamp = System.currentTimeMillis()
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, String.format(Locale.US, "ig_%d.jpg", timestamp))
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/InstagramClone",
            )
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues,
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val uri = outputFileResults.savedUri ?: latestGalleryImageUri(context)
                if (uri != null) {
                    onSaved(uri)
                }
            }
        },
    )
}

private fun latestGalleryImageUri(context: Context): Uri? {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val id = cursor.getLong(idColumn)
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        }
    }
    return null
}

private fun requiredPermissions(): Array<String> {
    val galleryPermissions = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }
    return arrayOf(Manifest.permission.CAMERA, *galleryPermissions)
}

private fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun hasGalleryPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= 34) {
        val imagesGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES,
        ) == PackageManager.PERMISSION_GRANTED
        val selectedGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        ) == PackageManager.PERMISSION_GRANTED
        return imagesGranted || selectedGranted
    }
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun loadGalleryImages(context: Context, limit: Int = 80): List<GalleryImage> {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED,
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    val result = mutableListOf<GalleryImage>()

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder,
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext() && result.size < limit) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            result += GalleryImage(id = id, uri = uri)
        }
    }

    return result
}
