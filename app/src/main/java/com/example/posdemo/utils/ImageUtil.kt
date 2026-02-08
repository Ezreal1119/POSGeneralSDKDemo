package com.example.posdemo.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import androidx.core.graphics.set

private const val MAX_PAGE_WIDTH = 384 // 58mm(2 inch): 48mm * 8dot/mm = 384
object ImageUtil {
    fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    fun scaleBitmap(bitmap: Bitmap, maxWidth: Int = MAX_PAGE_WIDTH): Bitmap {
        // 1. Create Scaled Bitmap
        val scaleRatio = maxWidth / bitmap.width.toFloat() // MAX_PAGE_WIDTH means scale to max size; Can adjust
        val newWidth = maxWidth
        val newHeight = (bitmap.height * scaleRatio).toInt()
        val scaledBitmap = bitmap.scale(newWidth, newHeight, true)

        // 2. Optimize the Print effect
        val output = createBitmap(newWidth, newHeight)
        val canvas = Canvas(output).apply {
            drawColor(android.graphics.Color.WHITE)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
        canvas.drawBitmap(scaledBitmap, 0F, 0F, paint)
        return output
    }

    fun textToBitmap(
        lines: List<String>,
        textSizePx: Int,// The size of the Text in Pixel(Dot)
        paddingPx: Int, // The size of the padding(left/right/top/bottom) of the whole Bitmap
        lineGapPx: Int // Addition Gap between each line of the bitmap
    ): Bitmap {
        // 1. Create a Painter that supports ANTI_ALIAS
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK // Color of the Text
            textSize = textSizePx.toFloat()
            typeface = Typeface.MONOSPACE // For better alignment
            isDither = true // For better print effect
            isFilterBitmap = true // For better print effect
        }
        // 2. Create a Empty Bitmap using MAX_PAGE_WIDTH & Calculated TotalHeight
        val fontMetrics = paint.fontMetrics
        val lineHeight = (fontMetrics.bottom - fontMetrics.top) // Calculate the height of each line; top of Text is negative(e.g. -22px), bottom of Text is positive(e.g. 6px)_
        val totalHeight = (paddingPx * 2 + lines.size * (lineHeight + lineGapPx)).toInt()
        val bitmap = createBitmap(MAX_PAGE_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        // 3. Bind a Canvas(Drawing Tool) to the Bitmap (White Background)
        val canvas = Canvas(bitmap).apply {
            drawColor(Color.WHITE)
        }
        // 4. Draw the Bitmap using Canvas line by line
        var baselineY = paddingPx - fontMetrics.top // e.g. paddingPx is 12 Px, top is -22(means 22 Px from the baseline); (paddingPx - fontMetrics.top) means the distance between the very top of the Canvas and the baseline
        for (line in lines) {
            canvas.drawText(line, paddingPx.toFloat(), baselineY, paint)
            baselineY += lineHeight + lineGapPx // This is to Draw from next line each time
        }
        return bitmap
    }

    fun pngToBitmap(res: Resources, id: Int): Bitmap {
        return BitmapFactory.decodeResource(res, id)
    }

    fun stringToQrBitmap(content: String, size: Int): Bitmap { // Less then 384
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size) // Create QR Matrix
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888) // Create Bitmap
        for (y in 0 until size) { // Draw the Bitmap
            for (x in 0 until size) {
                bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return bitmap
    }
}