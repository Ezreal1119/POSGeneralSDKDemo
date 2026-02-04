package com.example.posgeneralsdkdemo.others

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.R
import com.urovo.sdk.install.InstallManagerImpl
import com.urovo.sdk.install.listener.InstallApkListener
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.getValue
import androidx.core.net.toUri
import com.example.posgeneralsdkdemo.databinding.ActivityInstallManagerBinding
import com.example.posgeneralsdkdemo.utils.PermissionUtil

// <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>


private const val UMS_DOWNLOAD_URL = "https://ota-oss.urovo.com/https_ota_prod/Android/SQ29MB/DS/PKG-XX/english/10.25.0722.03/19190/patch.zip"
private const val APPMARKET_UMS_DOWNLOAD_URL = "https://ota-oss.urovo.com/https_ota_prod/Android/SQ29MB/DS/PKG-XX/english/10.25.0722.03/19197/patch.zip"
private const val UMS_NAME_ZIP = "ums_patrick.zip"
private const val APPMARKET_UMS_NAME_ZIP = "appmarket_ums_patrick.zip"
private const val UMS_NAME_APK = "ums_patrick.apk"
private const val APPMARKET_UMS_NAME_APK = "appmarket_ums_patrick.apk"
private const val UMS_PACKAGE = "com.urovo.uhome"
private const val APPMARKET_UMS_PACKAGE = "com.urovo.appmarket"
class InstallManager : AppCompatActivity() {

    private lateinit var binding: ActivityInstallManagerBinding

    private val mInstallManager by lazy { InstallManagerImpl.getInstance(this) }
    private var isUmsOrAppMarket = true // true means UMS, false means AppMarket

