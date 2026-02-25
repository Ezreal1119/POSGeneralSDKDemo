package com.example.posdemo.helpers

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.device.DeviceManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.posdemo.data.AppListResp
import com.example.posdemo.data.UnfinishedOrderResp
import com.example.posdemo.utils.PackageUtil
import com.example.posdemo.utils.PermissionUtil
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class UmsHelper(private val activity: Activity) {

    companion object {
        private const val TAG = "UmsHelper"
        private const val APP_LIST_URL = "https://uhomeov.urovo.com/api/v1/app/list"
        private const val UNFINISHED_ORDER_URL = "https://uhomeov.urovo.com/api/v1/order/unfinish"
    }

    private val client = OkHttpClient()
    private val gson = Gson()
    private lateinit var apkFile: File

    // This is a better practice than using DownloadManager
    // This is using the General way to install APP instead of InstallManagerImpl
    fun installPackage(targetPkg: String) {

        if (!PermissionUtil.ensureCanRequestPackageInstallsAutoToast(activity)) return
            // 1. Format the URL of APP List on APPMarket(UMS) that is associated with this SN
        val appListUrl = "$APP_LIST_URL?dvcid=${DeviceManager().deviceId}"

        // 2. Get the JSON file, then deserialize it.
        val queryRequest = Request.Builder().url(appListUrl).get().build()
        val respObject = client.newCall(queryRequest).execute().use { resp -> // will call resp.close() automatically after this block
            if (!resp.isSuccessful) error("Query HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty body") // This is the JSON file
            return@use gson.fromJson(body, AppListResp::class.java) //
        }

        // 3. Get the targeted APP Object from the deserialized response
        val item = respObject.data?.firstOrNull {
            it.appPackage == targetPkg
        } ?: error("Package not found on UMS: $targetPkg\n(Please upload to APP Market first)")

        fun doDownload() {
            activity.runOnUiThread {
                Toast.makeText(activity, "Downloading $targetPkg...", Toast.LENGTH_SHORT).show()
            }
            // (a). Get the Download URL of that APP
            val appUrl = item.appUrl?.takeIf { it.isNotBlank() }
                ?: error("appUrl empty for $targetPkg") // Error if appUrl is null or empty
            Log.e(TAG, "Found app: ${item.appName}, ver=${item.appVersionName}, url=$appUrl")

            // (b). Download the APK to local cache of this APP
            val downloadRequest = Request.Builder().url(appUrl).get().build()
            val apkFile = client.newCall(downloadRequest).execute().use { resp ->
                if (!resp.isSuccessful) error("Download HTTP ${resp.code}")
                apkFile = File(activity.cacheDir, "target.apk") // "/data/user/0/<package>/cache", might be cleared by OS
                resp.body?.byteStream().use { input -> // Get the body of resp as the Form of ByteStream(input: InputStream). This is standard for downloading from Internet
                    if (input == null) error("Download stream error") // If inputStream is null, means issue occurs during Downloading.
                    FileOutputStream(apkFile).use { output -> // This is the standard way to download something to a specific space.
                        input.copyTo(output)
                    }
                }
                return@use apkFile
            }
            Log.e(TAG, "Downloaded apk: ${apkFile.absolutePath} (${apkFile.length()} bytes)")
        }

        fun doInstall() {
            activity.runOnUiThread {
                Toast.makeText(activity, "Installing $targetPkg...", Toast.LENGTH_SHORT).show()
            }
            // Install the APP using Intent (Traditional way)
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apkFile)
            val intent = Intent(ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(intent)
        }

        // 4. Check if it's a new Installation or an update, then doInstall() accordingly.
        val info = PackageUtil.getInstalledAppInfoOrNull(activity, targetPkg)
        if (info != null) {
            val message = buildString {
                append("This App has already been installed!\n")
                append(" - Current Version: ${info.versionName}\n")
                append(" - Target Version: ${item.appVersionName}\n")
                append("Are you sure to update the APP?")
            }
            val isUpgrade = (item.appVersionCode?.toLong() ?: -1) >= info.versionCode
            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setTitle("Confirm")
                    .setMessage(message)
                    .setPositiveButton("Yes") { _, _ ->
                        if (!isUpgrade) {
                            Toast.makeText(activity, "This is a downgrade not upgrade!", Toast.LENGTH_SHORT).show()
                        } else {
                            Thread {
                                doDownload()
                                doInstall()
                            }.start()
                        }
                    }.setNegativeButton("Cancel", null)
                    .show()
            }
        } else {
            doDownload()
            doInstall()
        }
    }

    fun getUnfinishedOrder(): String? {
        val url = "$UNFINISHED_ORDER_URL?dvcid=${DeviceManager().deviceId}"
        val request = Request.Builder().url(url).get().build()
        val obj = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty body")
            return@use gson.fromJson(body, UnfinishedOrderResp::class.java)
        }
        if (obj.data?.isEmpty() ?: true) {
            return "No unfinished order for this device now"
        }
        return buildString {
            var index = 1
            append("Unfinished Order: \n")
            for (order in obj.data) {
                append("$index. orderType: ${order.orderType}\n")
                append(" - orderContent: ${order.orderContent}\n")
                append(" - orderTime: ${order.startTime}\n")
                index++
            }
        }
    }

}