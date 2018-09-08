package com.pinnacleimagingsystems.ambientviewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.support.media.ExifInterface

private fun Bitmap.asRotated(rotation: Int) = when(rotation) {
    0 -> this
    else -> {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}

fun loadBitmap(file: String): Bitmap {
    val exif = ExifInterface(file)
    val rotationDegrees = exif.rotationDegrees
    val bitmap = BitmapFactory.decodeFile(file)

    return bitmap.asRotated(rotationDegrees)
}

fun loadThumbnailBitmap(file: String): Bitmap? {
    val exif = ExifInterface(file)
    val rotationDegrees = exif.rotationDegrees
    val bitmap = exif.thumbnailBitmap ?: return null

    return bitmap.asRotated(rotationDegrees)
}

