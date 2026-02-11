package com.example.posdemo.others

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.device.DeviceManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posdemo.R
import androidx.core.content.edit
import com.example.posdemo.databinding.ActivityKioskRelatedBinding
import com.example.posdemo.utils.PermissionUtil
import com.example.posdemo.utils.PermissionUtil.ensureCanWriteSettings

private const val PREF_KIOSK = "pref_kiosk"
private const val KEY_HOME_ENABLED = "home_enabled"
private const val KEY_RECENT_ENABLED = "recent_enabled"
private const val STATUS_BAR_ENABLED = "status_bar_enabled"
private const val KIOSK_PASSWORD = "123456"
const val PACKAGE_COMPONENT_MAIN = "com.example.com.patrick.posdemo/com.example.com.patrick.posdemo.MainActivity"

class KioskRelatedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKioskRelatedBinding

    private val etPackageForWhitelist by lazy { findViewById<EditText>(R.id.etPackageForWhitelist) }



    private fun pref() = getSharedPreferences(PREF_KIOSK, MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKioskRelatedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEnableHome.setOnClickListener { onEnableHomeButtonClicked() }
        binding.btnDisableHome.setOnClickListener { onDisableHomeButtonClicked() }
        binding.btnEnableRecent.setOnClickListener { onEnableRecentButtonClicked() }
        binding.btnDisableRecent.setOnClickListener { onDisableRecentButtonClicked() }
        binding.btnEnableStatusBar.setOnClickListener { onEnableStatusBarButtonClicked() }
        binding.btnDisableStatusBar.setOnClickListener { onDisableStatusBarButtonClicked() }
        binding.btnSetKiosk.setOnClickListener { onSetKioskButtonClicked() }
        binding.btnCancelKiosk.setOnClickListener { onCancelKioskButtonClicked() }
        binding.btnSetKioskPwd123456.setOnClickListener { onSetKioskPwd123456ButtonClicked() }
        binding.btnSetAutoStart.setOnClickListener { onSetAutoStartButtonClicked() }
        binding.btnCancelAutoStart.setOnClickListener { onCancelAutoStartButtonClicked() }
        binding.btnSetDefaultLauncher.setOnClickListener { onSetDefaultLauncherButtonClicked() }
        binding.btnCancelDefaultLauncher.setOnClickListener { onCancelDefaultLauncherButtonClicked() }
        binding.btnSetForceLockScreen.setOnClickListener { onSetForceLockScreenButtonClicked() }
        binding.btnAddAppToWhitelist.setOnClickListener { onAddAppToWhitelistButtonClicked() }
        binding.btnGetAppWhitelist.setOnClickListener { onGetAppWhitelistButtonClicked() }
        binding.btnRemoveAppFromWhitelist.setOnClickListener { onRemoveAppFromWhitelistButtonClicked() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionUtil.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    private fun onAddAppToWhitelistButtonClicked() {
        if (!ensureCanWriteSettings(this)) {
            Toast.makeText(this, "Please grant permission first", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            DeviceManager().setAllowInstallApps(etPackageForWhitelist.text.toString(), 0, 1)
        }.onSuccess {
            Toast.makeText(this, "Add App to Whitelist successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Add App to Whitelist failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onGetAppWhitelistButtonClicked() {
        runCatching {
            Toast.makeText(this, DeviceManager().getAllowInstallApps(0).toString(), Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Get App Whitelist failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onRemoveAppFromWhitelistButtonClicked() {
        if (!ensureCanWriteSettings(this)) {
            Toast.makeText(this, "Please grant permission first", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            DeviceManager().setAllowInstallApps(etPackageForWhitelist.text.toString(), 0, 2)
        }.onSuccess {
            Toast.makeText(this, "Remove App from Whitelist successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Remove App from Whitelist failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    override fun onStart() {
        super.onStart()
        if (pref().getBoolean(KEY_HOME_ENABLED, true)) {
            binding.btnEnableHome.isEnabled = false
            binding.btnDisableHome.isEnabled = true
        } else {
            binding.btnEnableHome.isEnabled = true
            binding.btnDisableHome.isEnabled = false
        }
        if (pref().getBoolean(KEY_RECENT_ENABLED, true)) {
            binding.btnEnableRecent.isEnabled = false
            binding.btnDisableRecent.isEnabled = true
        } else {
            binding.btnEnableRecent.isEnabled = true
            binding.btnDisableRecent.isEnabled = false
        }
        if (pref().getBoolean(STATUS_BAR_ENABLED, true)) {
            binding.btnEnableStatusBar.isEnabled = false
            binding.btnDisableStatusBar.isEnabled = true
        } else {
            binding.btnEnableStatusBar.isEnabled = true
            binding.btnDisableStatusBar.isEnabled = false
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
            binding.btnEnableHome.isEnabled = false
            binding.btnDisableHome.isEnabled = true
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onDisableHomeButtonClicked() {
        runCatching {
            DeviceManager().enableHomeKey(false)
        }.onSuccess {
            pref().edit { putBoolean(KEY_HOME_ENABLED, false) }
            binding.btnEnableHome.isEnabled = true
            binding.btnDisableHome.isEnabled = false
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
            binding.btnEnableRecent.isEnabled = false
            binding.btnDisableRecent.isEnabled = true
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
            binding.btnEnableRecent.isEnabled = true
            binding.btnDisableRecent.isEnabled = false
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onEnableStatusBarButtonClicked() {
        runCatching {
            DeviceManager().enableStatusBar(true)
        }.onSuccess {
            pref().edit { putBoolean(STATUS_BAR_ENABLED, true) }
            binding.btnEnableStatusBar.isEnabled = false
            binding.btnDisableStatusBar.isEnabled = true
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onDisableStatusBarButtonClicked() {
        runCatching {
            DeviceManager().enableStatusBar(false)
        }.onSuccess {
            pref().edit { putBoolean(STATUS_BAR_ENABLED, false) }
            binding.btnEnableStatusBar.isEnabled = true
            binding.btnDisableStatusBar.isEnabled = false
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onSetKioskButtonClicked() {
        runCatching {
            DeviceManager().setLockTaskMode(packageName, true)
        }.onSuccess {
            binding.btnSetKiosk.isEnabled = false
            binding.btnCancelKiosk.isEnabled = true
        }.onFailure {
            it.printStackTrace()
        }
    }


    private fun onCancelKioskButtonClicked() {
        runCatching {
            DeviceManager().setLockTaskMode(packageName, false)
        }.onSuccess {
            binding.btnSetKiosk.isEnabled = true
            binding.btnCancelKiosk.isEnabled = false
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

fun isOnScreenButtons(): Boolean {
    val model = Build.MODEL.uppercase()
    val projectName = DeviceManager().getSettingProperty("pwv.project").uppercase()
    return (model.startsWith("I5300")
            || model.startsWith("I9200")
            || model.startsWith("I9600")
            || projectName.startsWith("SQ69")
            || projectName.startsWith("SQ65"))
}