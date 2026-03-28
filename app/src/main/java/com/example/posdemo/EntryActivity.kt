package com.example.posdemo

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

class EntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                DeviceManager().setSettingProperty("persist.sys.truncated.adb", "false")
                Toast.makeText(this, "ADB unlocked", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Please contact Urovo to use this...", Toast.LENGTH_SHORT).show()
            }
        }
    }
}