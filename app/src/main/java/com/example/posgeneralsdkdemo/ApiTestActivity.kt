package com.example.posgeneralsdkdemo

import android.device.DeviceManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

private const val TAG = "Patrick"
class ApiTestActivity : AppCompatActivity() {

    private val tvResult by lazy { findViewById<Button>(R.id.tvResult) }
    private val btnTest1 by lazy { findViewById<Button>(R.id.btnTest1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_test)

        btnTest1.setOnClickListener {
            // Get Brightness
            runCatching {
                Toast.makeText(this, "Brightness=${Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "onCreate: Brightness=${
                    Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)}", )
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}