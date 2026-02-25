package com.example.posdemo.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.posdemo.data.InstalledAppInfo

object PackageUtil {

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getInstalledAppInfoOrNull(context: Context, packageName: String): InstalledAppInfo? {
        val pm = context.packageManager
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getPackageInfo(packageName, 0)
            }

            val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            }
            InstalledAppInfo(packageName, info.versionName, versionCode)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

}