package com.angel.fitgains.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ImageUtils {
    private const val MAX_SIDE = 1024

    /**
     * Decodifica la foto (ImageDecoder respeta la orientación EXIF), la reduce a
     * 1024 px en el lado mayor y la comprime a JPEG: menos coste y latencia en la API.
     */
    suspend fun toCompressedJpeg(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = MAX_SIDE.toFloat() / maxOf(w, h)
            if (scale < 1f) decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.toByteArray()
        }
    }
}
