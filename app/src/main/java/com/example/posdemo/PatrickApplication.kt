package com.example.posdemo

import android.app.Application
import android.util.Log
import com.example.posdemo.btprinter.PERMISSION_REQ_BT
import com.urovo.file.logfile

class PatrickApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        logfile.setLogcatOut(true)
        Log.e("Patrick", "onCreate: This is the Start of the APP")
    }
}