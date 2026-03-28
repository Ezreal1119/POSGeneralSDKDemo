package com.example.posdemo.others

import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.device.DeviceManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.posdemo.databinding.ActivityLogBinding
import com.example.posdemo.utils.DeviceInfoUtil
import com.example.posdemo.utils.FileUtil
import com.example.posdemo.utils.PackageUtil
import com.example.posdemo.utils.PermissionUtil
import com.example.posdemo.utils.TimeUtil
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class LogActivity : AppCompatActivity() {

    companion object {
        private const val URL = "http://yun.urovo.com:8001/US/uploadLog"
        private val LOG_TYPES = arrayOf(
            "System Log",
            "DebugLogger Log",
            "EMV Log",
            "UMS Log",
            "UMS AppMarket Log",
            "Custom File Path"
        )

        private val LOG_FILE_PATHS = arrayOf(
            "/sdcard/ULog/logs/adb",
            "/sdcard/debuglogger",
            "/sdcard/UROPE",
            "/sdcard/UHome/",
            "/sdcard/UhomeAppmarket/",
            "/sdcard/"
        )
    }

    private lateinit var binding: ActivityLogBinding

    private var isMtkDevice = true

    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isMtkDevice = PackageUtil.isPackageInstalled(this, "com.debug.loggerui")

        binding.tvSelectedLogType.text = LOG_TYPES[0]
        binding.etFilePath.setText(LOG_FILE_PATHS[0])

        binding.ivDebugLogger.setOnClickListener { onDebugLoggerImageClicked() }
        binding.cardLogTypePicker.setOnClickListener { onLogTypePickerCardClicked() }
        binding.btnRecord.setOnClickListener { onRecordButtonClicked() }
        binding.btnUpload.setOnClickListener { onUploadButtonClicked() }

    }

    override fun onStart() {
        super.onStart()
        if (!PermissionUtil.ensureAllFilesAccess(this)) {
            Toast.makeText(this, "Please grant permission to access all files first", Toast.LENGTH_SHORT).show()
            return
        }
        if (DeviceInfoUtil.getNetworkType(this) == null) {
            Toast.makeText(this, "Please connect to Internet first", Toast.LENGTH_SHORT).show()
            return
        }
    }

    private fun onDebugLoggerImageClicked() {
        val intent = Intent().apply {
            component = if (isMtkDevice) {
                ComponentName(
                    "com.debug.loggerui",
                    "com.debug.loggerui.MainActivity"
                )
            } else {
                ComponentName(
                    "com.un.logredirect",
                    "com.un.logredirect.LogRedirectorSettings"
                )
            }
            flags = FLAG_ACTIVITY_NEW_TASK
        }
        runCatching {
            startActivity(intent)
        }.onFailure {
            Toast.makeText(this, "Start Debuglogger failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onLogTypePickerCardClicked() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Log Type")
            .setItems(LOG_TYPES) { _, which ->
                binding.tvSelectedLogType.text = LOG_TYPES[which]

                binding.etFilePath.setText(LOG_FILE_PATHS[which])
                if (!isMtkDevice && which == 1) {
                    binding.etFilePath.setText("/sdcard/log")
                }

                if (LOG_TYPES[which] == LOG_TYPES[6]) {
                    binding.etFilePath.isEnabled = true
                } else {
                    binding.etFilePath.isEnabled = false
                }
            }
            .show()
    }

    private fun onRecordButtonClicked() {
        isRecording = !isRecording

        if (isRecording) {
            startRecordingUi()
            if (isMtkDevice) {
                val intent = Intent("action.LOG_CONTROL_SERVICE").apply {
                    putExtra("option", 1)
                    putExtra("android", true)
                    putExtra("kernel", false)
                    putExtra("androidFile", "SystemLog_${DateFormat.format("yyyy-MM-dd_HH_mm_ss", Date().time)}")
                    putExtra("fileMaxSize", 10)
                }
                sendBroadcast(intent)
            } else {
                val intent = Intent("android.intent.action.UNER_START_LOG").apply {
                    setClassName(
                        "com.un.logredirect",
                        "com.un.logredirect.LogRedirectorReceiver"
                    )
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    putExtra("isStart", true)
                    putExtra("maxVolume", 10)
                }
                sendBroadcast(intent)
            }
        } else {
            stopRecordingUi()
            if (isMtkDevice) {
                val intent = Intent("action.LOG_CONTROL_SERVICE").apply {
                    putExtra("option", 0)
                    putExtra("android", true)
                    putExtra("kernel", false)
                }
                sendBroadcast(intent)
            } else {
                val intent = Intent("android.intent.action.UNER_START_LOG").apply {
                    setClassName(
                        "com.un.logredirect",
                        "com.un.logredirect.LogRedirectorReceiver"
                    )
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    putExtra("isStart", false)
                }
                sendBroadcast(intent)
            }
        }
    }


    private fun onUploadButtonClicked() {
        if (!PermissionUtil.ensureAllFilesAccess(this)) {
            Toast.makeText(this, "Please grant permission to access all files first", Toast.LENGTH_SHORT).show()
            return
        }
        if (DeviceInfoUtil.getNetworkType(this) == null) {
            Toast.makeText(this, "Please connect to Internet first", Toast.LENGTH_SHORT).show()
            return
        }
        if (!File(binding.etFilePath.text.toString()).isDirectory) {
            Toast.makeText(this, "Directory not Exists!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Are you sure you want to Upload the log?")
            .setPositiveButton("Confirm") { _, _ ->
                binding.btnUpload.alpha = 0f
                binding.btnUpload.isEnabled = false
                binding.layoutUploadLoading.visibility = View.VISIBLE
                binding.layoutUploadLoading.alpha = 0.5F
                val srcDir = File(binding.etFilePath.text.toString())
                val zipFile = File(getExternalFilesDir(null), "logs.zip")
                lifecycleScope.launch {
                    try {
                        binding.tvUploading.text = "Uploading... 0%"
                        withContext(Dispatchers.IO) {
                            val deviceInfoText = buildDeviceInfoText()
                            writeDeviceInfoFile(srcDir, deviceInfoText)
                            FileUtil.zipFolder(srcDir, zipFile)
                            uploadByChunksReal(
                                url = URL,
                                zipFile = zipFile,
                                onProgress = { sent, total ->
                                    val progress = (sent * 100 / total).toInt()

                                    runOnUiThread {
                                        binding.tvUploading.text = "Uploading... $progress%"
                                    }
                                }
                            )
                            runOnUiThread { Toast.makeText(this@LogActivity, "Upload Success", Toast.LENGTH_SHORT).show() }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@LogActivity, e.message, Toast.LENGTH_SHORT).show()
                    } finally {
                        binding.btnUpload.alpha = 1f
                        binding.btnUpload.isEnabled = true
                        binding.layoutUploadLoading.visibility = View.GONE
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // <---------------Helper methods---------------> //

    private fun uploadByChunksReal(
        url: String,
        zipFile: File,
        onProgress: (Long, Long) -> Unit
    ) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val fileLen = zipFile.length()
        val writeDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val deviceId = DeviceManager().deviceId + "_${binding.tvSelectedLogType.text.toString().replace(" ", "_")}"

        val fileName = "${binding.tvSelectedLogType.text.toString().replace(" ", "_")}_${TimeUtil.getCurrentTimeStr()}_Patrick.zip"

        val fileID = UUID.randomUUID().toString()
        var startLen = 0L
        val buffer = ByteArray(10240)

        FileInputStream(zipFile).use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break

                val requestBody = object : RequestBody() {
                    override fun contentType(): MediaType? =
                        "application/x-www-form-urlencoded".toMediaType()

                    override fun contentLength(): Long = read.toLong()

                    override fun writeTo(sink: BufferedSink) {
                        sink.write(buffer, 0, read)
                    }
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("fileLen", fileLen.toString())
                    .addHeader("fileName", fileName)
                    .addHeader("dvcId", deviceId)
                    .addHeader("commandCode", "")
                    .addHeader("writeDate", writeDate)
                    .addHeader("fileID", fileID)
                    .addHeader("startLen", startLen.toString())
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    Log.e(
                        "Upload",
                        "chunk start=$startLen read=$read code=${response.code} body=[$body]"
                    )
                    if (response.code != 200) {
                        throw IOException("Chunk failed: ${response.code}, body=$body")
                    }
                }

                startLen += read

                onProgress(startLen, fileLen)
            }
        }
    }

    private fun writeDeviceInfoFile(srcDir: File, content: String): File {
        if (!srcDir.exists()) {
            srcDir.mkdirs()
        }

        val infoFile = File(srcDir, "device_info.txt")
        infoFile.writeText(content, Charsets.UTF_8)
        return infoFile
    }

    private fun buildDeviceInfoText(): String {
        val deviceManager = DeviceManager()
        return buildString {
            append("SN: ${deviceManager.deviceId}\n")
            append("Model(Ext): ${DeviceManager().getSettingProperty("ro.product.vendor.model")}\n")
            append("Model: ${DeviceInfoUtil.getDevType()}\n")
            append("Firmware: \n - OS: ${Build.ID}\n")
            append(" - UFS: ${deviceManager.getSettingProperty("ro.ufs.custom")}-${deviceManager.getSettingProperty("ro.ufs.build.version")}\n")
            append(" - SE: ${deviceManager.getSettingProperty("persist-urv.se.version")}\n\n")


            append("OTA Firmware version:\n")
            append("OS version: \n - ${DeviceInfoUtil.getOSVersion()}\n")
            append("UFS version: \n - ${DeviceInfoUtil.getUFSVersion()}\n")
            append("SE version: \n - ${DeviceInfoUtil.getSEVersion()}\n\n")

            append("Device type: ${DeviceInfoUtil.getModelType()}\n")
            append("Language: ${DeviceInfoUtil.getLanguageType()}\n")
            append("Attestation Status:\n")
            append(DeviceInfoUtil.checkDeviceAttestation("attestation_key_alias"))
            append("\n\n")

            append("GSF: ${PackageUtil.isPackageInstalled(this@LogActivity, "com.google.android.gsf")}\n")
            append("GMS: ${PackageUtil.isPackageInstalled(this@LogActivity, "com.google.android.gms")} (available: ${GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this@LogActivity) == ConnectionResult.SUCCESS})\n")
            append("PlayStore: ${PackageUtil.isPackageInstalled(this@LogActivity, "com.android.vending")}\n")
            append("Chrome: ${PackageUtil.isPackageInstalled(this@LogActivity, "com.android.chrome")}\n")
            append("Maps: ${PackageUtil.isPackageInstalled(this@LogActivity, "com.google.android.apps.maps")}\n")
            append("UMS: ${PackageUtil.isPackageInstalled(this@LogActivity, "com.urovo.uhome")}\n")
            append("AppMarket_UMS: ${PackageUtil.isPackageInstalled(this@LogActivity, "com.urovo.appmarket")}\n")
            append("UTMS: ${PackageUtil.isPackageInstalled(this@LogActivity, "com.urovo.utms")}\n")
            append("AppMarket_UTMS: ${PackageUtil.isPackageInstalled(this@LogActivity, "com.urovo.utms.appmarket")}")

        }
    }

    private fun startRecordingUi() {
        binding.tvRecordingStatus.text = "Recording..."
        binding.tvRecordHint.text = "Tap again to stop recording"

        binding.btnRecord.setCardBackgroundColor(
            android.graphics.Color.parseColor("#EF4444")
        )

        binding.ivRecordIcon.setImageResource(android.R.drawable.ic_media_pause)

        binding.btnRecord.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(140)
            .withEndAction {
                binding.btnRecord.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(140)
                    .start()
            }
            .start()

        binding.viewRecordOuterRing.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .alpha(0.75f)
            .setDuration(220)
            .start()

        binding.viewRecordingDot.animate()
            .alpha(0.35f)
            .setDuration(500)
            .withEndAction {
                binding.viewRecordingDot.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start()
            }
            .start()
    }

    private fun stopRecordingUi() {
        binding.tvRecordingStatus.text = "Ready to record"
        binding.tvRecordHint.text = "Tap to start or stop recording"

        binding.btnRecord.setCardBackgroundColor(
            android.graphics.Color.parseColor("#FF5A6B")
        )

        binding.ivRecordIcon.setImageResource(android.R.drawable.ic_btn_speak_now)

        binding.btnRecord.animate()
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(120)
            .withEndAction {
                binding.btnRecord.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()

        binding.viewRecordOuterRing.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(200)
            .start()

        binding.viewRecordingDot.animate()
            .alpha(1f)
            .setDuration(150)
            .start()
    }
}