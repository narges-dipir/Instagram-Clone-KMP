package de.app.instagram.create.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import kotlinx.coroutines.delay
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import platform.UIKit.UIViewContentMode
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformCreateScreen(
    modifier: Modifier,
) {
    val cameraController = remember { IOSCameraController() }
    val density = LocalDensity.current
    val focusIndicatorSize = 56.dp
    val focusIndicatorSizePx = with(density) { focusIndicatorSize.toPx() }
    var selectedImage by remember { mutableStateOf<UIImage?>(null) }
    var selectedImageId by remember { mutableStateOf<String?>(null) }
    var galleryImages by remember { mutableStateOf(emptyList<IOSGalleryImage>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusPulseKey by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        galleryImages = loadRecentGalleryImages()
        if (selectedImage == null) {
            val first = galleryImages.firstOrNull()
            selectedImage = first?.image
            selectedImageId = first?.id
        }
    }

    LaunchedEffect(focusPulseKey) {
        if (focusPoint != null) {
            delay(700)
            focusPoint = null
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
            val image = selectedImage
            if (image != null) {
                UIKitView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        UIImageView(frame = CGRectZero.readValue()).apply {
                            contentMode = UIViewContentMode.UIViewContentModeScaleAspectFill
                            clipsToBounds = true
                        }
                    },
                    update = { view ->
                        view.image = image
                    },
                )
            } else {
                IOSCameraPreview(
                    controller = cameraController,
                    modifier = Modifier.fillMaxSize(),
                    onError = { errorMessage = it },
                    onFocusPoint = { point ->
                        focusPoint = point.useContents { Offset(x.toFloat(), y.toFloat()) }
                        focusPulseKey += 1
                    },
                )

                val point = focusPoint
                if (point != null) {
                    Box(
                        modifier = Modifier
                            .size(focusIndicatorSize)
                            .graphicsLayer {
                                translationX = point.x - (focusIndicatorSizePx / 2f)
                                translationY = point.y - (focusIndicatorSizePx / 2f)
                            }
                            .border(
                                width = 2.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                            ),
                    )
                }

                IconButton(
                    onClick = {
                        cameraController.switchCamera(
                            onError = { errorMessage = it },
                        )
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
                        .clickable {
                            cameraController.capturePhoto(
                                onCaptured = { captured ->
                                    if (captured == null) {
                                        errorMessage = "Capture failed."
                                        return@capturePhoto
                                    }
                                    selectedImage = captured
                                    selectedImageId = null
                                    errorMessage = null
                                    UIImageWriteToSavedPhotosAlbum(captured, null, null, null)
                                    dispatch_async(dispatch_get_main_queue()) {
                                        galleryImages = loadRecentGalleryImages()
                                    }
                                },
                                onError = { errorMessage = it },
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
            }

            if (selectedImage != null) {
                IconButton(
                    onClick = {
                        selectedImage = null
                        selectedImageId = null
                        errorMessage = null
                    },
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

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = Color(0xFFFF6B6B),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
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

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                items(galleryImages, key = { it.id }) { item ->
                    val isSelected = selectedImageId == item.id
                    UIKitView(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable {
                                selectedImage = item.image
                                selectedImageId = item.id
                                errorMessage = null
                            },
                        factory = {
                            UIImageView(frame = CGRectZero.readValue()).apply {
                                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFill
                                clipsToBounds = true
                            }
                        },
                        update = { view ->
                            view.image = item.image
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun IOSCameraPreview(
    controller: IOSCameraController,
    modifier: Modifier,
    onError: (String) -> Unit,
    onFocusPoint: (kotlinx.cinterop.CValue<platform.CoreGraphics.CGPoint>) -> Unit,
) {
    val previewContainer = remember {
        IOSCameraPreviewView(
            onLayoutBounds = { bounds -> controller.updatePreviewFrame(bounds) },
        )
    }
    DisposableEffect(Unit) {
        previewContainer.backgroundColor = platform.UIKit.UIColor.blackColor
        onDispose { }
    }

    DisposableEffect(controller) {
        controller.setOnFocusPointChanged(onFocusPoint)
        controller.attachPreview(previewContainer)
        controller.start(onError = onError)
        onDispose {
            controller.setOnFocusPointChanged(null)
            controller.stop()
        }
    }

    UIKitView(
        modifier = modifier,
        factory = { previewContainer },
        update = { },
    )
}
