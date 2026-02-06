package com.example.posgeneralsdkdemo

import android.annotation.SuppressLint
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

private const val TAG = "Patrick"
class ApiTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApiTestBinding

    @SuppressLint("MissingPermission", "ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.e(TAG, "onCreate: $packageName", )

        binding.btnTest1.setOnClickListener {
            Log.e(TAG, "onCreate: btnTest1")

            val pName = "com.urovo.appmarket"

            val pm = this.packageManager
            val intent = pm.getLaunchIntentForPackage(pName)

            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(intent)

        }
        binding.btnTest2.setOnClickListener {
            Log.e(TAG, "btnTest2")
            Log.e(TAG, "onCreate: ${DeviceManager().getAllowInstallApps(0)}", )
            
        }
        binding.btnTest3.setOnClickListener {
            Log.e(TAG, "btnTest3")
            DeviceManager().setAllowInstallApps("com.example.abd", 0, 2)
        }
        binding.btnTest4.setOnClickListener {
        }

        DeviceManager().setDeviceOwner(ComponentName.unflattenFromString("${packageName}/${MainActivity::class.java.name}"))
        runCatching {
            Log.e(TAG, "onCreate: ${DeviceManager().deviceOwner}", )

        }.onFailure {
            it.printStackTrace()
        }
        DeviceManager().setDeviceOwner(ComponentName.unflattenFromString(PACKAGE_COMPONENT_MAIN))

    }

    fun printText(ip: String, port: Int = 9100, text: String) {
        Thread {

            var socket: Socket? = null
            try {
                socket = Socket(ip, port)
                socket.soTimeout = 5000

                val out = socket.getOutputStream()


                out.write(byteArrayOf(0x1B, 0x40)) // Initialize
                out.write(text.toByteArray(Charsets.UTF_8))
                out.write(byteArrayOf(0x0A)) // feed line


                out.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }.start()
    }



}