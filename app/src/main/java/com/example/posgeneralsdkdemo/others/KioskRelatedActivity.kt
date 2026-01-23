package com.example.posgeneralsdkdemo.others

import android.annotation.SuppressLint
import android.content.ComponentName
import android.device.DeviceManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.R
import androidx.core.content.edit


private const val PREF_KIOSK = "pref_kiosk"
private const val KEY_HOME_ENABLED = "home_enabled"
private const val KEY_RECENT_ENABLED = "recent_enabled"
private const val STATUS_BAR_ENABLED = "status_bar_enabled"
private const val KIOSK_PASSWORD = "123456"
const val PACKAGE_COMPONENT_MAIN = "com.example.posgeneralsdkdemo/com.example.posgeneralsdkdemo.MainActivity"

class KioskRelatedActivity : AppCompatActivity() {

    private val btnEnableHome by lazy { findViewById<Button>(R.id.btnEnableHome) }
    private val btnDisenableHome by lazy { findViewById<Button>(R.id.btnDisenableHome) }
    private val btnEnableRecent by lazy { findViewById<Button>(R.id.btnEnableRecent) }
    private val btnDisenableRecent by lazy { findViewById<Button>(R.id.btnDisenableRecent) }
    private val btnEnableStatusBar by lazy { findViewById<Button>(R.id.btnEnableStatusBar) }
    private val btnDisenableStatusBar by lazy { findViewById<Button>(R.id.btnDisenableStatusBar) }
    private val btnSetKiosk by lazy { findViewById<Button>(R.id.btnSetKiosk) }
    private val btnCancelKiosk by lazy { findViewById<Button>(R.id.btnCancelKiosk) }
    private val btnSetKioskPwd123456 by lazy { findViewById<Button>(R.id.btnSetKioskPwd123456) }
    private val btnSetAutoStart by lazy { findViewById<Button>(R.id.btnSetAutoStart) }
    private val btnCancelAutoStart by lazy { findViewById<Button>(R.id.btnCancelAutoStart) }
    private val btnSetForceLockScreen by lazy { findViewById<Button>(R.id.btnSetForceLockScreen) }

    private fun pref() = getSharedPreferences(PREF_KIOSK, MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kiosk_related)

