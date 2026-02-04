package com.example.posgeneralsdkdemo.others

import android.app.admin.DevicePolicyManager
import android.device.DeviceManager
import android.os.Bundle
import android.provider.Settings
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

}