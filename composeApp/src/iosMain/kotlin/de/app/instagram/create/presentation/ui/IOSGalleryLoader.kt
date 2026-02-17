package de.app.instagram.create.presentation.ui

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHFetchOptions
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class)
fun loadRecentGalleryImages(
    limit: Int = 80,
    thumbnailSize: CValue<CGSize> = CGSizeMake(220.0, 220.0),
): List<IOSGalleryImage> {
    val options = PHFetchOptions().apply {
        setFetchLimit(limit.toULong())
    }
    val fetchResult = PHAsset.fetchAssetsWithMediaType(
        mediaType = PHAssetMediaTypeImage,
        options = options,
    )
    val requestOptions = PHImageRequestOptions().apply {
        setSynchronous(true)
        setDeliveryMode(PHImageRequestOptionsDeliveryModeHighQualityFormat)
    }

    val manager = PHImageManager.defaultManager()
    val result = mutableListOf<IOSGalleryImage>()
    val count = fetchResult.count.toInt()
    for (index in 0 until count) {
        val asset = fetchResult.objectAtIndex(index.toULong()) as? PHAsset ?: continue
        var image: UIImage? = null
        manager.requestImageForAsset(
            asset = asset,
            targetSize = thumbnailSize,
            contentMode = PHImageContentModeAspectFill,
            options = requestOptions,
        ) { uiImage, _ ->
            image = uiImage
        }
        val thumbnail = image ?: continue
        result += IOSGalleryImage(
            id = asset.localIdentifier,
            image = thumbnail,
        )
    }
    return result
}