        btnEnableHome.setOnClickListener { onEnableHomeButtonClicked() }
        btnDisenableHome.setOnClickListener { onDisenableHomeButtonClicked() }
        btnEnableRecent.setOnClickListener { onEnableRecentButtonClicked() }
        btnDisenableRecent.setOnClickListener { onDisnableRecentButtonClicked() }
        btnEnableStatusBar.setOnClickListener { onEnableStatusBarButtonClicked() }
        btnDisenableStatusBar.setOnClickListener { onDisenableStatusBarButtonClicked() }
        btnSetKiosk.setOnClickListener { onSetKioskButtonClicked() }
        btnCancelKiosk.setOnClickListener { onCancelKioskButtonClicked() }
        btnSetKioskPwd123456.setOnClickListener { onSetKioskPwd123456ButtonClicked() }
        btnSetAutoStart.setOnClickListener { onSetAutoStartButtonClicked() }
        btnCancelAutoStart.setOnClickListener { onCancelAutoStartButtonClicked() }
        btnSetForceLockScreen.setOnClickListener { onSetForceLockScreenButtonClicked() }
    }

    override fun onStart() {
        super.onStart()
        if (pref().getBoolean(KEY_HOME_ENABLED, true)) {
            btnEnableHome.isEnabled = false
            btnDisenableHome.isEnabled = true
        } else {
            btnEnableHome.isEnabled = true
            btnDisenableHome.isEnabled = false
        }
        if (pref().getBoolean(KEY_RECENT_ENABLED, true)) {
            btnEnableRecent.isEnabled = false
            btnDisenableRecent.isEnabled = true
        } else {
            btnEnableRecent.isEnabled = true
            btnDisenableRecent.isEnabled = false
        }
        if (pref().getBoolean(STATUS_BAR_ENABLED, true)) {
            btnEnableStatusBar.isEnabled = false
            btnDisenableStatusBar.isEnabled = true
        } else {
            btnEnableStatusBar.isEnabled = true
            btnDisenableStatusBar.isEnabled = false
        }
    }


    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
        Toast.makeText(this, "Back Button Pressed", Toast.LENGTH_SHORT).show()
    }

    private fun onEnableHomeButtonClicked() {
        runCatching {
            DeviceManager().enableHomeKey(true)
        }.onSuccess {
            pref().edit { putBoolean(KEY_HOME_ENABLED, true) }
            btnEnableHome.isEnabled = false
            btnDisenableHome.isEnabled = true
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onDisenableHomeButtonClicked() {
        runCatching {
            DeviceManager().enableHomeKey(false)
        }.onSuccess {
            pref().edit { putBoolean(KEY_HOME_ENABLED, false) }
            btnEnableHome.isEnabled = true
            btnDisenableHome.isEnabled = false
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onEnableRecentButtonClicked() {
        runCatching {
            if (isOnScreenButtons()) {
                DeviceManager().rightKeyEnabled = true
            } else {
                DeviceManager().leftKeyEnabled = true
            }
        }.onSuccess {
            pref().edit { putBoolean(KEY_RECENT_ENABLED, true) }
            btnEnableRecent.isEnabled = false
            btnDisenableRecent.isEnabled = true
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onDisnableRecentButtonClicked() {
        runCatching {
            if (isOnScreenButtons()) {
                DeviceManager().rightKeyEnabled = false
            } else {
                DeviceManager().leftKeyEnabled = false
            }
        }.onSuccess {
            pref().edit { putBoolean(KEY_RECENT_ENABLED, false) }
            btnEnableRecent.isEnabled = true
            btnDisenableRecent.isEnabled = false
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onEnableStatusBarButtonClicked() {
        runCatching {
            DeviceManager().enableStatusBar(true)
        }.onSuccess {
            pref().edit { putBoolean(STATUS_BAR_ENABLED, true) }
            btnEnableStatusBar.isEnabled = false
            btnDisenableStatusBar.isEnabled = true
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onDisenableStatusBarButtonClicked() {
        runCatching {
            DeviceManager().enableStatusBar(false)
        }.onSuccess {
            pref().edit { putBoolean(STATUS_BAR_ENABLED, false) }
            btnEnableStatusBar.isEnabled = true
            btnDisenableStatusBar.isEnabled = false
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onSetKioskButtonClicked() {
        runCatching {
            DeviceManager().setLockTaskMode(packageName, true)
        }.onSuccess {
            btnSetKiosk.isEnabled = false
            btnCancelKiosk.isEnabled = true
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onCancelKioskButtonClicked() {
        runCatching {
            DeviceManager().setLockTaskMode(packageName, false)
        }.onSuccess {
            btnSetKiosk.isEnabled = true
            btnCancelKiosk.isEnabled = false
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun onSetKioskPwd123456ButtonClicked() {
        runCatching {
            DeviceManager().setLockTaskModePassword(KIOSK_PASSWORD);
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun onSetAutoStartButtonClicked() {
        runCatching {
            DeviceManager().setAutoRunningApp(ComponentName.unflattenFromString(PACKAGE_COMPONENT_MAIN), 1)
        }.onSuccess {
            Toast.makeText(this, "Set AutoStart successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onCancelAutoStartButtonClicked() {
        runCatching {
            DeviceManager().setAutoRunningApp(ComponentName.unflattenFromString(PACKAGE_COMPONENT_MAIN), 0)
        }.onSuccess {
            Toast.makeText(this, "Cancel AutoStart successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onSetForceLockScreenButtonClicked() {
        runCatching {
            DeviceManager().setForceLockScreen(true)
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

}

private fun isOnScreenButtons(): Boolean {
    val model = Build.MODEL.uppercase()
    val projectName = DeviceManager().getSettingProperty("pwv.project").uppercase()
    return (model.startsWith("I5300")
            || model.startsWith("I9200")
            || model.startsWith("I9600")
            || projectName.startsWith("SQ69")
            || projectName.startsWith("SQ65"))
}