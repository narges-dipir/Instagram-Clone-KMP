package de.app.instagram.create.presentation.ui

import kotlinx.cinterop.CValue
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGPoint
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIPinchGestureRecognizer
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IOSCameraGestureHandler(
    private val hostView: UIView,
    private val onTap: (CValue<CGPoint>) -> Unit,
    private val onPinchScale: (Double) -> Unit,
) : NSObject() {
    private val tapRecognizer = UITapGestureRecognizer(
        target = this,
        action = NSSelectorFromString("handleTap:"),
    )
    private val pinchRecognizer = UIPinchGestureRecognizer(
        target = this,
        action = NSSelectorFromString("handlePinch:"),
    )

    fun attachToView(view: UIView) {
        view.userInteractionEnabled = true
        view.addGestureRecognizer(tapRecognizer)
        view.addGestureRecognizer(pinchRecognizer)
    }

    @ObjCAction
    fun handleTap(recognizer: UITapGestureRecognizer) {
        onTap(recognizer.locationInView(hostView))
    }

    @ObjCAction
    fun handlePinch(recognizer: UIPinchGestureRecognizer) {
        onPinchScale(recognizer.scale)
        recognizer.scale = 1.0
    }
}
