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

        btnTest1.setOnClickListener {
            Log.e(TAG, "onCreate: ${SystemClock.elapsedRealtime()}", )
        }
        btnTest2.setOnClickListener {
            DeviceManager().setDefaultLauncher(ComponentName.unflattenFromString(PACKAGE_COMPONENT_MAIN))
            Log.e(TAG, "onCreate: 1", )
        }
        btnTest3.setOnClickListener {
            DeviceManager().setDefaultLauncher(ComponentName.unflattenFromString(PACKAGE_COMPONENT_INFO))
            Log.e(TAG, "onCreate: 2", )
        }
    }
}