package com.example.posgeneralsdkdemo.others

import android.Manifest
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
import com.example.posgeneralsdkdemo.utils.PermissionUtil


private const val PREF_KIOSK = "pref_kiosk"
private const val KEY_HOME_ENABLED = "home_enabled"
private const val KEY_RECENT_ENABLED = "recent_enabled"
private const val STATUS_BAR_ENABLED = "status_bar_enabled"
private const val KIOSK_PASSWORD = "123456"
const val PACKAGE_COMPONENT_MAIN = "com.example.posgeneralsdkdemo/com.example.posgeneralsdkdemo.MainActivity"

class KioskRelatedActivity : AppCompatActivity() {

    private val btnEnableHome by lazy { findViewById<Button>(R.id.btnEnableHome) }
    private val btnDisableHome by lazy { findViewById<Button>(R.id.btnDisableHome) }
    private val btnEnableRecent by lazy { findViewById<Button>(R.id.btnEnableRecent) }
    private val btnDisableRecent by lazy { findViewById<Button>(R.id.btnDisableRecent) }
    private val btnEnableStatusBar by lazy { findViewById<Button>(R.id.btnEnableStatusBar) }
    private val btnDisableStatusBar by lazy { findViewById<Button>(R.id.btnDisableStatusBar) }
    private val btnSetKiosk by lazy { findViewById<Button>(R.id.btnSetKiosk) }
    private val btnCancelKiosk by lazy { findViewById<Button>(R.id.btnCancelKiosk) }
    private val btnSetKioskPwd123456 by lazy { findViewById<Button>(R.id.btnSetKioskPwd123456) }
    private val btnSetAutoStart by lazy { findViewById<Button>(R.id.btnSetAutoStart) }
    private val btnCancelAutoStart by lazy { findViewById<Button>(R.id.btnCancelAutoStart) }
    private val btnSetDefaultLauncher by lazy { findViewById<Button>(R.id.btnSetDefaultLauncher) }
    private val btnCancelDefaultLauncher by lazy { findViewById<Button>(R.id.btnCancelDefaultLauncher) }
    private val btnSetForceLockScreen by lazy { findViewById<Button>(R.id.btnSetForceLockScreen) }

    private fun pref() = getSharedPreferences(PREF_KIOSK, MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kiosk_related)

        btnEnableHome.setOnClickListener { onEnableHomeButtonClicked() }
        btnDisableHome.setOnClickListener { onDisableHomeButtonClicked() }
        btnEnableRecent.setOnClickListener { onEnableRecentButtonClicked() }
        btnDisableRecent.setOnClickListener { onDisableRecentButtonClicked() }
        btnEnableStatusBar.setOnClickListener { onEnableStatusBarButtonClicked() }
        btnDisableStatusBar.setOnClickListener { onDisableStatusBarButtonClicked() }
        btnSetKiosk.setOnClickListener { onSetKioskButtonClicked() }
        btnCancelKiosk.setOnClickListener { onCancelKioskButtonClicked() }
        btnSetKioskPwd123456.setOnClickListener { onSetKioskPwd123456ButtonClicked() }
        btnSetAutoStart.setOnClickListener { onSetAutoStartButtonClicked() }
        btnCancelAutoStart.setOnClickListener { onCancelAutoStartButtonClicked() }
        btnSetDefaultLauncher.setOnClickListener { onSetDefaultLauncherButtonClicked() }
        btnCancelDefaultLauncher.setOnClickListener { onCancelDefaultLauncherButtonClicked() }
        btnSetForceLockScreen.setOnClickListener { onSetForceLockScreenButtonClicked() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionUtil.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    override fun onStart() {
        super.onStart()
        if (pref().getBoolean(KEY_HOME_ENABLED, true)) {
            btnEnableHome.isEnabled = false
            btnDisableHome.isEnabled = true
        } else {
            btnEnableHome.isEnabled = true
            btnDisableHome.isEnabled = false
        }
        if (pref().getBoolean(KEY_RECENT_ENABLED, true)) {
            btnEnableRecent.isEnabled = false
            btnDisableRecent.isEnabled = true
        } else {
            btnEnableRecent.isEnabled = true
            btnDisableRecent.isEnabled = false
        }
        if (pref().getBoolean(STATUS_BAR_ENABLED, true)) {
            btnEnableStatusBar.isEnabled = false
            btnDisableStatusBar.isEnabled = true
        } else {
            btnEnableStatusBar.isEnabled = true
            btnDisableStatusBar.isEnabled = false
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
            btnDisableHome.isEnabled = true
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onDisableHomeButtonClicked() {
        runCatching {
            DeviceManager().enableHomeKey(false)
        }.onSuccess {
            pref().edit { putBoolean(KEY_HOME_ENABLED, false) }
            btnEnableHome.isEnabled = true
            btnDisableHome.isEnabled = false
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
            btnDisableRecent.isEnabled = true
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onDisableRecentButtonClicked() {
        runCatching {
            if (isOnScreenButtons()) {
                DeviceManager().rightKeyEnabled = false
            } else {
                DeviceManager().leftKeyEnabled = false
            }
        }.onSuccess {
            pref().edit { putBoolean(KEY_RECENT_ENABLED, false) }
            btnEnableRecent.isEnabled = true
            btnDisableRecent.isEnabled = false
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
            btnDisableStatusBar.isEnabled = true
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onDisableStatusBarButtonClicked() {
        runCatching {
            DeviceManager().enableStatusBar(false)
        }.onSuccess {
            pref().edit { putBoolean(STATUS_BAR_ENABLED, false) }
            btnEnableStatusBar.isEnabled = true
            btnDisableStatusBar.isEnabled = false
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

    private fun onSetDefaultLauncherButtonClicked() {
        runCatching {
            DeviceManager().setDefaultLauncher(ComponentName.unflattenFromString(PACKAGE_COMPONENT_MAIN))
        }.onSuccess {
            Toast.makeText(this, "Set Default Launcher successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onCancelDefaultLauncherButtonClicked() {
        runCatching {
            DeviceManager().removeDefaultLauncher(packageName)
        }.onSuccess {
            Toast.makeText(this, "Cancel Default Launcher successfully", Toast.LENGTH_SHORT).show()
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