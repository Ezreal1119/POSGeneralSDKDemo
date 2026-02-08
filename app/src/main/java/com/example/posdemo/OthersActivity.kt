package com.example.posdemo

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.device.DeviceManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.posdemo.others.DeviceInfoActivity
import com.example.posdemo.others.InstallManager
import com.example.posdemo.others.KioskRelatedActivity
import com.example.posdemo.others.SwitchesActivity
import com.urovo.sdk.utils.SystemProperties.getSystemProperty
import java.io.File
import androidx.core.net.toUri
import com.example.posdemo.databinding.ActivityOthersBinding
import com.example.posdemo.others.NewSerialPortActivity
import com.example.posdemo.others.SettingsActivity
import com.example.posdemo.others.WifiActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val ACTION_LOAD_GMS = "com.osupdate.upgraderom"
private const val FILE_PATH = "filePath"
private const val FILE_NAME= "gms_urovo_patrick.zip"
class OthersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOthersBinding

    private var url: String = ""

    private val downloadReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 3. Send Broadcast to initial the GMS firmware installation
            val firmware = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
            if (!firmware.isFile()) {
                Toast.makeText(this@OthersActivity, "Firmware not exists", Toast.LENGTH_SHORT).show()
                return
            }
            AlertDialog.Builder(this@OthersActivity)
                .setTitle("Confirm")
                .setMessage("Are you sure you want to load GMS?\nThis will override current UFS!")
                .setPositiveButton("Confirm") { _, _ ->
                    val loadGmsIntent = Intent().apply {
                        action = ACTION_LOAD_GMS
                        flags = FLAG_ACTIVITY_NEW_TASK
                        setClassName("com.ubx.update", "com.ubx.update.receivers.UpdateCheckReceiver")
                        putExtra(FILE_PATH, "/sdcard/Download/$FILE_NAME")
                    }
                    sendBroadcast(loadGmsIntent)
                }
                .setNegativeButton("Cancel", null)
                .show()
            binding.btnLoadGMS.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOthersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGetDeviceInfo.setOnClickListener { startActivity(Intent(this, DeviceInfoActivity::class.java)) }
        binding.btnInstallManager.setOnClickListener { startActivity(Intent(this, InstallManager::class.java)) }
        binding.btnKioskRelated.setOnClickListener { startActivity(Intent(this, KioskRelatedActivity::class.java)) }
        binding.btnWifi.setOnClickListener { startActivity(Intent(this, WifiActivity::class.java)) }
        binding.btnSwitches.setOnClickListener { startActivity(Intent(this, SwitchesActivity::class.java)) }
        binding.btnNewSerialPort.setOnClickListener { startActivity(Intent(this, NewSerialPortActivity::class.java)) }
        binding.btnFactoryMenu.setOnClickListener { onFactoryMenuButtonClicked() }
        binding.btnOtherSettings.setOnClickListener { startActivity((Intent(this, SettingsActivity::class.java))) }
        binding.btnLoadGMS.setOnClickListener { onLoadGMSButtonClicked() }
        binding.btnDebuglogger.setOnClickListener { onDebugloggerButtonClicked() }
        binding.btnUploadLog.setOnClickListener { onUploadLogButtonClicked() }
        binding.btnShutDown.setOnClickListener { onShutDownButtonClicked() }
        binding.btnReboot.setOnClickListener { onRebootButtonClicked() }
        binding.btnReset.setOnClickListener { onResetButtonClicked() }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        for (fw in GmsFirmware.entries) {
            if (getDevType() == fw.toString()) {
                url = fw.url
                break
            }
        }
        if ("GMS" in DeviceManager().getSettingProperty("ro.ufs.custom") || url.isEmpty()) {
            binding.btnLoadGMS.isEnabled = false
        }
        binding.btnUploadLog.isEnabled = File("/sdcard/debuglogger").isDirectory
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(downloadReceiver)
    }

    private fun onLoadGMSButtonClicked() {
        // 1. Check if have Internet
        if (getNetworkType() == null) {
            Toast.makeText(this, "Please connect to Internet first", Toast.LENGTH_SHORT).show()
            return
        }
        // 2. Download the GMS firmware from the Internet
        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle("Downloading GMS firmware")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
        }
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        binding.btnLoadGMS.isEnabled = false
    }


    private fun onFactoryMenuButtonClicked() {
        val intent = Intent().apply {
            component = ComponentName(
                "com.ubx.factorykit",
                "com.ubx.factorykit.Framework.Framework"
            )
            flags = FLAG_ACTIVITY_NEW_TASK
        }
        runCatching {
            startActivity(intent)
        }.onFailure {
            Toast.makeText(this, "Start Factory Menu failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onDebugloggerButtonClicked() {
        val intent = Intent().apply {
            component = ComponentName(
                "com.debug.loggerui",
                "com.debug.loggerui.MainActivity"
            )
            flags = FLAG_ACTIVITY_NEW_TASK
        }
        runCatching {
            startActivity(intent)
        }.onFailure {
            Toast.makeText(this, "Start Debuglogger failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onUploadLogButtonClicked() {
        if (!ensureAllFilesAccess(this)) {
            Toast.makeText(this, "Please grant permission to access all files first", Toast.LENGTH_SHORT).show()
            return
        }
        if (getNetworkType() == null) {
            Toast.makeText(this, "Please connect to Internet first", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this@OthersActivity)
            .setTitle("Confirm")
            .setMessage("Are you sure you want to Upload the log?")
            .setPositiveButton("Confirm") { _, _ ->
                binding.btnUploadLog.isEnabled = false
                val srcDir = File("/sdcard/debuglogger")
                val zipFile = File(getExternalFilesDir(null), "debuglogger.zip")
                runCatching {
                    zipFolder(srcDir, zipFile)
                }.onFailure {
                    it.printStackTrace()
                    binding.btnUploadLog.isEnabled = true
                    return@setPositiveButton
                }
                runCatching {
                    uploadLog(
                        serverUrl = "https://logs.patrick-shenzhen.org",
                        logZip = zipFile,
                        sn = DeviceManager().deviceId
                    )
                }.onSuccess {
                    Toast.makeText(this, "Start uploading", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    it.printStackTrace()
                    binding.btnUploadLog.isEnabled = true
                    return@setPositiveButton
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onShutDownButtonClicked() {
        AlertDialog.Builder(this@OthersActivity)
            .setTitle("Confirm")
            .setMessage("Are you sure you want to Shutdown the device?")
            .setPositiveButton("Confirm") { _, _ ->
                DeviceManager().shutdown(false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun onRebootButtonClicked() {
        runCatching {
            AlertDialog.Builder(this@OthersActivity)
                .setTitle("Confirm")
                .setMessage("Are you sure you want to Reboot the device?")
                .setPositiveButton("Confirm") { _, _ ->
                    DeviceManager().shutdown(true)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onResetButtonClicked() {
        runCatching {
            AlertDialog.Builder(this@OthersActivity)
                .setTitle("Confirm")
                .setMessage("Are you sure to Reset/Wipe the device?")
                .setPositiveButton("Confirm") { _, _ ->
                    DeviceManager().wipeData()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }.onFailure {
            it.printStackTrace()
        }
    }




    // <---------------Helper functions--------------->

    private fun getDevType(): String {
        return getSystemProperty("pwv.project", "no result found!")
    }

    private fun getNetworkType(): String? {
        val activeNetworkInfo = (this
            .getSystemService("connectivity") as ConnectivityManager).activeNetworkInfo
        if (activeNetworkInfo == null) {
            return null
        }
        return if (activeNetworkInfo.type == 1) "Wifi" else "4G"
    }


    private fun ensureAllFilesAccess(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        if (Environment.isExternalStorageManager()) return true

        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = "package:${activity.packageName}".toUri()
        }
        activity.startActivity(intent)
        return false
    }
    private fun zipFolder(srcDir: File, outZip: File) {
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

    private fun uploadLog(serverUrl: String, logZip: File, sn: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val fileBody = logZip.asRequestBody("application/zip".toMediaType())
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("sn", sn)
            .addFormDataPart(
                "file",
                logZip.name,
                fileBody
            )
            .build()
        val request = Request.Builder()
            .url("$serverUrl/upload")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@OthersActivity, "Upload failed", Toast.LENGTH_SHORT).show()
                    binding.btnUploadLog.isEnabled = true
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val ok = response.isSuccessful
                val code = response.code
                response.close()
                runOnUiThread {
                    if (ok) {
                        Toast.makeText(this@OthersActivity, "Upload success", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@OthersActivity, "Upload failed: $code", Toast.LENGTH_SHORT).show()
                    }
                    binding.btnUploadLog.isEnabled = true
                }
            }
        })
    }
}

enum class GmsFirmware(val url: String) {
    SQ68("https://ota-oss.urovo.com/https_ota_prod/Android/SQ68/DS/PKG-XX/english/14.25.0624.01/19177/patch.zip"),
    SQ29MB("https://ota-oss.urovo.com/https_ota_prod/Android/SQ29MB/DS/PKG-XX/english/10.25.0101.01/19183/patch.zip")
}

