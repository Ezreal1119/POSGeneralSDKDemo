package com.example.posgeneralsdkdemo

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.databinding.ActivityLedBinding
import com.urovo.sdk.led.LEDDriverImpl

class LedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLedBinding
    private val mLedManager = LEDDriverImpl.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTurnOnLed.setOnClickListener { onTurnOnLedButtonClicked() }
        binding.btnTurnOffLed.setOnClickListener { onTurnOffLedButtonClicked() }
        binding.btnTurnOnAllLed.setOnClickListener { onTurnOnAllLedButtonClicked() }
        binding.btnTurnOffAllLed.setOnClickListener { onTurnOffAllLedButtonClicked() }

        binding.spLedColor.adapter = ArrayAdapter(this, simple_spinner_item, LedColor.entries).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
    }

    private fun onTurnOnLedButtonClicked() {
        runCatching {
            val color = binding.spLedColor.selectedItem as LedColor
            mLedManager.turnOn(color.id)
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOffLedButtonClicked() {
        runCatching {
            val color = binding.spLedColor.selectedItem as LedColor
            mLedManager.turnOff(color.id)
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOnAllLedButtonClicked() {
        runCatching {
            for (i in 1..4) {
                mLedManager.turnOn(i)
            }
        }.onSuccess {
            Toast.makeText(this, "Turned on all LEDs successfully", Toast.LENGTH_SHORT).show()
        }.onFailure{
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOffAllLedButtonClicked() {
        runCatching {
            for (i in 1..4) {
                mLedManager.turnOff(i)
            }
        }.onSuccess {
            Toast.makeText(this, "Turned off all LEDs successfully", Toast.LENGTH_SHORT).show()
        }.onFailure{
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

}

enum class LedColor(val id: Int, val color: String) {
    BLUE(1, "Blue"),
    YELLOW(2, "Yellow"),
    GREEN(3, "Green"),
    RED(4, "Red");

    override fun toString(): String {
        return color
    }
}