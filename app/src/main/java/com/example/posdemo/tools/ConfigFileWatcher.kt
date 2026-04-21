package com.example.posdemo.tools

import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File

class ConfigFileWatcher(
    private val filePath: String,
    private val onFileChanged: () -> Unit
) {

    companion object {
        private const val TAG = "Patrick"
    }

    private val mainLooper = Handler(Looper.getMainLooper())
    private val targetFile = File(filePath)
    private var fileObserver: FileObserver? = null
    private var lastModifiedTime: Long = 0L
    private var isUpdating: Boolean = false


    fun startWatching() {
        // 1. Make sure Parent directory exists.
        lastModifiedTime = targetFile.lastModified()
        val parentDir = targetFile.parentFile
        if (parentDir == null) {
            Log.e(TAG, "Parent directory is null")
            return
        }
        val fileName = targetFile.name

        // 2. Make sure fileObserver is not running
        fileObserver?.stopWatching()

        // 3. Create the fileObserver that listens to the parent directory. W
        fileObserver = object : FileObserver(parentDir.absolutePath, MODIFY or CLOSE_WRITE or CREATE) {
            override fun onEvent(event: Int, path: String?) {
                // Make sure only callback when the file is the one specified
                if (path == null || path != fileName) {
                    return
                }
                // Make sure callback in these cases
                if ((event and CREATE) != 0 ||
                    (event and MODIFY) != 0 ||
                    (event and CLOSE_WRITE) != 0
                ) {
                    // Make sure to give some time for the file to be stable.
                    mainLooper.postDelayed({
                        checkAndNotify()
                    }, 1000)
                }
            }
        }
        fileObserver?.startWatching()
        Log.e(TAG, "Started watching: $filePath")
    }

    fun stopWatching() {
        fileObserver?.stopWatching()
        fileObserver = null
        Log.e(TAG, "Stopped watching: $filePath")
    }

    // Will be triggered once the targetFile is created, modified, or close_write.
    private fun checkAndNotify() {
        // 1. Make sure not updating more than one time
        if (isUpdating) {
            Log.e(TAG, "Already updating, skip")
            return
        }

        // 2. Make sure targetFile still exists, otherwise no need to do anything.
        if (!targetFile.exists()) {
            Log.e(TAG, "Target file no longer exists")
            return
        }

        // 3. Process lastModifiedTime
        val currentModifiedTime = targetFile.lastModified() // Current lastModifiedTime of targetFile
        if (currentModifiedTime <= lastModifiedTime) { // Current lastModifiedTime == recorded lastModifiedTime means no change since last checkAndNotify()
            Log.e(TAG, "File not changed")
            return
        }
        lastModifiedTime = currentModifiedTime // update the recorded lastModifiedTime if changed

        // 4. Update "isUpdate" flag
        isUpdating = true

        // 5. Call the lambda function that passed in when create the object
        try {
            onFileChanged()
        } finally {
            isUpdating = false
        }
    }
}