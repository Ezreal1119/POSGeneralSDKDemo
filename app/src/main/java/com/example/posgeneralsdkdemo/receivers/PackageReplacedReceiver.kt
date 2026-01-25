package com.example.posgeneralsdkdemo.receivers

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.device.DeviceManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.example.posgeneralsdkdemo.R

// <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
private const val TAG = "PackageReplacedReceiver"
private const val CHANNEL_ID = "update_channel"
class PackageReplacedReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.e(TAG, "onReceive: ")
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        if(Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {
            // Can set Default Launcher(Kiosk Mode as well), then do this.
//            Handler(Looper.getMainLooper()).postDelayed({
//                DeviceManager().shutdown(true)
//            }, 1000)
            val packageManager = context.packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName) ?: return
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    ensureUpdateChannel(context)
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle("App Updated")
                            .setContentText("Tap to reopen the app")
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .build()
                    nm.notify(10086, notification)
                } else {
                    context.startActivity(launchIntent)
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }


    private fun ensureUpdateChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                CHANNEL_ID,
                "App Update",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications after app update"
            }
            nm.createNotificationChannel(ch)
        }
    }

}