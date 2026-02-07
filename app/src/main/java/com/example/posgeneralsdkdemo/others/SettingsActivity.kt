package com.example.posgeneralsdkdemo.others

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.device.DeviceManager
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.posgeneralsdkdemo.R
import com.example.posgeneralsdkdemo.databinding.ActivitySettingsBinding
import com.example.posgeneralsdkdemo.utils.PermissionUtil
import com.google.android.material.slider.Slider
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGetBrightness.setOnClickListener { onGetBrightnessButtonClicked() }
        binding.btnSetBrightness.setOnClickListener { onSetBrightnessButtonClicked() }
        binding.btnQueryApnByName.setOnClickListener { onQueryApnByNameButtonClicked() }
        binding.btnAddApn.setOnClickListener { onAddApnButtonClicked() }
        binding.btnDeleteApnByName.setOnClickListener { onDeleteApnByNameButtonClicked() }
        binding.btnSetLockPassword.setOnClickListener { onSetLockPasswordButtonClicked() }
        binding.btnClearLockPassword.setOnClickListener { onClearLockPasswordButtonClicked() }
        binding.btnTtsTest.setOnClickListener { onTtsTestButtonClicked() }
        binding.btnGetTimeSettings.setOnClickListener { onGetTimeSettingsButtonClicked() }
        binding.btnSetTimeSettings.setOnClickListener { onSetTimeSettingsButtonClicked() }
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
                    DeviceManager().shutdown(true)
                }
                .show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
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
            DeviceManager().saveLockPassword(binding.etLockPassword.text.toString(), 1)
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


}