package com.example.posgeneralsdkdemo.utils

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

object ImageUtil {
    fun image2Bytes(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }
}