package com.example.posdemo

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.device.DeviceManager
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import com.example.posdemo.databinding.ActivityEntryBinding
import com.example.posdemo.printers.WebPrintActivity
import com.example.posdemo.receivers.MyDeviceAdminReceiver

const val START_PRINT_SERVICE = "startPrintService"
class EntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryBinding

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideNavigationBar()
        handleDeepLink(intent)

        binding.btnEnter.setOnClickListener { onEnterButtonClicked() }
        binding.etEnterCode.doOnTextChanged { text, _, _, _ ->
            if (text.toString() == "1119") {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
        }
        binding.etEnterCode.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onEnterButtonClicked()
                true
            }
            false
        }
    }

    override fun onStart() {
        super.onStart()
        dpm = getSystemService(DevicePolicyManager::class.java)
        admin = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (dpm.isDeviceOwnerApp(packageName)) {
            val group = getSharedPreferences("mdm", MODE_PRIVATE)
                .getString("group", "default")
            Toast.makeText(this, "This app is Device Owner ($group)\nTest: Enter 6759 to lock screen.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onEnterButtonClicked() {
        val intent = Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        when (binding.etEnterCode.text.toString()) {
            "1119" -> {
                intent.component = ComponentName(packageName, "$packageName.MainActivity")
                startActivity(intent)
            }
            "3333" -> {
                intent.component = ComponentName(packageName, "$packageName.others.LogActivity")
                startActivity(intent)
            }
            "7777" -> {
                intent.component = ComponentName(packageName, "$packageName.ApiTestActivity")
                startActivity(intent)
            }
            "36985" -> {
                DeviceManager().setSettingProperty("persist-persist.sys.truncated.adb", "false")
                Toast.makeText(this, "ADB unlocked", Toast.LENGTH_SHORT).show()
            }
            "3721" -> {
                intent.component = ComponentName(packageName, "$packageName.PrinterActivity")
                startActivity(intent)
            }
            "4631" -> {
                intent.component = ComponentName(packageName, "$packageName.PinpadActivity")
                startActivity(intent)
            }
            "6759" -> {
                try {
                    dpm.lockNow()
                } catch (_: Exception) {
                    Toast.makeText(this, "ERROR. NOT Device Owner.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(this, "Please contact Urovo to use this...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    // <-------------------Helper methods-------------------> //

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "wsprint" && data.host == "open_printer") {
            val intent = Intent(this, WebPrintActivity::class.java).apply {
                if (data.getQueryParameter("autostart") == "true") {
                    putExtra(START_PRINT_SERVICE, true)
                }
            }
            startActivity(intent)
        }
    }

    private fun hideNavigationBar() {
        window.insetsController?.let {
            it.hide(android.view.WindowInsets.Type.navigationBars())
            it.systemBarsBehavior =
                android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}