package com.example.posgeneralsdkdemo.others

import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.posgeneralsdkdemo.R
import com.example.posgeneralsdkdemo.utils.PermissionUtil
import com.google.android.material.slider.Slider

private const val SCREEN_BRIGHTNESS =  "screen_brightness"
class SettingsActivity : AppCompatActivity() {

    private val sliderBrightness by lazy { findViewById<Slider>(R.id.sliderBrightness) }
    private val btnGetBrightness by lazy { findViewById<Button>(R.id.btnGetBrightness) }
    private val btnSetBrightness by lazy { findViewById<Button>(R.id.btnSetBrightness) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btnGetBrightness.setOnClickListener { onGetBrightnessButtonClicked() }
        btnSetBrightness.setOnClickListener { onSetBrightnessButtonClicked() }
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
            Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS, sliderBrightness.value.toInt())
        }
    }


}