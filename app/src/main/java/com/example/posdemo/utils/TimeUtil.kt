package com.example.posdemo.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtil {
    fun getCurrentTimeStr(): String {
        val sdf = SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date())
    }
}