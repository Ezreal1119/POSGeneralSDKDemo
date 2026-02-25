package com.example.posdemo.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object PermissionUtil {

    fun requestPermissions(activity: Activity, permissions: Array<String>, req: Int): Boolean {
        // Return true if PERMISSIONS have been granted already
        val ungranted = mutableListOf<String>().apply {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    add(permission)
                }
            }
        }
        if (ungranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, ungranted.toTypedArray(), req)
            return false
        }
        return true
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

    // <uses-permission android:name="android.permission.WRITE_SETTINGS"/>
    fun ensureCanWriteSettings(context: Context): Boolean {
        if (!Settings.System.canWrite(context)) {
            Toast.makeText(context, "Need permission: Modify system settings", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
            return false
        }
        return true
    }

    fun ensureCanRequestPackageInstallsAutoToast(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pm = activity.packageManager
            if (pm.canRequestPackageInstalls()) return true
            activity.runOnUiThread {
                Toast.makeText(activity, "Please grant permission first", Toast.LENGTH_SHORT).show()
            }
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${activity.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            return false
        }
        return true
    }

}