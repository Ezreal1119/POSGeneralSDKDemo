package com.example.posgeneralsdkdemo

import android.content.ComponentName
import android.device.DeviceManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.others.PACKAGE_COMPONENT_INFO
import com.example.posgeneralsdkdemo.others.PACKAGE_COMPONENT_MAIN
import java.net.Socket
import java.nio.charset.Charset

private const val TAG = "Patrick"
class ApiTestActivity : AppCompatActivity() {

    private val tvResult by lazy { findViewById<Button>(R.id.tvResult) }
    private val btnTest1 by lazy { findViewById<Button>(R.id.btnTest1) }

    private val btnTest2 by lazy { findViewById<Button>(R.id.btnTest2) }
    private val btnTest3 by lazy { findViewById<Button>(R.id.btnTest3) }
    private val btnTest4 by lazy { findViewById<Button>(R.id.btnTest4) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_test)
        Log.e(TAG, "onCreate: $packageName", )

        btnTest1.setOnClickListener {
            DeviceManager().rightKeyEnabled = true
            DeviceManager().leftKeyEnabled = true
        }
        btnTest2.setOnClickListener {
            DeviceManager().setDefaultLauncher(ComponentName.unflattenFromString(PACKAGE_COMPONENT_MAIN))
            Log.e(TAG, "onCreate: 1", )
        }
        btnTest3.setOnClickListener {
            DeviceManager().removeDefaultLauncher(packageName)
            Log.e(TAG, "onCreate: 2", )
        }
        btnTest4.setOnClickListener {
            printText("10.10.11.177", 9100, "Hello Patrick\nHello Again!")
        }

        DeviceManager().setDeviceOwner(ComponentName.unflattenFromString("${packageName}/${MainActivity::class.java.name}"))
        runCatching {
            Log.e(TAG, "onCreate: ${DeviceManager().deviceOwner}", )

        }.onFailure {
            it.printStackTrace()
        }
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