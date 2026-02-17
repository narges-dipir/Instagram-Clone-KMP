package de.app.instagram.create.presentation.ui

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IOSCameraPreviewView(
    private val onLayoutBounds: (CValue<CGRect>) -> Unit,
) : UIView(frame = CGRectZero.readValue()) {

    override fun layoutSubviews() {
        super.layoutSubviews()
        onLayoutBounds(bounds)
    }
}
