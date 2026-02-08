package com.example.posdemo.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

private const val TAG = "BootReceiver"
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.e(TAG, "BootReceiver - onReceive: elapsedRealtime=${SystemClock.elapsedRealtime()}")
        }
    }
}