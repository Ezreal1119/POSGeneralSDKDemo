package com.example.posdemo.printers

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.posdemo.START_PRINT_SERVICE
import com.example.posdemo.btprinter.PERMISSIONS_BT
import com.example.posdemo.databinding.ActivityWebPrintBinding
import com.example.posdemo.services.WebSocketPrintService
import com.example.posdemo.services.WebSocketPrinterServiceListener
import com.example.posdemo.utils.DeviceInfoUtil
import com.example.posdemo.utils.FileUtil
import com.example.posdemo.utils.PackageUtil
import com.example.posdemo.utils.PermissionUtil
import com.urovo.sdk.install.InstallManagerImpl
import com.urovo.sdk.install.listener.InstallApkListener
import org.bouncycastle.util.Pack
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class WebPrintActivity : AppCompatActivity(), WebSocketPrinterServiceListener {

    companion object {
        private const val PERMISSION_REQ_BT_NOTIFICATION = 1002
        private const val HTML_WEB_SOCKET_FILE_NAME = "web_socket_demo.html"

    }

    private lateinit var binding: ActivityWebPrintBinding

    private var service: WebSocketPrintService? = null
    private var bound = false
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            binder: IBinder?
        ) {
            service = (binder as WebSocketPrintService.LocalBinder).getService()
            service?.setListener(this@WebPrintActivity)
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service?.setListener(null)
            service = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebPrintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            btnStartWebSocketPrinter.setOnClickListener { onStartWebSocketPrinterButtonClicked() }
            btnOpenPrintWebLocal.setOnClickListener { onOpenPrintWebLocalButtonClicked() }
            btnOpenPrintWebCloud.setOnClickListener { onOpenPrintWebCloudButtonClicked() }
            btnStopWebSocketPrinter.setOnClickListener { onStopWebSocketPrinterButtonClicked() }
            btnInstallPrintService.setOnClickListener { onInstallPrintServiceButtonClicked() }

            binding.tvPrintNotes.text = buildString {
                appendLine("Notes:")
                appendLine("You can also visit this web on the computer that is in the same network of the terminal.")
                appendLine("Then, send print command to the terminal's IP to test WebSocket Print.")
                appendLine("You should use HTTP instead of HTTPS on computer!!!")
                appendLine()
                appendLine("URL: http://urovo-tech.patrick-shenzhen.org/api/web-print")
                appendLine("Terminal_IP: ${DeviceInfoUtil.getWifiIpv4(this@WebPrintActivity)}")
                }
        }


        if (intent.getBooleanExtra(START_PRINT_SERVICE, false) ) {
            onStartWebSocketPrinterButtonClicked()
        }
    }

    override fun onStart() {
        super.onStart()
        if (WebSocketPrintService.isRunning) {
            WebSocketPrintService.bind(this, conn)
        }
        binding.btnStartWebSocketPrinter.isEnabled = !WebSocketPrintService.isRunning
        binding.btnStopWebSocketPrinter.isEnabled = WebSocketPrintService.isRunning
    }

    override fun onStop() {
        super.onStop()
        runCatching {
            if (bound) {
                service?.setListener(null)
                service = null
                unbindService(conn)
                bound = false
            }
        }.onFailure {
            it.printStackTrace()
        }
    }


    override fun onServiceStart() {
        runOnUiThread {
            binding.btnStartWebSocketPrinter.isEnabled = false
            binding.btnStopWebSocketPrinter.isEnabled = true
        }
    }

    override fun onServiceDestroy() {
        runOnUiThread {
            binding.btnStartWebSocketPrinter.isEnabled = true
            binding.btnStopWebSocketPrinter.isEnabled = false
        }
    }


    private fun onStartWebSocketPrinterButtonClicked() {
        if(!PermissionUtil.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS) + PERMISSIONS_BT, PERMISSION_REQ_BT_NOTIFICATION)) {
            Toast.makeText(this, "Please grant Permission first", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            WebSocketPrintService.start(this)
            WebSocketPrintService.bind(this, conn)
        }.onFailure {
            Toast.makeText(this, "Server started failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    /*
        1. Register a FileProvider (It's an Interface for other APP to access the File asset of this APP.
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

        <?xml version="1.0" encoding="utf-8"?>
        <paths>
            <cache-path name="cache" path="." />
        </paths>

        2. Create a Uri Interface using FileProvider, this is the requirement after Android 8
        (The other APP can only access the File asset of this APP using FileProvider API)
     */
    private fun onOpenPrintWebLocalButtonClicked() {
        val file = FileUtil.copyAssetToCacheIfNeeded(this, HTML_WEB_SOCKET_FILE_NAME)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // This means no package can handle this Intent(with action=ACTION_VIEW)
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onOpenPrintWebCloudButtonClicked() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://urovo-tech.patrick-shenzhen.org/api/web-print")
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onStopWebSocketPrinterButtonClicked() {
        runCatching {
            unbindService(conn)
            WebSocketPrintService.stop(this)
        }.onSuccess {
            Toast.makeText(this, "Server stopped successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Server stopped failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onInstallPrintServiceButtonClicked() {
        if (PackageUtil.isPackageInstalled(this, "com.urovo.printerservice.more")) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Prompt")
                .setMessage("Print Service is already installed.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        binding.btnInstallPrintService.isEnabled = false

        val progressDialog = android.app.ProgressDialog(this).apply {
            setTitle("Installing Print Service")
            setMessage("Starting download...")
            setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
            max = 100
            progress = 0
            setCancelable(false)
            show()
        }

        val apkUrl = "https://cdn.patrick-shenzhen.org/urovo/apk/PrinterService_MORE_260407_release_signed.apk"

        Thread {
            runCatching {
                val apkFile = File(
                    this.getExternalFilesDir(null),
                    "PrinterService_MORE_260407_release_signed.apk"
                )

                val conn = URL(apkUrl).openConnection()
                val totalSize = conn.contentLength

                conn.inputStream.use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0
                        var read: Int

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read

                            if (totalSize > 0) {
                                val percent = downloaded * 100 / totalSize
                                runOnUiThread {
                                    progressDialog.progress = percent
                                    progressDialog.setMessage("Downloading... $percent%")
                                }
                            }
                        }
                    }
                }

                runOnUiThread {
                    progressDialog.isIndeterminate = true
                    progressDialog.setMessage("Download completed. Installing...")

                    InstallManagerImpl
                        .getInstance(this)
                        .install(apkFile.path, object : InstallApkListener {
                            override fun onInstallFinished(
                                packageName: String?,
                                returnCode: Int,
                                returnMsg: String?
                            ) {
                                runOnUiThread {
                                    progressDialog.dismiss()
                                    Toast.makeText(
                                        this@WebPrintActivity,
                                        "Installed successfully. Please grant all the permissions。",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    binding.btnInstallPrintService.isEnabled = true
                                    startActivity(Intent().setClassName("com.urovo.printerservice.more", "com.urovo.printerservice.MainActivity"))
                                }
                            }

                            override fun onUnInstallFinished(
                                packageName: String?,
                                returnCode: Int,
                                returnMsg: String?
                            ) {
                                // Not used
                            }
                        })
                }

            }.onFailure { e ->
                e.printStackTrace()
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "Download/install failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnInstallPrintService.isEnabled = true
                }
            }
        }.start()
    }
}