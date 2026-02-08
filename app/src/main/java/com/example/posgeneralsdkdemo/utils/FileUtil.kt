package com.example.posgeneralsdkdemo.utils

import android.content.Context
import java.io.File

object FileUtil {


    fun copyAssetToCacheIfNeeded(context: Context, fileName: String): File {
        val outFile = File(context.cacheDir, fileName)
        if (!outFile.exists()) {
            context.assets.open(fileName).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile
    }
}