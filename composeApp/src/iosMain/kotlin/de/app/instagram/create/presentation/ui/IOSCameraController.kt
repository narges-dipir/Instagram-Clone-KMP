package de.app.instagram.create.presentation.ui

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePosition
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIImage
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIView
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
class IOSCameraController {
    private val session = AVCaptureSession()
    private val photoOutput = AVCapturePhotoOutput()
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)
    private var currentPosition: AVCaptureDevicePosition = AVCaptureDevicePositionBack
    private var currentDevice: AVCaptureDevice? = null
    private var previewHostView: UIView? = null
    private var gestureHandler: IOSCameraGestureHandler? = null
    private var onFocusPointChanged: ((CValue<CGPoint>) -> Unit)? = null
    private var zoomFactor: Double = 1.0
    private var configured = false
    private val sessionQueue = NSOperationQueue().apply {
        maxConcurrentOperationCount = 1
    }

    fun attachPreview(view: UIView) {
        previewHostView = view
        if (previewLayer.superlayer == null) {
            view.layer.addSublayer(previewLayer)
        }
        if (gestureHandler == null) {
            gestureHandler = IOSCameraGestureHandler(
                hostView = view,
                onTap = { point -> focusAtPoint(point) },
                onPinchScale = { scale -> adjustZoom(scale) },
            ).also { it.attachToView(view) }
        }
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        previewLayer.frame = view.bounds
    }

    fun updatePreviewFrame(bounds: CValue<CGRect>) {
        previewLayer.frame = bounds
    }

    fun setOnFocusPointChanged(listener: ((CValue<CGPoint>) -> Unit)?) {
        onFocusPointChanged = listener
    }

    fun start(onError: (String) -> Unit) {
        sessionQueue.addOperationWithBlock {
            if (!configured) {
                configured = configureSession(onError)
            }
            if (!configured) return@addOperationWithBlock
            if (!session.running) {
                session.startRunning()
                if (!session.running) {
                    configured = false
                    dispatch_async(dispatch_get_main_queue()) {
                        onError("Camera failed to start. Check permissions and close other camera apps.")
                    }
                }
            }
        }
    }

    fun stop() {
        sessionQueue.addOperationWithBlock {
            if (session.running) {
                session.stopRunning()
            }
        }
    }

    fun switchCamera(onError: (String) -> Unit) {
        sessionQueue.addOperationWithBlock {
            currentPosition = if (currentPosition == AVCaptureDevicePositionBack) {
                AVCaptureDevicePositionFront
            } else {
                AVCaptureDevicePositionBack
            }
            configured = configureSession(onError)
            if (configured && !session.running) {
                session.startRunning()
            }
        }
    }

    fun capturePhoto(
        onCaptured: (UIImage?) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!configured) {
            onError("Camera is not ready.")
            return
        }
        val host = previewHostView
        if (host == null) {
            onError("Preview is not attached.")
            return
        }
        val size = host.bounds.useContents { this.size }
        val sizeValue = CGSizeMake(size.width, size.height)
        UIGraphicsBeginImageContextWithOptions(sizeValue, false, 0.0)
        host.drawViewHierarchyInRect(host.bounds, true)
        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        onCaptured(image)
    }

    private fun configureSession(onError: (String) -> Unit): Boolean {
        if (session.running) {
            session.stopRunning()
        }
        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPresetPhoto
        session.inputs.forEach { input ->
            session.removeInput(input as AVCaptureDeviceInput)
        }
        session.outputs.forEach { output ->
            session.removeOutput(output as AVCaptureOutput)
        }

        val device = cameraDeviceForPosition(currentPosition)
            ?: cameraDeviceForPosition(AVCaptureDevicePositionBack)
            ?: AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (device == null) {
            session.commitConfiguration()
            dispatch_async(dispatch_get_main_queue()) {
                onError("No camera device available.")
            }
            return false
        }

        val input = AVCaptureDeviceInput.deviceInputWithDevice(
            device = device,
            error = null,
        )

        if (input == null || !session.canAddInput(input)) {
            session.commitConfiguration()
            dispatch_async(dispatch_get_main_queue()) {
                onError("Could not initialize camera input.")
            }
            return false
        }
        session.addInput(input)
        if (session.canAddOutput(photoOutput)) {
            session.addOutput(photoOutput)
        } else {
            session.commitConfiguration()
            dispatch_async(dispatch_get_main_queue()) {
                onError("Could not initialize camera output.")
            }
            return false
        }
        currentDevice = device
        session.commitConfiguration()
        return true
    }

    private fun focusAtPoint(point: CValue<CGPoint>) {
        val device = currentDevice ?: return
        if (!device.lockForConfiguration(null)) return
        onFocusPointChanged?.invoke(point)
        device.unlockForConfiguration()
    }

    private fun adjustZoom(scaleDelta: Double) {
        val device = currentDevice ?: return
        if (!device.lockForConfiguration(null)) return
        val minZoom = 1.0
        val maxZoom = 4.0
        zoomFactor = (zoomFactor * scaleDelta).coerceIn(minZoom, maxZoom)
        previewLayer.setAffineTransform(CGAffineTransformMakeScale(zoomFactor, zoomFactor))
        device.unlockForConfiguration()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun cameraDeviceForPosition(position: AVCaptureDevicePosition): AVCaptureDevice? {
    val discovery = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
        deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
        mediaType = AVMediaTypeVideo,
        position = position,
    )
    val matching = discovery.devices.firstOrNull() as? AVCaptureDevice
    if (matching != null) return matching

    val allDevices = AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo)
    return allDevices
        .mapNotNull { it as? AVCaptureDevice }
        .firstOrNull()
}
