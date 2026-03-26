package com.example.posdemo.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

    fun zipFolder(srcDir: File, outZip: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outZip))).use { zos ->
            srcDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val entryName = file.relativeTo(srcDir).path.replace("\\", "/")
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
        }
    }

    fun uploadZipFile(
        url: String,
        zipFile: File
    ): String {
        val client = OkHttpClient()

        val requestBody = zipFile.asRequestBody("application/zip".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Upload failed: ${response.code}")
            }
            return response.body?.string().orEmpty()
        }
    }

    fun uploadZipFileWithProgress(
        url: String,
        zipFile: File,
    ): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val requestBody = object : RequestBody() {
            override fun contentType(): MediaType? = null

            override fun contentLength(): Long = zipFile.length()

            override fun writeTo(sink: BufferedSink) {
                var sent = 0L
                val buffer = ByteArray(10 * 1024)

                FileInputStream(zipFile).use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        sink.write(buffer, 0, read)
                        sent += read
                    }
                }
            }
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()

            Log.e("Upload", "code=${response.code}")
            Log.e("Upload", "message=${response.message}")
            Log.e("Upload", "headers=${response.headers}")
            Log.e("Upload", "body=[$body]")

            if (response.code != 200) {
                throw IOException("Upload failed, code=${response.code}, body=$body")
            }

            return body
        }
    }

}

private class ProgressRequestBody(
    private val file: File,
    private val contentType: MediaType?,
    private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = file.length()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploaded = 0L

        FileInputStream(file).use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                sink.write(buffer, 0, read)
                uploaded += read
                onProgress(uploaded, total)
            }
        }
    }
}