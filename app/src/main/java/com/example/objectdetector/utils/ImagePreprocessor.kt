package com.example.objectdetector.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImagePreprocessor {

    fun decodeImageFile(file: File): Bitmap {
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun preprocessImage(bitmap: Bitmap, inputSize: Int): ByteBuffer {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        scaledBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            byteBuffer.putFloat(((pixel shr 16 and 0xFF) - 127.5f) / 127.5f)
            byteBuffer.putFloat(((pixel shr 8 and 0xFF) - 127.5f) / 127.5f)
            byteBuffer.putFloat(((pixel and 0xFF) - 127.5f) / 127.5f)
        }

        return byteBuffer
    }
}