    private val myInstallApkListener = object: InstallApkListener {
        override fun onInstallFinished(
            packageName: String?,
            returnCode: Int,
            returnMsg: String?
        ) {
            runOnUiThread {
                binding.tvResult.text = buildString {
                    append("APP installation finished:\n\n")
                    append(" - Package Name: \n$packageName\n\n")
                    append(" - Return Code: \n$returnCode\n\n")
                    append(" - Return Message: \n$returnMsg\n\n")
                }
                if (isPackageInstalled(UMS_PACKAGE)) {
                    binding.btnInstallUMS.isEnabled = false
                    binding.btnUninstallUMS.isEnabled = true
                } else {
                    binding.btnInstallUMS.isEnabled = true
                    binding.btnUninstallUMS.isEnabled = false
                }
                if (isPackageInstalled(APPMARKET_UMS_PACKAGE)) {
                    binding.btnInstallAppMarketUMS.isEnabled = false
                    binding.btnUninstallAppMarketUMS.isEnabled = true
                } else {
                    binding.btnInstallAppMarketUMS.isEnabled = true
                    binding.btnUninstallAppMarketUMS.isEnabled = false
                }
            }
        }

        override fun onUnInstallFinished(
            packageName: String?,
            returnCode: Int,
            returnMsg: String?
        ) {
            runOnUiThread {
                binding.tvResult.text = buildString {
                    append("APP Uninstallation finished:\n\n")
                    append(" - Package Name: \n$packageName\n\n")
                    append(" - Return Code: \n$returnCode\n\n")
                    append(" - Return Message: \n$returnMsg\n\n")
                }
                if (isPackageInstalled(UMS_PACKAGE)) {
                    binding.btnInstallUMS.isEnabled = false
                    binding.btnUninstallUMS.isEnabled = true
                } else {
                    binding.btnInstallUMS.isEnabled = true
                    binding.btnUninstallUMS.isEnabled = false
                }
                if (isPackageInstalled(APPMARKET_UMS_PACKAGE)) {
                    binding.btnInstallAppMarketUMS.isEnabled = false
                    binding.btnUninstallAppMarketUMS.isEnabled = true
                } else {
                    binding.btnInstallAppMarketUMS.isEnabled = true
                    binding.btnUninstallAppMarketUMS.isEnabled = false
                }
            }
        }
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val zipName = if (isUmsOrAppMarket) UMS_NAME_ZIP else APPMARKET_UMS_NAME_ZIP
            val apkName = if (isUmsOrAppMarket) UMS_NAME_APK else APPMARKET_UMS_NAME_APK

            val zipFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), zipName)
            val apkFile = File(getExternalFilesDir(null), apkName)
            if (!zipFile.isFile) {
                Toast.makeText(this@InstallManager, "ZIP not exists", Toast.LENGTH_SHORT).show()
                return
            }
            var foundApk = false
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.lowercase().endsWith(".apk")) {
                        apkFile.parentFile?.mkdirs()
                        FileOutputStream(apkFile).use { fos ->
                            zis.copyTo(fos)
                            fos.flush()
                            fos.fd.sync()
                        }
                        zis.closeEntry()
                        foundApk = true
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            if (!foundApk || !apkFile.isFile || apkFile.length() <= 0L) {
                Toast.makeText(this@InstallManager, "No valid APK found in zip", Toast.LENGTH_SHORT).show()
                return
            }
            runCatching {
                mInstallManager.install(apkFile.path, myInstallApkListener)
            }.onSuccess {
                when (apkName) {
                    "ums_patrick.apk" -> Toast.makeText(this@InstallManager, "Installing UMS", Toast.LENGTH_SHORT).show()
                    "appmarket_ums_patrick.apk"-> Toast.makeText(this@InstallManager, "Installing AppMarket_UMS", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                binding.tvResult.text = it.message
                it.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstallManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnInstallApp.setOnClickListener { onInstallAppButtonClicked() }
        binding.btnUninstallApp.setOnClickListener { onUninstallAppButtonClicked() }
        binding.btnInstallUMS.setOnClickListener { onInstallUMSButtonClicked() }
        binding.btnUninstallUMS.setOnClickListener { onUninstallUMSButtonClicked() }
        binding.btnInstallAppMarketUMS.setOnClickListener { onInstallAppMarketUMSButtonClicked() }
        binding.btnUninstallAppMarketUMS.setOnClickListener { onUninstallAppMarketUMSButtonClicked() }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
//        PermissionUtil.ensureAllFilesAccess(this)
        if (isPackageInstalled(UMS_PACKAGE)) {
            binding.btnInstallUMS.isEnabled = false
            binding.btnUninstallUMS.isEnabled = true
        } else {
            binding.btnInstallUMS.isEnabled = true
            binding.btnUninstallUMS.isEnabled = false
        }
        if (isPackageInstalled(APPMARKET_UMS_PACKAGE)) {
            binding.btnInstallAppMarketUMS.isEnabled = false
            binding.btnUninstallAppMarketUMS.isEnabled = true
        } else {
            binding.btnInstallAppMarketUMS.isEnabled = true
            binding.btnUninstallAppMarketUMS.isEnabled = false
        }
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(downloadReceiver)
    }

    private fun onInstallAppButtonClicked() {
        runCatching {
            if (!File(binding.etApkPath.text.toString().trim()).isFile) {
                binding.tvResult.text = "Apk not exists!"
                return
            }
            mInstallManager.install(binding.etApkPath.text.toString().trim(), myInstallApkListener)
        }.onSuccess {
            binding.tvResult.text = ""
            Toast.makeText(this, "Installing App", Toast.LENGTH_SHORT).show()
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onUninstallAppButtonClicked() {
        runCatching {
            mInstallManager.uninstall(
                binding.etPackageName.text.toString().trim(), myInstallApkListener)
        }.onSuccess {
            binding.tvResult.text = ""
            Toast.makeText(this, "Uninstalling ${binding.etPackageName.text.toString().trim()}", Toast.LENGTH_SHORT).show()
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onInstallUMSButtonClicked() {
        if (getNetworkType() == null) {
            Toast.makeText(this, "Please connect to Internet first", Toast.LENGTH_SHORT).show()
            return
        }
        val request = DownloadManager.Request(UMS_DOWNLOAD_URL.toUri()).apply {
            setTitle("Downloading UMS Agent")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, UMS_NAME_ZIP)
        }
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        isUmsOrAppMarket = true
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        binding.btnInstallUMS.isEnabled = false
    }

    private fun onUninstallUMSButtonClicked() {
        runCatching {
            mInstallManager.uninstall(UMS_PACKAGE, myInstallApkListener)
        }.onSuccess {
            binding.tvResult.text = ""
            binding.btnUninstallUMS.isEnabled = false
            Toast.makeText(this, "Uninstalling UMS", Toast.LENGTH_SHORT).show()
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onInstallAppMarketUMSButtonClicked() {
        if (getNetworkType() == null) {
            Toast.makeText(this, "Please connect to Internet first", Toast.LENGTH_SHORT).show()
            return
        }
        val request = DownloadManager.Request(APPMARKET_UMS_DOWNLOAD_URL.toUri()).apply {
            setTitle("Downloading UMS Agent")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APPMARKET_UMS_NAME_ZIP)
        }
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        isUmsOrAppMarket = false
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        binding.btnInstallAppMarketUMS.isEnabled = false
    }


    private fun onUninstallAppMarketUMSButtonClicked() {
        runCatching {
            mInstallManager.uninstall(APPMARKET_UMS_PACKAGE, myInstallApkListener)
        }.onSuccess {
            binding.tvResult.text = ""
            binding.btnUninstallAppMarketUMS.isEnabled = false
            Toast.makeText(this, "Uninstalling AppMarket_UMS", Toast.LENGTH_SHORT).show()
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }



    private fun getNetworkType(): String? {
        val activeNetworkInfo = (this
            .getSystemService("connectivity") as ConnectivityManager).activeNetworkInfo
        if (activeNetworkInfo == null) {
            return null
        }
        return if (activeNetworkInfo.type == 1) "Wifi" else "4G"
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            this.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

}

