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
import com.example.posdemo.data.ConfigResp
import com.example.posdemo.data.QueryLogRecordResp
import com.example.posdemo.data.UnfinishedOrderResp
import com.example.posdemo.utils.PackageUtil
import com.example.posdemo.utils.PermissionUtil
import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class UmsHelper(private val activity: Activity) {

    companion object {
        private const val TAG = "UmsHelper"
        private const val APP_LIST_URL = "https://uhomeov.urovo.com/api/v1/app/list"
        private const val UNFINISHED_ORDER_URL = "https://uhomeov.urovo.com/api/v1/order/unfinish"
        private const val UPLOAD_RESULT_URL = "https://uhomeov.urovo.com/api/v1/monitor/uploadGeneral"
        private const val CONFIG_URL = "https://uhomeov.urovo.com/api/v2/get/all/config"
        private const val QUERY_DEVICE_LOG_RECORD = "https://uhomeov.urovo.com/api/v1/query/device/log/record"
    }

    private val client = OkHttpClient()
    private val gson = Gson()
    private lateinit var apkFile: File
    var sn = ""

    fun getAppMarketList(): String {
        val url = "$APP_LIST_URL?dvcid=$sn"
        val request = Request.Builder().url(url).get().build()
        val obj = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty body")
            return@use gson.fromJson(body, AppListResp::class.java)
        }
        if (obj.data?.isEmpty() ?: true) {
            return "No App on the APP Market\n"
        }
        return buildString {
            var index = 1
            append("App List:\n")
            for (app in obj.data) {
                append("(${index}). PackageName: ${app.appPackage}\n")
                append(" - versionName: ${app.appVersionName}\n")
                index++
            }
        }
    }

    // This is a better practice than using DownloadManager
    // This is using the General way to install APP instead of InstallManagerImpl
    fun installPackage(targetPkg: String) {

        if (!PermissionUtil.ensureCanRequestPackageInstallsAutoToast(activity)) return
            // 1. Format the URL of APP List on APPMarket(UMS) that is associated with this SN
        val appListUrl = "$APP_LIST_URL?dvcid=$sn"

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

    fun getUnfinishedOrder(): String {
        val url = "$UNFINISHED_ORDER_URL?dvcid=$sn"
        val request = Request.Builder().url(url).get().build()
        val obj = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty body")
            return@use gson.fromJson(body, UnfinishedOrderResp::class.java)
        }
        if (obj.data?.isEmpty() ?: true) {
            return "No unfinished order for this device now\n"
        }
        return buildString {
            var index = 1
            append("Unfinished Order: \n")
            for (order in obj.data) {
                append("($index). orderType: ${parseOrderType(order.orderType.toIntOrNull() ?: -1)}\n")
                append(" - orderContent: ${order.orderContent}\n")
                append(" - orderTime: ${order.startTime}\n")
                index++
            }
        }
    }

    fun getConfig(): String {
        val url = "$CONFIG_URL?dvcid=$sn"
        val request = Request.Builder().url(url).get().build()
        val obj = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty body")
            return@use gson.fromJson(body, ConfigResp::class.java)
        }
        if (obj.data?.isEmpty() ?: true) {
            return "No Config for this device now\n"
        }
        return buildString {
            var index = 1
            append("Device Config:\n")
            for (config in obj.data) {
                append("($index). configType: ${config.configType}\n")
                append(" - pushTime: ${config.pushTime}\n")
                index++
            }
        }
    }

    fun queryDeviceLogRecord(): String? {
        val formBody = FormBody.Builder()
            .add("dvcid", sn)
            .add("dvcType", DeviceManager().getSettingProperty("ro.product.vendor.model"))
            .build()
        val request = Request.Builder()
            .url(QUERY_DEVICE_LOG_RECORD)
            .post(formBody)
            .build()
        val responseObj = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty Body")
            return@use gson.fromJson(body, QueryLogRecordResp::class.java)
        }
        if (responseObj.data?.isEmpty() ?: true) {
            return "No Log Upload Task for this device now\n"
        }
        return buildString {
            var index = 1
            for (logTask in responseObj.data) {
                append("(${index}). logType: ${parseLogType(logTask.logType)}\n")
                append(" - Content: ${logTask.logTaskContent}\n")
                append(" - CreateTime: ${logTask.createTime}\n")
                index++
            }
        }
    }

    // <---------------- Helper methods ----------------> //
    fun parseLogType(type: Int): String {
        return when (type) {
            1 -> "UMS Log"
            2 -> "System Log"
            3 -> "File Upload"
            else -> "Unknown"
        }
    }

    fun parseOrderType(type: Int): String {
        return when (type) {
            0 -> "Freeze the Device: startActivity(FREEZE_PAGE) + KIOSK"
            1 -> "Unfreeze the Device: Cancel KIOSK"
            4 -> "File Distribution: Download(OSS)"
            5 -> "Shutdown: DeviceManager().shutdown(false)"
            6 -> "Restart: DeviceManager().shutdown(true)"
            7 -> "Clear Unlock Password: DeviceManager().clearLock()"
            8 -> "Uninstall APP: DeviceManager().uninstallApplication()"
            15 -> "Extract System Log: HTTP POST"
            17 -> "Reset: DeviceManager().wipeData()"
            61 -> "Ring Device: UMS.ring()"
            63 -> "Push Message: UMS.sendBroadcast(a=com.urovo.message)"
            else -> "Unknown"
        }
    }

}

/*
Further:
Won't trigger when save but must have:
 - device_fence_config: Will trigger on Backend
 - device_config(Default: e.g. MQTT_PWD, PollingTime)

Will examine if Config is right when polling:
 - silent_app_config: DM.setAutoRunningApp()
 - function_config: ?????
 - wifi_whitelist_config: DM.insertToWifiWhiteList()/removeFromWifiWhiteList()
 - occupy_screen_app_config: DM.setLockTaskMode() + DM.saveLockPassword(PWD)
 - desktop_config: DM.setDefaultLauncher()/DM.removeDefaultLauncher()

Won't examine if Config Code is the same when polling:
 - wifi_config: DM.connectWifi()
 - apn_config: DM.setAPN()/deleteAPN()
 - send_script_config: Intent(Act, Serv, Broadcast)
 - app_whitelist_config: DM.setAllowInstallApps()
 - app_deploy_config: Download(OSS) + DM.installApplication()
 - boot_animation_config: Download(OSS) + ???
 - strategy_config: UMS.startActivity(UStage, Config) + UStage.sendBroadcast/AIDL

Note:
 - Will poll periodically(Only Config+UnfinishedOrder)
 - Will poll thoroughly(Config+UnfinishedOrder+ReportInfo) when UMS first Installed or Reboot
 - MoveGroup/DistributeSubOrg will clear all the Orders
 */
