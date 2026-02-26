package com.example.posdemo.others

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.device.DeviceManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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

        binding.btnSetSn.setOnClickListener { onSetSnButtonClicked() }
        binding.btnCheckPackage.setOnClickListener { onCheckPackageButtonClicked() }
        binding.btnCheckAppList.setOnClickListener { onCheckAppListButtonClicked() }
        binding.btnInstallPackage.setOnClickListener { onInstallPackageButtonClicked() }
        binding.btnUmsStatus.setOnClickListener { onUmsStatusButtonClicked() }
        binding.btnGetUnfinishedOrder.setOnClickListener { onGetUnfinishedOrderButtonClicked() }
        binding.btnGetConfig.setOnClickListener { onGetConfigButtonClicked() }

        binding.etSerialNumber.setText(DeviceManager().deviceId.toString())

        umsHelper.sn = binding.etSerialNumber.text.toString()
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


    private fun onSetSnButtonClicked() {
        Toast.makeText(this, "Set SN successfully", Toast.LENGTH_SHORT).show()
        umsHelper.sn = binding.etSerialNumber.text.toString()
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


    private fun onCheckAppListButtonClicked() {
        binding.tvResult.text = ""
        binding.btnCheckAppList.isEnabled = false
        Thread {
            runCatching {
                return@runCatching umsHelper.getAppMarketList()
            }.onSuccess { ret ->
                runOnUiThread {
                    binding.tvResult.text = buildString {
                        append(ret)
                        append("\n - Get App list:\n")
                        append("https://uhomeov.urovo.com/api/v1/app/list\n")
                    }
                }
            }.onFailure {
                runOnUiThread {
                    binding.tvResult.text = it.message
                }
                it.printStackTrace()
            }
            runOnUiThread {
                binding.btnCheckAppList.isEnabled = true
            }
        }.start()
    }

    private fun onInstallPackageButtonClicked() {
        fun doInstall() {
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

        AlertDialog.Builder(this)
            .setTitle("Continue Install/Update?")
            .setMessage("This is using General way (FileProvider+IPC) to Install. If want to bypass permission check, need to use SKD API. e.g.\nDeviceManager().installApplication()")
            .setPositiveButton("Continue") { _, _ ->
                runOnUiThread {
                    doInstall()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        binding.btnGetUnfinishedOrder.isEnabled = false
        Thread {
            runCatching {
                return@runCatching umsHelper.getUnfinishedOrder()
            }.onSuccess { ret ->
                runOnUiThread {
                    binding.tvResult.text = buildString {
                        append(ret)
                        append("\n - Get Order:\n")
                        append("https://uhomeov.urovo.com/api/v1/order/unfinish\n")
                        append(" - UploadResult:\n")
                        append("https://uhomeov.urovo.com/api/v1/monitor/uploadGeneral\n\n")
                        append("Note: \n" +
                                " - Will poll periodically(Only Config+UnfinishedOrder)\n" +
                                " - Will poll thoroughly(Config+UnfinishedOrder+ReportInfo) when UMS first Installed or Reboot\n" +
                                " - Ungrouped to Any Group trigger MQTT for config_detail\n" +
                                " - MQTT will trigger all the UMS APIs for sure.")
                    }
                }
            }.onFailure {
                runOnUiThread {
                    binding.tvResult.text = it.message
                    it.printStackTrace()
                }
            }
            runOnUiThread {
                binding.btnGetUnfinishedOrder.isEnabled = true
            }
        }.start()
    }


    private fun onGetConfigButtonClicked() {
        binding.tvResult.text = ""
        binding.btnGetConfig.isEnabled = false
        Thread {
            runCatching {
                return@runCatching umsHelper.getConfig()
            }.onSuccess { ret ->
                runOnUiThread {
                    binding.tvResult.text = buildString {
                        append(ret)
                        append("\n - Get Config:\n")
                        append("https://uhomeov.urovo.com/api/v2/get/all/config\n")
                        append(" - UploadResult:\n")
                        append("https://uhomeov.urovo.com/api/v1/configRule/configRuleFeedback\n\n")
                        append("Further:\n" +
                                "Won't trigger when save but must have:\n" +
                                " - device_fence_config: Will trigger on Backend\n" +
                                " - device_config(Default: e.g. MQTT_PWD, PollingTime)\n" +
                                "\n" +
                                "Will examine if Config is right when polling:\n" +
                                " - silent_app_config: DM.setAutoRunningApp()\n" +
                                " - function_config: \n" +
                                "   - DM.enableHomeKey() \n" +
                                "   - DM.enableStatusBar() \n" +
                                "   - DM.setSettingProperty(Global-disable_pop_softinput, t/f) \n" +
                                "   - DM.setPackageInstaller() \n" +
                                "   - DM.controlUSB() \n" +
                                "   - DM.controlAdb() \n" +
                                "   - DM.controlBT() \n" +
                                "   - DM.controlWifi() \n" +
                                "   - DM.setSettingProperty(UROVO_FORBIDDEN_UNINSTALL, t/f) \n" +
                                " - wifi_whitelist_config: DM.insertToWifiWhiteList()/removeFromWifiWhiteList()\n" +
                                " - occupy_screen_app_config: DM.setLockTaskMode() + DM.saveLockPassword(PWD)\n" +
                                " - desktop_config: DM.setDefaultLauncher()/DM.removeDefaultLauncher()\n" +
                                "\n" +
                                "Won't examine if Config Code is the same when polling:\n" +
                                " - wifi_config: DM.connectWifi()\n" +
                                " - apn_config: DM.setAPN()/deleteAPN()\n" +
                                " - send_script_config: Intent(Act, Serv, Broadcast)\n" +
                                " - app_whitelist_config: DM.setAllowInstallApps()\n" +
                                " - app_deploy_config: Download(OSS) + DM.installApplication()\n" +
                                " - boot_animation_config: Download(OSS) + ???\n" +
                                " - strategy_config: UMS.startActivity(UStage, Config) + UStage.sendBroadcast/AIDL\n" +
                                "\n" +
                                "Note:\n" +
                                " - Will poll(Config+UnfinishedOrder) periodically\n" +
                                " - Will poll(Config+UnfinishedOrder) + push(ReportDeviceInfo) when UMS first Installed or Reboot\n" +
                                " - MoveGroup/DistributeSubOrg will clear all the Unfinished Orders")
                    }
                }
            }.onFailure {
                runOnUiThread {
                    binding.tvResult.text = it.message
                    it.printStackTrace()
                }
            }
            runOnUiThread {
                binding.btnGetConfig.isEnabled = true
            }
        }.start()
    }


    // <------------------ Helper methods ------------------> //

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