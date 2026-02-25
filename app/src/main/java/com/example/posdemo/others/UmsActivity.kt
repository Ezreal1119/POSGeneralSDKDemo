package com.example.posdemo.others

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.posdemo.R
import com.example.posdemo.databinding.ActivityUmsBinding
import com.example.posdemo.helpers.UmsHelper
import com.example.posdemo.utils.PackageUtil
import com.urovo.sdk.install.InstallManagerImpl
import com.urovo.sdk.install.listener.InstallApkListener
import com.urovo.uhome.IUmsCallback
import com.urovo.uhome.IUmsFunction
import kotlin.math.PI

class UmsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUmsBinding

    private val umsHelper by lazy { UmsHelper(this) }
    private var ums: IUmsFunction? = null // This is an AIDL API
    private var isBound = false
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            ums = IUmsFunction.Stub.asInterface(service)
            runOnUiThread {
                Toast.makeText(this@UmsActivity, "UMS AIDL connected", Toast.LENGTH_SHORT).show()
                binding.btnUmsStatus.text = "Check UMS Status"
                binding.btnUmsStatus.isEnabled = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            ums = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUmsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnInstallPackage.setOnClickListener { onInstallPackageButtonClicked() }
        binding.btnCheckPackage.setOnClickListener { onCheckPackageButtonClicked() }
        binding.btnUmsStatus.setOnClickListener { onUmsStatusButtonClicked() }
        binding.btnGetUnfinishedOrder.setOnClickListener { onGetUnfinishedOrderButtonClicked() }
    }

    override fun onStart() {
        super.onStart()
        bindUmsServiceOnce()
        if (ums != null) {
            binding.btnUmsStatus.text = "Check UMS Status"
            binding.btnUmsStatus.isEnabled = true
        } else {
            binding.btnUmsStatus.text = "No Conn to UMS AIDL"
            binding.btnUmsStatus.isEnabled = false
        }
    }

    override fun onStop() {
        super.onStop()
        unbindUmsServiceOnce()
    }

    private fun onInstallPackageButtonClicked() {
        binding.btnInstallPackage.isEnabled = false
        Thread {
            runCatching {
                umsHelper.installPackage(binding.etPackageName.text.toString())
            }.onFailure {
                runOnUiThread {
                    binding.tvResult.text = it.message
                }
                it.printStackTrace()
            }
            runOnUiThread {
                binding.btnInstallPackage.isEnabled = true
            }
        }.start()
    }

    private fun onCheckPackageButtonClicked() {
        runCatching {
            val info = PackageUtil.getInstalledAppInfoOrNull(this, binding.etPackageName.text.toString())
            if (info != null) {
                binding.tvResult.text = buildString {
                    append("APP Info: \n")
                    append(" - PackageName: ${info.packageName}\n")
                    append(" - VersionName: ${info.versionName}\n")
                    append(" - VersionCode: ${info.versionCode}")
                }
            } else {
                binding.tvResult.text = "App not exists: ${binding.etPackageName.text}"
            }
        }
    }


    private fun onUmsStatusButtonClicked() {
        binding.tvResult.text = ""
        runCatching {
            ums ?: throw IllegalStateException("UMS not connected")
            Thread {
                val connectionStatus = if (ums?.connectStatus != null && ums?.connectStatus?.isNotBlank() ?: false) {
                    "MQTT Params gotten already"
                } else {
                    "No MQTT Params yet"
                }
                val onlineStatus = ums?.onlineStatus
                runOnUiThread {
                    binding.tvResult.text = buildString {
                        append("Connection Status:\n")
                        append(" - $connectionStatus\n\n")
                        append("Online(MQTT) Status:\n")
                        append(" - $onlineStatus")
                    }
                }
            }.start()
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onGetUnfinishedOrderButtonClicked() {
        binding.tvResult.text = ""
        Thread {
            runCatching {
                val ret = umsHelper.getUnfinishedOrder() ?: error("Body is empty")
                return@runCatching ret
            }.onSuccess { ret ->
                runOnUiThread {
                    binding.tvResult.text = ret
                }
            }.onFailure {
                runOnUiThread {
                    binding.tvResult.text = it.message
                    it.printStackTrace()
                }
            }
        }.start()
    }



    private fun bindUmsServiceOnce() {
        if (isBound) return

        val intent = Intent().apply {
            component = ComponentName(
                "com.urovo.uhome",
                "com.urovo.uhome.third.UmsFuncService"
            )
        }

        val ok = bindService(intent, conn, BIND_AUTO_CREATE)
        Log.e("Ums", "bindService ok=$ok")
        isBound = ok

        if (!ok) {
            binding.btnUmsStatus.text = "Bind failed"
            binding.btnUmsStatus.isEnabled = false
        }
    }

    private fun unbindUmsServiceOnce() {
        if (!isBound) return
        runCatching { unbindService(conn) }
        isBound = false
        ums = null
    }

}