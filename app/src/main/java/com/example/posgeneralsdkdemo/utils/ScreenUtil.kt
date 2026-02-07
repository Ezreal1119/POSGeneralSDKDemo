package com.example.posgeneralsdkdemo.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

object ScreenUtil {

    fun getScreenWidth(activity: Activity): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return activity.windowManager.currentWindowMetrics.bounds.width()
        } else {
            val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMatrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMatrics)
            return displayMatrics.widthPixels
        }
    }

    fun getScreenHeight(activity: Activity): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return activity.windowManager.currentWindowMetrics.bounds.height()
        } else {
            val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.heightPixels
        }
    }
}