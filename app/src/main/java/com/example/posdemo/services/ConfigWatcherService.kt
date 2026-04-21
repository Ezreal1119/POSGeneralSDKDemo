package com.example.posdemo.services

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.posdemo.tools.ConfigFileWatcher
import com.google.gson.Gson
import java.io.File

class ConfigWatcherService : Service() {

    companion object {
        private const val TAG = "Patrick"
        private const val TARGET_FILE_PATH = "/sdcard/patrick_config.json"
    }

    private var configFileWatcher: ConfigFileWatcher? = null

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "ConfigWatcherService onCreate")
        configFileWatcher = ConfigFileWatcher(TARGET_FILE_PATH) {
            Log.e(TAG, "Target file changed")
            try {
                val file = File(TARGET_FILE_PATH)
                val json = file.readText()
                val config = Gson().fromJson(json, PatrickConfig::class.java)
                Log.d(TAG, "Parsed config: ${config.patrick_config}")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        applicationContext,
                        "Config received: ${config.patrick_config}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read config file", e)
            }
        }
        configFileWatcher?.startWatching()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        configFileWatcher?.stopWatching()
        configFileWatcher = null
    }

    // Make sure the service is killed when the APP is removed from Recents list
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}


private data class PatrickConfig(
    val patrick_config: String
)