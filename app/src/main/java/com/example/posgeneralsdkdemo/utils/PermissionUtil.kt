package com.example.posgeneralsdkdemo.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object PermissionUtil {

    fun requestPermissions(activity: Activity, permissions: Array<String>, req: Int) {
        val ungranted = mutableListOf<String>().apply {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    add(permission)
                }
            }
        }
        if (ungranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, ungranted.toTypedArray(), req)
        }
    }

    fun checkPermissions(context: Context, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun ensureAllFilesAccess(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        if (Environment.isExternalStorageManager()) return true

        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = "package:${activity.packageName}".toUri()
        }
        activity.startActivity(intent)
        return false
    }

}