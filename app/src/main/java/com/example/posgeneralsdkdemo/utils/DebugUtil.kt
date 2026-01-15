package com.example.posgeneralsdkdemo.utils

import android.app.Activity
import android.util.Log
import android.widget.Toast

object DebugUtil {

    fun logAndToast(activity: Activity, tag: String, message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        Log.e(tag, message)
    }

}