package com.example.posgeneralsdkdemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ComponentCaller
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.device.DeviceManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.databinding.ActivityApiTestBinding
import com.example.posgeneralsdkdemo.others.PACKAGE_COMPONENT_INFO
import com.example.posgeneralsdkdemo.others.PACKAGE_COMPONENT_MAIN
import com.example.posgeneralsdkdemo.utils.PermissionUtil
import java.io.File
import java.io.FileOutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.security.Permission
import java.util.Locale

private const val TAG = "Patrick"
private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
private const val PERMISSION_REQ_SCAN = 1001
class ApiTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApiTestBinding
    lateinit var tts: TextToSpeech


    @SuppressLint("MissingPermission", "ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.e(TAG, "onCreate: $packageName", )

        binding.btnTest1.setOnClickListener {
            if (!PermissionUtil.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_SCAN)) {
                Toast.makeText(this, "Please grant camera permission first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent("com.ubx.scandemo.SCAN").apply {
                putExtra("camera", 1)
                putExtra("timeout", 30)
                putExtra("title", "Scan")
                putExtra("up_prompt", "")
                putExtra("down_prompt", "")
                putExtra("flash_enable", true)
            }
            startActivityForResult(intent, 1002)
        }
//        binding.btnTest2.setOnClickListener {
//            Log.e(TAG, "btnTest2")
//            Log.e(TAG, "onCreate: ${DeviceManager().getSettingProperty("Global-ntp_server")}", )
//            Log.e(TAG, "onCreate: ${DeviceManager().getSettingProperty("persist-persist.sys.timezone")}", )
//            Log.e(TAG, "onCreate: ${DeviceManager().getSettingProperty("persist-persist.sys.settimezone")}", )
//
//        }
//        binding.btnTest3.setOnClickListener {
//            Log.e(TAG, "btnTest3")
//            DeviceManager().setAllowInstallApps("com.example.abd", 0, 2)
//        }
//        binding.btnTest4.setOnClickListener {
//        }
//
//        DeviceManager().setDeviceOwner(ComponentName.unflattenFromString("${packageName}/${MainActivity::class.java.name}"))
//        runCatching {
//            Log.e(TAG, "onCreate: ${DeviceManager().deviceOwner}", )
//
//        }.onFailure {
//            it.printStackTrace()
//        }
//        DeviceManager().setDeviceOwner(ComponentName.unflattenFromString(PACKAGE_COMPONENT_MAIN))

    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1002) {
            if (resultCode == RESULT_OK) {
                val scanResult = data?.getStringExtra("scan_result")
                runOnUiThread {
                    Toast.makeText(this, scanResult, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}