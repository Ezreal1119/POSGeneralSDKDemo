package com.example.posdemo.others

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.app.AlertDialog
import android.content.Intent
import android.device.DeviceManager
import android.device.PiccManager
import android.device.UFSManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posdemo.R
import com.example.posdemo.databinding.ActivitySettingsBinding
import com.example.posdemo.services.ConfigWatcherService
import com.example.posdemo.tools.ConfigFileWatcher
import com.example.posdemo.utils.ImageUtil
import com.example.posdemo.utils.PermissionUtil
import com.example.posdemo.webview.WebViewActivity
import com.urovo.sdk.utils.SystemProperties.getSystemProperty
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    companion object {
        private val ARRAY_OF_PROPERTIES = arrayOf(
            "Complete Settings Password", // persist.sys.urv.all.settings.password ? "$password" ? ""
            "Icon Settings Password", // persist.sys.urv.set.settings.password ? "$password" ? ""
            "StatusBar Settings Password", // persist.sys.urv.enable.record.access.status ? "true" ? "*"
            "ADB truncation status", // persist.sys.truncated.adb ? "*" ? "false"
            "Double Tap 2 wake", // persist.sys.urv.tp.wakeup.gesture ? "doubleclick" : ""
        )
    }

    private lateinit var binding: ActivitySettingsBinding
    val piccManager = PiccManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGetSystemProperty.setOnClickListener { onGetSystemPropertyButtonClicked() }
        binding.btnSetSystemProperty.setOnClickListener { onSetSystemPropertyButtonClicked() }
        binding.btnGetBrightness.setOnClickListener { onGetBrightnessButtonClicked() }
        binding.btnSetBrightness.setOnClickListener { onSetBrightnessButtonClicked() }
        binding.btnSetFontSize.setOnClickListener { onSetFontSizeButtonClicked() }
        binding.btnResetFontSize.setOnClickListener { onResetFontSizeButtonClicked() }
        binding.btnSetDisplaySize.setOnClickListener { onSetDisplaySizeButtonClicked() }
        binding.btnResetDisplaySize.setOnClickListener { onResetDisplaySizeButtonClicked() }
        binding.btnQueryApnByName.setOnClickListener { onQueryApnByNameButtonClicked() }
        binding.btnAddApn.setOnClickListener { onAddApnButtonClicked() }
        binding.btnDeleteApnByName.setOnClickListener { onDeleteApnByNameButtonClicked() }
        binding.btnSetLockPassword.setOnClickListener { onSetLockPasswordButtonClicked() }
        binding.btnClearLockPassword.setOnClickListener { onClearLockPasswordButtonClicked() }
        binding.btnSetSettingsPassword.setOnClickListener { onSetSettingsPasswordButtonClicked() }
        binding.btnClearSettingsPassword.setOnClickListener { onClearSettingsPasswordButtonClicked() }
        binding.btnTurnOnHce.setOnClickListener { onTurnOnHceButtonClicked() }
        binding.btnTurnOffHce.setOnClickListener { onTurnOffHceButtonClicked() }
        binding.btnTurnOnHostMode.setOnClickListener { onTurnOnHostModeButtonClicked() }
        binding.btnTurnOffHostMode.setOnClickListener { onTurnOffHostModeButtonClicked() }
        binding.btnGetTimeSettings.setOnClickListener { onGetTimeSettingsButtonClicked() }
        binding.btnSetTimeSettings.setOnClickListener { onSetTimeSettingsButtonClicked() }
        binding.btnSetCustomWallpaper.setOnClickListener { onSetCustomWallpaperButtonClicked() }
        binding.btnSetDefaultWallpaper.setOnClickListener { onSetDefaultWallpaperButtonClicked() }
        binding.btnTtsTest.setOnClickListener { onTtsTestButtonClicked() }
        binding.btnStartConfigWatcher.setOnClickListener { onStartConfigWatcherButtonClicked() }
        binding.btnWebViewTestUrovo.setOnClickListener { onWebViewTestUrovoButtonClicked() }
        binding.btnWebViewTestLocal.setOnClickListener { onWebViewTestLocalButtonClicked() }

        binding.etNumberInput.showSoftInputOnFocus = false
        binding.etNumberInput.setOnFocusChangeListener{ v, hasFocus ->
            if (hasFocus) {
                DeviceManager().setSettingProperty("Global-ufans.keyboard.state", "0")
                sendBroadcast(Intent("android.intent.action.ACTION_SWITCH_KEY_STATE"))
            }
        }

        binding.etLetterInput.showSoftInputOnFocus = false
        binding.etLetterInput.setOnFocusChangeListener{ v, hasFocus ->
            if (hasFocus) {
                DeviceManager().setSettingProperty("Global-ufans.keyboard.state", "1")
                sendBroadcast(Intent("android.intent.action.ACTION_SWITCH_KEY_STATE"))
            }
        }

        binding.spSystemProperty.adapter = ArrayAdapter(this, simple_spinner_item, ARRAY_OF_PROPERTIES).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        binding.spSystemProperty.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                var propName= ""
                var valueHint = ""
                when (binding.spSystemProperty.selectedItem as String) {
                    ARRAY_OF_PROPERTIES[0] -> {
                        propName = "persist.sys.urv.all.settings.password"
                        valueHint = "\"\$password\" | \"\""
                    }
                    ARRAY_OF_PROPERTIES[1] -> {
                        propName = "persist.sys.urv.set.settings.password"
                        valueHint = "\"\$password\" | \"\""
                    }
                    ARRAY_OF_PROPERTIES[2] -> {
                        propName = "persist.sys.urv.enable.record.access.status"
                        valueHint = "\"true\" | \"*\""
                    }
                    ARRAY_OF_PROPERTIES[3] -> {
                        propName = "persist.sys.truncated.adb"
                        valueHint = "\"*\" | \"false\""
                    }
                    ARRAY_OF_PROPERTIES[4] -> {
                        propName = "persist.sys.truncated.adb"
                        valueHint = "\"*\" | \"false\""
                    }
                }
                binding.etSystemPropertyKey.setText(propName)
                binding.etSystemPropertyValue.hint = valueHint
                binding.etSystemPropertyValue.setText("")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

    }

    private fun onStartConfigWatcherButtonClicked() {
        if (!PermissionUtil.ensureAllFilesAccess(this)) {
            return
        }
        Toast.makeText(this, "ConfigWatcherService started", Toast.LENGTH_SHORT).show()
        val configWatcherIntent = Intent(this, ConfigWatcherService::class.java)
        startService(configWatcherIntent)
    }

    private fun onTurnOnHceButtonClicked() {

        val addr = 1
        val writeData = "Patrick".toByteArray()
        val readBuffer = ByteArray(128)

        piccManager.picc_TAG_SetMode(1)
        piccManager.picc_TAG_Write(addr, writeData, writeData.size)
        piccManager.picc_TAG_Read(addr, readBuffer.size, readBuffer)

        val result = String(readBuffer).trim('\u0000', ' ')
        Toast.makeText(this, "HCE on: $result", Toast.LENGTH_SHORT).show()
    }

    private fun onTurnOffHceButtonClicked() {
        piccManager.picc_TAG_SetMode(0)
        Toast.makeText(this, "HCE off", Toast.LENGTH_SHORT).show()
    }

    private fun onTurnOnHostModeButtonClicked() {
        runCatching {
            DeviceManager().setSettingProperty("System-sys.hostkey.switch","1")
        }.onSuccess {
            Toast.makeText(this, "Turned on Host Mode", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onTurnOffHostModeButtonClicked() {
        runCatching {
            DeviceManager().setSettingProperty("System-sys.hostkey.switch","0")
        }.onSuccess {
            Toast.makeText(this, "Turned on Host Mode", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onGetSystemPropertyButtonClicked() {
        runCatching {
            return@runCatching DeviceManager().getSettingProperty(binding.etSystemPropertyKey.text.toString().trim())
        }.onSuccess { ret ->
            if (ret.isBlank()) {
                Toast.makeText(this, "No Value for this Property", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Value: $ret", Toast.LENGTH_SHORT).show()
            }
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onSetSystemPropertyButtonClicked() {
        runCatching {
            DeviceManager().setSettingProperty("persist-${binding.etSystemPropertyKey.text.toString().trim()}", binding.etSystemPropertyValue.text.toString().trim())
        }.onSuccess {
            if (binding.etSystemPropertyKey.text.toString().isBlank()) {
                Toast.makeText(this, "Please enter a Property first", Toast.LENGTH_SHORT).show()
                return
            }
            if (binding.etSystemPropertyValue.text.toString().isBlank()) {
                Toast.makeText(this, "Set Empty String successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Set value ${binding.etSystemPropertyValue.text} successfully", Toast.LENGTH_SHORT).show()
            }
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onSetFontSizeButtonClicked() {
        runCatching {
            DeviceManager().setSettingProperty("System-font_scale", binding.sliderFontSize.value.toString());
        }.onSuccess {
            Toast.makeText(this, "Set Font Size to ${binding.sliderFontSize.value} successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Set Font Size failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onResetFontSizeButtonClicked() {
        runCatching {
            DeviceManager().setSettingProperty("System-font_scale", "1")
        }.onSuccess {
            binding.sliderFontSize.value = 1F
            Toast.makeText(this, "Reset Font Size successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Reset Font Size failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onSetDisplaySizeButtonClicked() {
        runCatching {
            DeviceManager().setSettingProperty("Secure-display_density_forced", binding.sliderDisplaySize.value.toString());
        }.onSuccess {
            Toast.makeText(this, "Set Display Size to ${binding.sliderDisplaySize.value} successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Set Display Size failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onResetDisplaySizeButtonClicked() {
        runCatching {
            DeviceManager().setSettingProperty("Secure-display_density_forced", "1")
        }.onSuccess {
            binding.sliderDisplaySize.value = 1F
            Toast.makeText(this, "Reset Display Size successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Reset Display Size failed", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onGetBrightnessButtonClicked() {
        runCatching {
            Toast.makeText(this, "Brightness: ${Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)}", Toast.LENGTH_SHORT).show()  // 0 - 255
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onSetBrightnessButtonClicked() {
        runCatching {
            if (!PermissionUtil.ensureCanWriteSettings(this)) {
                return
            }
             Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, binding.sliderBrightness.value.toInt())
//            DeviceManager().setSettingProperty("System-screen_brightness", binding.sliderBrightness.value.toString())
        }.onSuccess {
            Toast.makeText(this, "Set Brightness to ${binding.sliderBrightness.value} successfully\n Use setSettings to bypass permission check", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onQueryApnByNameButtonClicked() {
        runCatching {
            return@runCatching DeviceManager().queryAPN("apn=?", arrayOf(binding.etApnName.text.toString())) ?: throw Exception("Query APN failed.")
        }.onSuccess { apn ->
            Toast.makeText(this, apn, Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "${it.message} Only APN of current SIM will be displayed.", Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onAddApnButtonClicked() {
        runCatching {
            val ret = DeviceManager().setAPN("Patrick", binding.etApnName.text.toString(), "", 0, "", "", "", "", binding.etMobileCountryCode.text.toString(), binding.etMobileNetworkCode.text.toString(), "", 0, 0, "", "", 0, "", true)
            if (!ret) throw Exception("Add APN failed")
        }.onSuccess {
            Toast.makeText(this, "Add APN successfully. Only MCC/MNC matched APNs will be displayed", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onDeleteApnByNameButtonClicked() {
        runCatching {
            return@runCatching DeviceManager().deleteAPN("apn=?", arrayOf(binding.etApnName.text.toString()))
        }.onSuccess { count ->
            Toast.makeText(this, "$count APN(s) deleted successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onSetLockPasswordButtonClicked() {
        runCatching {
            DeviceManager().saveLockPassword(binding.etPassword.text.toString(), 1)
        }.onSuccess {
            Toast.makeText(this, "Set Lock Screen Password successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onClearLockPasswordButtonClicked() {
        runCatching {
            DeviceManager().clearLock()
        }.onSuccess {
            Toast.makeText(this, "Clear Lock Screen Password successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onSetSettingsPasswordButtonClicked() {
        runCatching {
            if (getDevType() == "SQ29M") {
                DeviceManager().setSettingProperty("persist-persist.sys.urv.set.settings.password", binding.etPassword.text.toString())
            } else if (getDevType() == "SQ68"){
                DeviceManager().setSettingProperty("persist-persist.sys.urv.settings.password", binding.etPassword.text.toString())
            } else {
                Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show()
            }
        }.onSuccess {
            Toast.makeText(this, "Set Password ${binding.etPassword.text} successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onClearSettingsPasswordButtonClicked() {
        runCatching {
            DeviceManager().setSettingProperty("persist-persist.sys.urv.all.settings.password", "")
        }.onSuccess {
            Toast.makeText(this, "Set Password ${binding.etPassword.text} successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onGetTimeSettingsButtonClicked() {
        runCatching {
            val ntpServer = DeviceManager().getSettingProperty("Global-ntp_server")
            val timeZone = DeviceManager().getSettingProperty("persist-persist.sys.timezone")
            return@runCatching Pair(ntpServer, timeZone)
        }.onSuccess { (ntpServer, timeZone) ->
            Toast.makeText(this, "ntpServer: $ntpServer\nTimeZone: $timeZone", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onSetTimeSettingsButtonClicked() {
        runCatching {
            AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("The device will reboot to change TimeZone to 'America/Los_Angeles', are you sure?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm") { _, _ ->
                    DeviceManager().setSettingProperty("persist-persist.sys.timezone", "America/Los_Angeles")
//                    DeviceManager().setSettingProperty("persist-persist.sys.timezone", "Asia/Shanghai")
                    DeviceManager().shutdown(true)
                }
                .show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onSetCustomWallpaperButtonClicked() {
        runCatching {
            val clazz = Class.forName("android.device.UFSManager")
            val method = clazz.getMethod("setWallpaper", Bitmap::class.java, Int::class.java)
            method.invoke(UFSManager(), ImageUtil.pngToBitmap(resources, R.drawable.wallpaper), 1)
        }.onSuccess {
            Toast.makeText(this, "Set Custom Wallpaper successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onSetDefaultWallpaperButtonClicked() {
        runCatching {
            val clazz = Class.forName("android.device.UFSManager")
            val method = clazz.getMethod("setWallpaper", Bitmap::class.java, Int::class.java)
            method.invoke(UFSManager(), null, 1)
        }.onSuccess {
            Toast.makeText(this, "Set Default Wallpaper successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTtsTestButtonClicked() {
        lateinit var tts: TextToSpeech
        runCatching {
            tts = TextToSpeech(this) { status ->
                if (status != TextToSpeech.SUCCESS) return@TextToSpeech
                tts.language = Locale.JAPANESE
                tts.setSpeechRate(0.95f)
                tts.setPitch(1.05f)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onDone(utteranceId: String?) {
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "Finished TTS!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onError(utteranceId: String?) {
                    }

                    override fun onStart(utteranceId: String?) {
                    }
                })

                tts.speak(
                    "真実はいつもひとつ！",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "conan_truth"
                )
            }
        }
    }

    private fun onWebViewTestUrovoButtonClicked() {
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra("url", "https://en.urovo.com/")
        }
        startActivity(intent)
    }

    private fun onWebViewTestLocalButtonClicked() {
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra("url", "file:///android_asset/web_socket_demo.html")
        }
        startActivity(intent)
    }

    private fun getDevType(): String {
        return getSystemProperty("pwv.project", "no result found!")
    }
}